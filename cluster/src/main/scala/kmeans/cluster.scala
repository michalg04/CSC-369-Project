package kmeans

import org.apache.spark.SparkContext._
import scala.io._
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.rdd._
import org.apache.spark.rdd._
import org.apache.log4j.Logger
import org.apache.log4j.Level
import scala.collection._
import scala.collection.immutable.Vector
import scala.util.Random


object cluster {

	def main(args: Array[String]){

		Logger.getLogger("org").setLevel(Level.OFF)
		Logger.getLogger("akka").setLevel(Level.OFF)
		
		val conf = new SparkConf().setAppName("cluster")//.setMaster("local[4]")
		val sc = new SparkContext(conf)
	
		// Read in labels	
		val labels = sc.textFile("input/flattened.out").map(x => x.split(" ")(0))
		// Read in image vectors
		val input = sc.textFile("input/flattened.out").map(x => x.split(" ")).
																									filter(x => x.length == 2).
																									map(x => x(1).split("").map(x => x.toFloat))
		val vectorLength = input.collect()(0).length
	
		// Algorithm Parameters	
		val k = 14
		val MAX_ITER = 10
		val EPS = .0001


		//******************** RUN INITIAL CLUSTERING ****************

		// Randomly select datapoints from our input data to be clusters
		var oldCentroids = input.takeSample(false, k).zipWithIndex.map{case (v,k)=>(k,v)}.toArray

		// Map each data point to the index of the closest cluster
		var clusteredData = input.map(vec => (computeClosestCentroid(vec, oldCentroids, vectorLength),vec))
		println("(Cluster ID, number of pts in this cluster)")
		clusteredData.map{case (x,y) => x}.countByValue().foreach{println}
		
		// Compute new centroids
		var currCentroids = clusteredData.mapValues(v=>(v,1)).
														reduceByKey{(x,y) => (addVectors(x._1,y._1), x._2+y._2)}.
														mapValues{case (x,y) => divideByCount(x,y)}

		// Compute distance between old centroids and new centroids
		var dist = sc.parallelize(oldCentroids).keyBy(x => x._1).mapValues(x => x._2).
									leftOuterJoin(currCentroids).
									mapValues{case (x,None) => (x,x)
														case (x, Some(y)) => (x,y)}. 
									mapValues{case (x,y) => computeDistBetweenPoints(x,y)}.
									values.
									sum
			

		var num_iterations = 0
		
		// Run until convergence or maximum iterations have been performed
		while((dist > EPS) && num_iterations < MAX_ITER){
						num_iterations += 1
						oldCentroids = currCentroids.collect().toArray

						// Map each data point to the index of the closest cluster
						clusteredData = input.map(vec => (computeClosestCentroid(vec, oldCentroids, vectorLength),vec))
						
						// Compute new centroids based on previous clustering
						currCentroids = clusteredData.mapValues(v=>(v,1)).
																		reduceByKey{(x,y) => (addVectors(x._1,y._1), x._2+y._2)}.
																		mapValues{case (x,y) => divideByCount(x,y)}
						
						// If a cluster id is missing from new ones, add it back in from the old clusters
						for(key_value <- oldCentroids){	
							if(currCentroids.keys.collect() contains key_value._1 == false){
								currCentroids = currCentroids.union(sc.parallelize(Seq(key_value)))
							}
						}	
						
						// Compute distance between old centroids and new centroids
						dist = sc.parallelize(oldCentroids).keyBy(x => x._1).mapValues(x => x._2).
													leftOuterJoin(currCentroids).
													mapValues{case (x,None) => (x,x)
																		case (x, Some(y)) => (x,y)}. 
													mapValues{case (x,y) => computeDistBetweenPoints(x,y)}.
													values.
													sum
						println("dist "+dist)
		}
	println("(Cluster ID, number of pts in this cluster)")
	clusteredData.map(x => x._1).countByValue().foreach{println}
	// Return the cluster IDs for all of the input rows
	clusteredData.map(x => x._1).zipWithIndex.map{case (k,v)=>(v,k)}.
								join(labels.zipWithIndex.map{case (k,v)=>(v,k)}). // join on position to connect label with cluster
								values. // return cluster id and label
								saveAsTextFile("clusterIDs")						
	}
	// Return the cluster id of the closest centroid
	def computeClosestCentroid(vector:Array[Float], centroids:Array[(Int,Array[Float])],vectorLength:Int):Int={
		// Compute distance to each centroid
		val distToCentroids = centroids.
														map{case (index,x) => (index,vector.zip(x).
														map{y => (y._1-y._2)*(y._1-y._2)}.sum)}
		
		// Get the argmin (i.e. index of the closest centroid)
		distToCentroids.reduceLeft((p1,p2) => if(p2._2 < p1._2) p2 else p1)._1
	}	

	// Adds vectors. Used to compute new centroids
	def addVectors(x:Array[Float], y:Array[Float]):Array[Float]={
		x.zip(y).map{case (x,y) => (x+y)}.toArray
	}
	// Divide each element by a count. Helps compute new centroids
	def divideByCount(arr:Array[Float],count:Int):Array[Float]={
		arr.map(x=>x/count).toArray
		}

	// Compute Sum of Squares between points
	def computeDistBetweenPoints(vector1:Array[Float], vector2:Array[Float]):Float={
	vector1.zip(vector2).map{y => (y._1-y._2)*(y._1-y._2)}.sum
	}
}
