val scalaV = "2.11.11"   // "2.12.2"  // Scala 2.12 requires JVM 1.8.0_111 or newer.
val akkaV = "2.5.3"
val akkaHttpV = "10.0.9"

resolvers += Resolver.jcenterRepo  // for akka persistence in memory

lazy val adapt = (project in file(".")).settings(
  name := "adapt",
  version := "0.5",
  organization := "com.galois",
  scalaVersion := scalaV,

  autoScalaLibrary := false,
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-library" % scalaV,
    "com.typesafe" % "config" % "1.3.1",
    "org.scalatest" %% "scalatest" % "3.0.0", // % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
    "org.apache.avro" % "avro" % "1.8.1",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.7.2",
    "com.thinkaurelius.titan" % "titan-core" % "1.0.0",
    //  "org.apache.tinkerpop" % "tinkergraph-gremlin" % "3.2.3",
    //  "org.slf4j" % "slf4j-api" % "1.7.25",
    "com.typesafe.akka" %% "akka-actor" % akkaV,
//    "com.typesafe.akka" %% "akka-cluster" % akkaV,
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpV,
//    "com.typesafe.akka" %% "akka-persistence" % akkaV,
//    "com.typesafe.akka" %% "akka-cluster-tools" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-stream-kafka" % "0.16",
    // "com.typesafe.akka" %% "akka-testkit" % akkaV % "test"
    // "com.github.scopt" %% "scopt" % "3.5.0",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
//    "com.github.dnvriend" %% "akka-persistence-inmemory" % "1.3.18",
    "org.mapdb" % "mapdb" % "3.0.5",
    // Titan related
    "com.thinkaurelius.titan" % "titan-core" % "1.0.0" excludeAll ExclusionRule(organization = "org.slf4j"),
    "com.thinkaurelius.titan" % "titan-cassandra" % "1.0.0" excludeAll ExclusionRule(organization = "org.slf4j"),
    "org.apache.cassandra" % "cassandra-all" % "2.1"  excludeAll ExclusionRule(organization = "org.slf4j")
  ),

//  fork in run := true,
//  javaOptions in run ++= Seq("-Xmx6G"),

  {
  // Compile Avro schema at the command line with `sbt avroCompile`
    lazy val avroCompile = taskKey[Unit]("Compile Avro sources from the schema")
    val avroToolsJarPath = "lib/avro-tools-1.8.1.jar"
    val avroSpecPath = "src/main/avro/TCCDMDatum14.avdl"
    // TODO Now takes two commands to compile schema, check with Ryan on how to change build file...
    // java -jar lib/avro-tools-1.8.1.jar idl src/main/avro/CDM14.avdl src/main/avro/CDM14.avpr
    // java -jar lib/avro-tools-1.8.1.jar compile protocol src/main/avro/CDM14.avpr src/main/java/
    avroCompile := s"java -jar $avroToolsJarPath compile schema $avroSpecPath target/scala-2.11/src_managed/main/".!
  },

  // Run the Ingest main class at the command line with `sbt run`
  //mainClass in (Compile, run) := Some("com.galois.adapt.scepter.SimpleTestRunner") //Some("com.galois.adapt.Ingest")
  mainClass in assembly := Some("com.galois.adapt.Application"),

  // Do not buffer test output (which is the default) so that all test results are shown as they happen (helpful for async or timeout results)
  logBuffered in Test := false,

  assemblyMergeStrategy in assembly := {
    case PathList("reference.conf") => MergeStrategy.concat
    case PathList("META-INF", xs@_*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
)


lazy val scepter = (project in file("scepter")).settings(
  name := "scepter",
  version := "0.1",
  organization := "com.galois",
  scalaVersion := scalaV,

  libraryDependencies ++= Seq(
    // "org.scalaj" %% "scalaj-http" % "2.3.0",
    "com.github.scopt" %% "scopt" % "3.5.0"
  ),
  mainClass in (Compile, run) := Some("com.galois.adapt.scepter.Wrapper"),
  mainClass in assembly := Some("com.galois.adapt.scepter.Wrapper")
)