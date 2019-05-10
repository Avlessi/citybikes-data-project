package com.clustering

import com.typesafe.config.Config
import org.apache.spark.ml.clustering.KMeans
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.{StringType, StructField, StructType}

/**
 * @author Oleksandr Avlesi
 */
class BikesClusteringJob(implicit spark: SparkSession, config: Config) {

  val inputFile: String = getClass.getResource(config.getString("inputFile")).getPath
  val outputDir: String = config.getString("outputPath")
  val clusterNumber: Int = config.getInt("clusterNumber")

  val inputSchema = StructType(
    Array(
      StructField(BikeClusteringColumns.id, StringType),
      StructField(BikeClusteringColumns.name, StringType),
      StructField(BikeClusteringColumns.address, StringType),
      StructField(BikeClusteringColumns.latitude, StringType),
      StructField(BikeClusteringColumns.longitude, StringType)
    )
  )

  def runJob(): Unit = {
    import spark.implicits._

    val df = spark
      .read
      .schema(inputSchema)
      .option("multiLine", "true")  // in input one json record does not fit into one line, so we need to activate multiline option
      .option("mode", "PERMISSIVE")
      .json(inputFile)
      .persist()  // when we have iterative operations on df, it's better to persist df thus distributing its chunks by data nodes

    // cast latitude and longitude columns to double
    val castDf = df
      .withColumn(BikeClusteringColumns.latitude, col(BikeClusteringColumns.latitude).cast("double"))
      .withColumn(BikeClusteringColumns.longitude, col(BikeClusteringColumns.longitude).cast("double"))

    // take only data where latitude and longitude are not null
    val filteredDf = castDf
      .filter(col(BikeClusteringColumns.latitude).isNotNull && col(BikeClusteringColumns.longitude).isNotNull)

    val bikeLocationDf = filteredDf.as[BikeLocation]

    // create vector assembler and make features column from latitude and longitude
    val vectorAssembler = new VectorAssembler()
      .setInputCols(Array(BikeClusteringColumns.latitude, BikeClusteringColumns.longitude))
      .setOutputCol("features")

    val featuredBikeLocationDf = vectorAssembler.transform(bikeLocationDf)

    // use kmeans clustering algorithm
    val kmeans = new KMeans().setK(clusterNumber)

    val clusterModel = kmeans.fit(featuredBikeLocationDf)

    // resulting df containing new column called predictions specifying a cluster
    val clusteredBikeLocation = clusterModel.transform(featuredBikeLocationDf)

    // write data to output
    clusteredBikeLocation
      .write
      .json(outputDir)
  }
}
