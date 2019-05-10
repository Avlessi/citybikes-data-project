import sbt.Keys.libraryDependencies

name := "CityBikesClustering"

version := "0.1"

val org: String = "com.clustering"

lazy val baseSettings = Seq(
  organization := org,
  scalaVersion := "2.11.12",
  fork in Test := true,
  logBuffered in Test := false,
  parallelExecution in Test := false
)

lazy val assemblySettings = Seq(
  test in assembly := {},
  assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)
)

lazy val dependencies = new {

  val sparkOrg: String = "org.apache.spark"
  val scalaTestOrg: String = "org.scalatest"
  val typesafeOrg: String = "com.typesafe"

  val tsConfigV: String = "1.3.3"
  val sparkV: String = "2.3.0"
  val scalatestV: String = "3.0.5"

  lazy val tsConfig: ModuleID = typesafeOrg % "config" % tsConfigV
  lazy val sparkCore: ModuleID = sparkOrg %% "spark-core" % sparkV //% "provided,test"
  lazy val sparkSql: ModuleID = sparkOrg %% "spark-sql" % sparkV //% "provided,test"
  lazy val sparkML: ModuleID = sparkOrg %% "spark-mllib" % sparkV //% "provided,test"
  lazy val scalaTest: ModuleID = scalaTestOrg %% "scalatest" % scalatestV % "test"

  def getSparkBase: Seq[ModuleID] = Seq(sparkCore, sparkSql, sparkML).map(_ % "provided,test")
  def getConf: Seq[ModuleID] = Seq(tsConfig)
  def getScalaTest: Seq[ModuleID] = Seq(scalaTest)
}


lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(Defaults.itSettings)
  .settings(baseSettings ++ assemblySettings ++ Seq(
    libraryDependencies ++= dependencies.getConf,
      libraryDependencies ++= dependencies.getSparkBase,
    libraryDependencies ++= dependencies.getScalaTest

    ),
    mainClass in assembly := Some("com.clustering.BikesClusteringJobRunner"),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x: Any                        => MergeStrategy.first
    }
  )