package com.clustering

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.spark.sql.SparkSession

/**
 * @author Oleksandr Avlesi
 */
object BikesClusteringJobRunner {
  def main(args: Array[String]): Unit = {

    implicit val spark: SparkSession = SparkSession
      .builder()
      .appName("BikesClusteringJob")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")

    // load config
    implicit val config: Config = ConfigFactory.load("default.conf")

    new BikesClusteringJob().runJob()
  }
}