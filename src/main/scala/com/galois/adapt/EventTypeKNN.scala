package com.galois.adapt

import akka.actor.{Actor, ActorLogging}
import com.galois.adapt.adm.{AdmPathNode, AdmSubject}
import com.galois.adapt.cdm18.EventType
import smile.classification._
import smile.math.distance.Distance
import smile.validation.CrossValidation
import java.io.{FileInputStream, ObjectOutputStream, PrintWriter}

import com.thoughtworks.xstream.XStream

import scala.collection.immutable
import scala.io.Source

object EventTypeKNN {
  type ProcessName = Set[String]
  type EventCounts = Map[EventType,Int]
  type EventVec = Array[Int]
  type ConfusionTuple = (Int,Int,Int,Int) //tp,fp,fn,tn

  class JaccardDistance extends Distance[EventVec] {
    override def d(x: EventVec, y: EventVec): Double = {
      val denominator = x.zip(y).count(z=>z._1+z._2>0)
      val numerator = x.zip(y).count(z=>z._1*z._2>0)
      1-numerator.toDouble/denominator.toDouble
    }
  }

  class GeneralizedJaccardDistance extends Distance[EventVec] {
    override def d(x: EventVec, y: EventVec): Double = {
      val denominator = x.zip(y).map(z=>math.max(z._1,z._2)).sum
      val numerator = x.zip(y).map(z=>math.min(z._1,z._2)).sum
      1-numerator.toDouble/denominator.toDouble
    }
  }

  class CosineDistance extends Distance[EventVec] {
    override def d(x: EventVec, y: EventVec): Double = {
      val magnitude_x = math.sqrt(x.map(z=>z*z).sum.toDouble)
      val magnitude_y = math.sqrt(y.map(z=>z*z).sum.toDouble)
      val numerator = x.zip(y).map(z=>z._1*z._2).sum.toDouble
      1-numerator.toDouble/(magnitude_x*magnitude_y)
    }
  }

  class EventTypeKNNTrain {
    var dataMap: Map[ProcessName,Array[EventCounts]] = Map.empty

    def collect(s: AdmSubject, subPaths: Set[String], eventMap: EventCounts): Unit = {
      val newArray = dataMap.getOrElse(subPaths,Array(Map.empty[EventType,Int])) :+ eventMap
      dataMap += (subPaths -> newArray)
    }

    def transformData(data: Map[ProcessName,Array[EventCounts]]): (Array[EventVec],Array[ProcessName]) = {
    val dataPairs = data.toList.filter(x => x._2.length>=10)
      .flatMap( x => x._2
      .map(emap => (x._1,EventType.values
        .map(e => emap
          .getOrElse(e,0))
          .toArray))).toArray
    (dataPairs.map(_._2),dataPairs.map(_._1))

    }

    def stratifiedSplit(X: Array[EventVec],y: Array[Int],trainPercent: Int = 50): (Array[EventVec], Array[Int],Array[EventVec], Array[Int]) ={
      val pairs: Array[(EventVec, Int)] = X.zip(y)
      val trueLabel = pairs.filter(x => x._2 == 1)
      val falseLabel = pairs.filter(x => x._2 == 0)
      val tidx = math.round(trueLabel.length*trainPercent/100.0).toInt
      val fidx = math.round(falseLabel.length*trainPercent/100.0).toInt
      val data_true: (Array[(EventVec, Int)], Array[(EventVec, Int)]) = trueLabel.splitAt(tidx)
      val data_false: (Array[(EventVec, Int)], Array[(EventVec, Int)]) = falseLabel.splitAt(fidx)
      val X_true = data_true._1.unzip._1
      val y_true = data_true._1.unzip._2
      val X_false = data_false._1.unzip._1
      val y_false = data_false._1.unzip._2
      val Xt_true = data_true._2.unzip._1
      val yt_true = data_true._2.unzip._2
      val Xt_false = data_false._2.unzip._1
      val yt_false = data_false._2.unzip._2

      (X_true++X_false,y_true++y_false,Xt_true++Xt_false,yt_true++yt_false)


    }

    def transformLabels(processNameArray: Array[ProcessName],processName: ProcessName): Array[Int] = {
      processNameArray.map(x => if (x==processName) 1 else 0)
    }

