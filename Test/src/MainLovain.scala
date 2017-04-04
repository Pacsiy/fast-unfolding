import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.mllib.fpm.PrefixSpan
import java.util._

object MainLovain {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("GraphXExample")
    conf.setMaster("local");

    // conf.setSparkHome("/usr/local/spark")
    val sc = new SparkContext(conf)
    val inputHashFunc = (id: String) => id.toLong
    var edgeRDD = sc.textFile("/usr/local/dga/spark-distributed-louvain-modularity/dga-graphx/examples/small_edges.tsv").map(row => {
      val tokens = row.split("\t").map(_.trim())
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
    val f = Graph.fromEdges(edgeRDD, None)
    val ranks = f.pageRank(0.0001)
      val sumup = f.outerJoinVertices(users) {
        case (uid, deg, Some(attrList)) => attrList
      }
      val nodecount = f.vertices.count()
      val ranksByUsername = ranks.outerJoinVertices(users) {
        case (id, rank, Some(attrList)) => (attrList, rank)
      }
      val pic = ranksByUsername.vertices.map {
        case (vid, ((username, v1, v2), rank)) => (v1*rank, v2*rank,rank)
      }.reduce((a, b) => (a._1 + b._1, a._2 + b._2,a._3 + b._3))
      println("" + (pic._1.toDouble / pic._3.toDouble) + "," + (pic._2.toDouble / pic._3.toDouble))

      println(ranksByUsername.vertices.top(ranksByUsername.vertices.collect().length)(Ordering.by(_._2._1)).mkString("\n"))
    
  }
}