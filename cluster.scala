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


/*
Input
k -  a number of clusters
file_name - input file to perform clustering on

Output
list of cluster ids


*/
object cluster {

	def main(args: Array[String]){

		Logger.getLogger("org").setLevel(Level.OFF)
		Logger.getLogger("akka").setLevel(Level.OFF)
		
		val conf = new SparkConf().setAppName("cluster")
		val sc = new SparkContext(conf)
		
		// Read in each line and convert to a list of integers
		val input = sc.textFile("input/test.txt").map(_.split("").map(_.toDouble))
		val vectorLength = input.collect()(0).length
		
		// Should make some of this a command line input later
		val k = 2
		val MAX_ITER = 10
		val EPS = .01


		//******************** RUN INITIAL CLUSTERING ****************

		// Randomly select datapoints from our input data to be clusters
		//		var oldCentroids = sc.parallelize(input.takeSample(false, k).zipWithIndex).keyBy(_._2).mapValues(x=>x._1)
		var oldCentroids = input.takeSample(false, k).zipWithIndex.map{case (v,k)=>(k,v)}.toArray
		// Map each data point to the index of the closest cluster
		var clusteredData = input.map(vec => (computeClosestCentroid(vec, oldCentroids, vectorLength),vec))
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
		}

	// Return the cluster IDs for all of the input rows
	clusteredData.map(x => x._1).saveAsTextFile("clusterIDs")					
	}

	def computeClosestCentroid(vector:Array[Double], centroids:Array[(Int,Array[Double])],vectorLength:Int):Int={
		// Compute distance to each centroid
		//val distToCentroids = centroids.
		//												map{case (index,x) => (index,vector.zip(x).
		//												map{y => (y._1-y._2)*(y._1-y._2)}.sum)}
		//
		// Get the argmin (i.e. index of the closest centroid)
		//distToCentroids.reduceLeft((p1,p2) => if(p2._2 < p1._2) p2 else p1)._1

		var i = 0
		var min = 100000000.0
		var argmin = 0

		while (i < centroids.size){
			var j = 0
			var totalDiff = 0.0
			// Get sum of squared differences for each centroid
			while (j < vector.size){
				totalDiff += (vector(i)-centroids(i)._2(j))*(vector(i)-centroids(i)._2(j))
				j += 1
			}
			if(min > totalDiff){
				min = totalDiff
				argmin = i
			}
			i+= 1
		}

		argmin
	}	

		
	def addVectors(x:Array[Double], y:Array[Double]):Array[Double]={
		//x.zip(y).map{case (x,y) => (x+y)}
		val size = x.size
		var i = 0
		var result = new Array[Double](x.size)
		while(i < size){
			result(i)=x(i) + y(i)
			i+=1
		}
		result

	}
	def divideByCount(arr:Array[Double],count:Int):Array[Double]={
		//arr.map(x=>x/count)
		var i = 0
		var result = new Array[Double](arr.size)
		while(i < arr.size){
			result(i) = arr(i)/count
			i+=1
		}
		result

		}

	// Compute Sum of Squares between points
	def computeDistBetweenPoints(vector1:Array[Double], vector2:Array[Double]):Double={
		var total = 0.0
		var i = 0
		while(i < vector1.size){
			var dist = vector1(i) - vector2(i)
			total += dist*dist
			i += 1	
		}
		total
	}


}