    def confusionArray(y_actual: Array[Int], y_predict: Array[Int]): ConfusionTuple = { // tn, fp, fn, tp
      val predictions = y_predict.zipWithIndex
      predictions.foldLeft((0,0,0,0))((acc,x) =>
        x._1 match {
          case 1 if y_actual(x._2)==1 => (acc._1,acc._2,acc._3,acc._4 +1)
          case 1 => (acc._1,acc._2+1,acc._3,acc._4)
          case 0 if y_actual(x._2)==1 => (acc._1,acc._2,acc._3+1,acc._4 )
          case 0 => (acc._1+1,acc._2,acc._3,acc._4 +1)
        })
    }

    def getKNNValidationStats(X: Array[EventVec], y: Array[Int], distance: Distance[EventVec], k: Int): (Distance[EventVec],Int,ConfusionTuple) = { //tp,fp,fn,tn returned

      val pairs: Array[(EventVec, Int)] = X.zip(y)
      val trueLabel = pairs.filter(x => x._2 == 1)
      val falseLabel = pairs.filter(x => x._2 == 0)

      // The following function was created to help with stratifying the cross validation records
      def getCrossValRecords(labeledPairs: Array[(EventVec,Int)],folds: Int = 3): Array[(Array[EventVec], Array[Int], Array[EventVec], Array[Int])] = {
        val unzipped = labeledPairs.unzip

        val cv = new CrossValidation(unzipped._2.length, folds)

        val testDataWithIndices = (unzipped._1.zipWithIndex, unzipped._2.zipWithIndex)

        val trainingDPSets: Array[Array[EventVec]] = cv.train
          .map(indexList => indexList
            .map(index => testDataWithIndices
              ._1.collectFirst { case (dp, `index`) => dp }.get))

        val trainingClassifierSets = cv.train
          .map(indexList => indexList
            .map(index => testDataWithIndices
              ._2.collectFirst { case (dp, `index`) => dp }.get))

        val testingDPSets = cv.test
          .map(indexList => indexList
            .map(index => testDataWithIndices
              ._1.collectFirst { case (dp, `index`) => dp }.get))

        val testingClassifierSets = cv.test
          .map(indexList => indexList
            .map(index => testDataWithIndices
              ._2.collectFirst { case (dp, `index`) => dp }.get))

        trainingDPSets
          .zipWithIndex.map(x => (x._1,
          trainingClassifierSets(x._2),
          testingDPSets(x._2),
          testingClassifierSets(x._2)
        )
        )
      }

      val trueValidationRoundRecords = getCrossValRecords(trueLabel,3).zipWithIndex
      val falseValidationRoundRecords = getCrossValRecords(falseLabel,3)

      val validationRoundRecords = trueValidationRoundRecords.map(x =>
        (x._1._1 ++ falseValidationRoundRecords(x._2)._1,
          x._1._2 ++ falseValidationRoundRecords(x._2)._2,
          x._1._3 ++ falseValidationRoundRecords(x._2)._3,
          x._1._4 ++ falseValidationRoundRecords(x._2)._4)
      )
      val confusionTuple = validationRoundRecords
        .foldLeft((0,0,0,0)) { //tn,fp,fn,tp
          (acc:ConfusionTuple, record: (Array[EventVec],Array[Int],Array[EventVec],Array[Int])) =>

          val model = knn[EventVec](record._1, record._2, distance, k)

          //And for each test data point make a prediction with the model
          val predictions = record
            ._3
            .map(x => model.predict(x))

           val confusionTuple = confusionArray(record._4,predictions)

            (acc._1+confusionTuple._1,acc._2+confusionTuple._2,acc._3+confusionTuple._3,acc._4+confusionTuple._4)
        }
      (distance,k,confusionTuple)
    }

    def selectBestModel(modelStats: List[(Distance[EventVec],Int,ConfusionTuple)]): (Distance[EventVec],Int,ConfusionTuple) = {
      modelStats.sortBy(x => x._3._3)
        .take(3) //Take the three model parameters that produce the fewest false alarms.
        .maxBy(x => (x._3._1 + x._3._4).toDouble / (x._3._1 + x._3._2 + x._3._3 + x._3._4).toDouble)  //Of those, take the one with the highest accuracy.
    }

    def bestModelIfExists(model: KNN[EventVec], X: Array[EventVec],y: Array[Int], distance: Distance[EventVec],k: Int,falseAlarmThreshold: Int):
    (Option[KNN[EventVec]],Distance[EventVec],Int,ConfusionTuple) = {
      val predictions = X.map(x => model.predict(x))
      val confusionTuple = confusionArray(y,predictions)
      if (confusionTuple._3 <= falseAlarmThreshold) (Some(model),distance,k,confusionTuple)
      else (None,distance,k,confusionTuple)
    }

