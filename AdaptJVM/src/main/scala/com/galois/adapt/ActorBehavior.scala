package com.galois.adapt

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.galois.adapt.ServiceRegistryProtocol._
import scala.collection.mutable.{Map => MutableMap}


/*
 * This class provides a default implementation of `receive` which is the identity element of the
 * monoid over PartialFunction[Any,Unit] with operation `orElse`. Concretely, this lets us create a
 * bunch of traits all extending `BaseActorBehaviour` and use `super.receive` in their definition of
 * `super.receive`.
 */
trait BaseActorBehavior { s: Actor with ActorLogging =>
  def receive: PartialFunction[Any,Unit] = PartialFunction.empty
}


trait ServiceClient extends BaseActorBehavior { s: Actor with ActorLogging =>
  lazy val thisName = this.getClass.getSimpleName

  val registry: ActorRef
  def localReceive: PartialFunction[Any,Unit] = PartialFunction.empty
  val dependencies: List[String]
  def beginService(): Unit
  def endService(): Unit

  override def receive = localReceive orElse handleServiceManagerMessages orElse super.receive

  lazy val dependencyMap: MutableMap[String, Option[ActorRef]] = MutableMap(dependencies.map(_ -> None):_*)

  private def startIfReady() = {
    val depsSatisfied = dependencyMap forall (_._2.isDefined)
    if (depsSatisfied) {
      beginService()
      registry.!(PublishService(thisName, context.self))(context.self)
    } else {
      log.info(s"$thisName is waiting to satisfy more dependencies before publing service availability: $dependencyMap")
    }
  }

  private def handleServiceManagerMessages: PartialFunction[Any,Unit] = {

    case ServiceAvailable(serviceName, actorRef) =>
      log.info(s"$serviceName has announced its availability to $thisName")
      if (dependencyMap.keySet contains serviceName) {
        dependencyMap(serviceName) foreach ( s =>
          log.warning(s"Dependency is already available at actor: $s Replacing with: $actorRef")
        )
        dependencyMap += (serviceName -> Some(actorRef))
        startIfReady()
      } else {
        log.warning(s"Unplanned `Dependency Available` notice: $serviceName at: $actorRef")
      }

    case ServiceUnAvailable(serviceName) =>
      log.info(s"$serviceName has announced its UNavailability to $thisName")
      if (dependencyMap.keySet contains serviceName) {
        dependencyMap += (serviceName -> None)
//        registry ! UnSubscribeToService()
        registry.!(UnPublishService(thisName))(context.self)
        endService()
      } else {
        log.warning(s"Unplanned `Dependency Unavailable` notice: $serviceName")
      }

    case DoSubscriptions =>
      log.info(s"Announcing dependencies to Service Manager: ${dependencies.mkString(",")}")
      dependencies.foreach(d => registry.!(SubscribeToService(d))(context.self))
      startIfReady()
  }

  private case object DoSubscriptions
  context.self.!(DoSubscriptions)(context.self)
}

