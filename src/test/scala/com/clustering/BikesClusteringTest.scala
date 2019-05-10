package com.clustering

import java.io.File

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.commons.io.FileUtils
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}
/**
 * @author Oleksandr Avlesi
 */
class BikesClusteringTest extends FlatSpec with Matchers with BeforeAndAfterEach {

  implicit var spark: SparkSession = _
  implicit val config: Config = ConfigFactory.load("test.conf")
  val outputPath: String = config.getString("outputPath")

  val name: String = "BikesClusteringJobTest"

  val master: String = "local[*]"

  val configs: Map[String, String] = Map(
    "spark.ui.enabled" -> "false",
    "spark.sql.warehouse.dir" -> "/tmp/spark-warehouse",
    "spark.sql.session.timeZone" -> "UTC"
  )

  override def beforeEach(): Unit = {
    // delete output dir if exists
    new reflect.io.Directory(new File(outputPath)).deleteRecursively()

    val sc: SparkConf = new SparkConf()
      .setAppName(name)
      .setMaster(master)
      .setAll(configs)

    spark = SparkSession
      .builder()
      .config(sc)
      .getOrCreate()
  }

  it should "run job" in {
    new BikesClusteringJob().runJob()

    // check that output directory if not empty and contains json files
    FileUtils
      .listFiles(new File(config.getString("outputPath")), Array("json"), false)
      .size() should be > 0
  }

  override def afterEach(): Unit = {
    spark.close()
  }
}
