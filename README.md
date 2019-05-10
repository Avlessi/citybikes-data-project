# Data engineering project providing citybikes stations clustering

## Data: Static geographical information of CityBike‘s stations in Brisbane (“Brisbane_CityBike.json”)

## Instructions
Propose a code to perform a clustering based on either the location or characteristics of bike stations.
Any clustering library can be used no assessment is done on the choice of the library. The data is a subset provided by
the business.
It should be developed as close as a real industrialized process.
In production this code should be launched daily on 10 Go of data, the choice of the platform is up to you:
it can be run on a Spark cluster or Kubernetes Cluster or Azure function.

## Implementation, explanation of technical choices and industrialization propositions

- Chosen language is Scala
- Chosen data-processing engine is Spark
- Chosen clustering algorithm is K-means. The choice is related to the fact that it is the most commonly used clustering
algorithm, it clusters data into a predefined number of clusters.
As a library implementing K-means, I chose Spark MLlib for its parallelization capabilities.
- Input data is json file. Each record has <id, name, address, latitude, longitude> fields. An analysis showed that there can
be anomalies. For example, there are several records with complex field <coordinates> containing latitude and longitude.
Another problem is that langitude or longitude can have not an expected double value, but string (see record with id=7).
Also there was a record having a field <position>.
I needed to have data schema relevant for all cases. So I specified a String as an expected type of every field (here is an extract from BikesClusteringJob):
```scala
val inputSchema = StructType(
    Array(
      StructField(BikeClusteringColumns.id, StringType),
      StructField(BikeClusteringColumns.name, StringType),
      StructField(BikeClusteringColumns.address, StringType),
      StructField(BikeClusteringColumns.latitude, StringType),
      StructField(BikeClusteringColumns.longitude, StringType)
    )
  )
```
And I threw away <coordinates> and <position> data fields deciding to consider them as mistaken and not providing value.

Further after dataframe loading, I cast its latitude and longitude columns to Double. In records where this cast is not possible, values will become null. Then I filter dataframe selecting records where latitude and longitude have non null values. 

Afterwards I apply k-means algorithm to this cleared dataframe and get a new dataframe with additional column <predictions> specifying a cluster for a given record.

- The number of clusters for kmeans algorithm, input path and output path are configurable since they are specified in config files. For main part it is default.conf, for test it is relevantly test.conf. For better industrialization, I would put these files to the cloud bucket and give it to job as parameters when running it. So job would be completely independent from its configuration.

- A project contains a test. It is called com.clustering.BikesClusteringTest. To run it, use command:
```
sbt test
```
The test is quite simple. It runs job on Spark locally and verifies if there's an output.

- Main project class is BikesClusteringJobRunner. It is responsible for launching a job (job itself is represented by com.clustering.BikeClusteringJob class). In order to run the job on a real Spark cluster, you need to run:
```
sbt assembly
```
to create a jar and then you can submit it on your Spark cluster.
For example if I wanted to run it on GCP Dataproc (Google managed Hadoop cluster), I would run:
``` gcloud dataproc jobs submit hadoop --cluster <cluster-name> \
  --jar file://<path to jar> \
  --class com.clustering.BikesClusteringJobRunner
```

Class BikesClusteringJobRunner uses such Spark configuration:
```
 val spark: SparkSession = SparkSession
      .builder()
      .appName("BikesClusteringJob")
      .getOrCreate()
```
Here we do not specify <master> property, since the information about data nodes to run processing will be received by job from YARN manager of cluster.

- According to input, job should be run 1 time a day. In order to do this scheduling, in production I would use AirFlow or another scheduler.

- To analyze a job behaviour I would look at Spark UI or History Server.

- Another thing that we could do in production is to create a dedicated cluster any time we run a job. In this case I would use Terraform scripts to define and create cluster on demand.

- Another things that should be done is analysis of clustering results. We could do create a visualization to get insight about its correctness.