    def testSelectWriteModels(falseAlarmThreshold: Int): Unit = {
      dataMap.foreach(x => println(x._1+" "+x._2.length.toString))
      //val data = transformData(dataMap)
      val generalData = transformData(dataMap) //(X,y)
      val processNames = generalData._2.toSet
      processNames.foreach(p => println(generalData._1.length.toString+" "+generalData._2.count(ps=>ps==p)))
      val processModels: Map[ProcessName,(Option[KNN[EventVec]],Distance[EventVec],Int,ConfusionTuple)] = processNames.map { processName =>
        println("Processing "+processName)
        val data = stratifiedSplit(generalData._1,transformLabels(generalData._2,processName)) //X_train,y_train,X_test,y_test
        println("Train Trues "+data._2.count(_==1).toString)
        println("Test Trues "+data._4.count(_==1).toString)
        val distances = List(new JaccardDistance,new GeneralizedJaccardDistance, new CosineDistance)
        val numNeighbors = List(1, 3, 5)
        val modelChoices = for (d <- distances; k <- numNeighbors) yield getKNNValidationStats(data._1,data._2, d, k)
        val bestParams = selectBestModel(modelChoices)
        val bestModel = knn[EventVec](data._1, data._2, bestParams._1, bestParams._2)
        processName ->
          bestModelIfExists(bestModel, data._3, data._4, bestParams._1, bestParams._2, falseAlarmThreshold)
      }.toMap

      processModels.foreach(x => println(x._1.mkString(","),x._2._2,x._2._3,x._2._4.toString(),
        (x._2._4._1+x._2._4._4).toDouble/(x._2._4._1+x._2._4._4+x._2._4._2+x._2._4._3).toDouble))

      val toStore: Map[ProcessName,KNN[EventVec]] = processModels
        .filter(x => x._2._1.isDefined)
        .map(x => x._1 -> x._2._1.get)
      val storeMe = toStore.get(Set("sh","dd"))
      smile.write.xstream(storeMe,"knnmodel.ser")

      val xstream = new XStream
      val xml = xstream.toXML(toStore)
      new PrintWriter("knnmodels.xml") {
        write(xml)
        close
      }
    }
  }

  class EventTypeKNNEvaluate(knnModelFile: String) {
    def readModel(file: String): AnyRef = {
      val xml = Source.fromFile(file).mkString
      val xstream = new XStream
      xstream.fromXML(xml)
    }

    val modelMap = readModel(knnModelFile).asInstanceOf[Map[ProcessName,KNN[EventVec]]]

    def evaluate(s: AdmSubject, subPaths: ProcessName, eventMap: EventCounts): Option[Boolean] = {
      val eventVec = EventType.values
        .map(e => eventMap
          .getOrElse(e,0))
        .toArray

      modelMap.get(subPaths) match {
        case Some(mdl) => println(subPaths.mkString(",")+" "+mdl.predict(eventVec).toString)
          Some(mdl.predict(eventVec) == 1)
        case _ => None
      }
    }
  }
}

class KNNTrainActor extends Actor with ActorLogging {
  import EventTypeKNN._
  val eventTypeKNNTrain = new EventTypeKNNTrain()

  def receive = {

    case (s: AdmSubject, subPathNodes: Set[AdmPathNode], eventMap: EventCounts) =>
      eventTypeKNNTrain.collect(s,subPathNodes.map(_.path),eventMap)
      sender() ! Ack

    case InitMsg => sender() ! Ack
    case CompleteMsg => eventTypeKNNTrain.testSelectWriteModels(3);"All Done!" //upon completion save trained models and print summary stats
    case x => log.error(s"Received Unknown Message: $x")
  }
}

class KNNActor extends Actor with ActorLogging {
  import EventTypeKNN._
  val eventTypeKNNEvaluate = new EventTypeKNNEvaluate("knnmodels.xml")

  def receive = {

    case (s: AdmSubject, subPathNodes: Set[AdmPathNode], eventMap: EventCounts) =>
      eventTypeKNNEvaluate.evaluate(s,subPathNodes.map(_.path),eventMap)
      sender() ! Ack

    case InitMsg => sender() ! Ack
    case CompleteMsg => "All Done" //upon completion save trained models and print summary stats
    case x => log.error(s"Received Unknown Message: $x")
  }
}
