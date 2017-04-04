package LPA.GraphxMain

import org.apache.spark.graphx._
import org.apache.spark.graphx.lib.LabelPropagation
import org.apache.spark.rdd.RDD
import org.apache.spark._
import org.apache.spark.SparkContext._

object Main {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("GraphXExample")
    conf.setMaster("local");

    // conf.setSparkHome("/usr/local/spark")
    val sc = new SparkContext(conf)
    val inputHashFunc = (id: String) => id.toLong
    var edgeRDD = sc.textFile("/usr/local/dga/louvain-modularity/src/data/input/sample1.txt").map(row => {
      val tokens = row.split(",").map(_.trim())
      tokens.length match {
        case 2 => { new Edge(inputHashFunc(tokens(0)), inputHashFunc(tokens(1)), 1L) }
        case 3 => { new Edge(inputHashFunc(tokens(0)), inputHashFunc(tokens(1)), tokens(2).toLong) }
        case _ => { throw new IllegalArgumentException("invalid input line: " + row) }
      }
    })

    val users = sc.textFile("/usr/local/dga/spark-distributed-louvain-modularity/dga-graphx/examples/user.tsv").map { line =>
      val fields = line.split(",")
      (fields(0).toLong, (fields(1), fields(2).toDouble, fields(3).toDouble))
    }
    val graph = Graph.fromEdges(edgeRDD, None)
    
    val n = graph.vertices.collect().length;
    val labels = LabelPropagation.run(graph, n/2).cache()
    val attrs = labels.vertices.map { case (_, attr) => attr }.distinct.collect
    println(attrs.mkString(","))
    //attrs.foreach { x => println(x) }
  /*  val grahps = attrs.map(attr => {
      val vertices = labels.vertices.filter {
        case (_, someAttr) =>
          someAttr == attr
      }
      val con = vertices.map(f => f._1).collect()
      // vertices.foreach(x=>println(x._1))
      // println()
      Graph(vertices, labels.edges.filter { x => con.contains(x.srcId) && con.contains(x.dstId) });
    })
    println(grahps.length)
    grahps.foreach(f =>
      {
        println("---")
        println(f.vertices.collect().mkString(","))
      })*/
  /*  grahps.foreach(f => {
      val ranks = f.pageRank(0.0001)
      val sumup = f.outerJoinVertices(users) {
        case (uid, deg, Some(attrList)) => attrList
      }
      val nodecount = f.vertices.count()
      val ranksByUsername = ranks.outerJoinVertices(users) {
        case (id, rank, Some(attrList)) => (attrList, rank)
      }
      //   val ranksinUser = ranks.
      //  f.vertices.collect().foreach(println)

     /* val triCounts = f.triangleCount().vertices
      println("Edge Number: " + f.edges.count())
      println("Nodes Count: " + nodecount)
      if (nodecount >= 3) {
        val totalcount = nodecount * (nodecount - 1) * (nodecount - 2)
        println("Triangles: " + triCounts.map {
          case (vid, data) => data.toLong
        }.reduce(_ + _) / totalcount)

      }*/

      val pic = ranksByUsername.vertices.map {
        case (vid, ((username, v1, v2), rank)) => (v1, v2)
      }.reduce((a, b) => (a._1 + b._1, a._2 + b._2))
      println("" + (pic._1.toDouble / nodecount) + "," + (pic._2.toDouble / nodecount))

      println(ranksByUsername.vertices.top(ranksByUsername.vertices.collect().length)(Ordering.by(_._2._1)).mkString("\n"))
    })*/
  }
}