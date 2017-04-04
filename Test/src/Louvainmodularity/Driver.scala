package Louvainmodularity

import org.apache.spark.graphx.{ Edge, Graph }
import org.apache.spark.{ SparkContext, SparkConf }
import org.apache.log4j.{ Level, Logger }

object Driver {

  def main(args: Array[String]): Unit = {

    val config = LouvainConfig(
      "/usr/local/dga/louvain-modularity/src/data/input/sample1.txt",
      "/usr/local/dga/louvain-modularity/src/data/output/",
      20,
      2000,
      2,
      ",")

    // def deleteOutputDir(config: LouvainConfig): Unit = {
    //   val hadoopConf = new org.apache.hadoop.conf.Configuration()

    //   val hdfs = org.apache.hadoop.fs.FileSystem.get(new java.net.URI("hdfs://localhost:8020"), hadoopConf)

    //   try {
    //     hdfs.delete(new org.apache.hadoop.fs.Path(config.outputDir), true)
    //   }
    //   catch {
    //     case _ : Throwable => { }
    //   }
    // }

    // val conf = new SparkConf().setAppName("ApproxTriangles").setMaster("local[2]")
    // conf.set("spark.default.parallelism", (8).toString)
    // conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    // conf.set("spark.logConf", "true")
    // //sparkConf.getAll.foreach(println(_))
    // val sc = new SparkContext(conf)
    // Logger.getRootLogger.setLevel(Level.WARN)

    val sc = new SparkContext("local", "Louvain", "/usr/local/spark") //,
    //  List("target/scala-2.11/louvain-modularity_2.11-0.0.1.jar"))

    // deleteOutputDir(config)

    val louvain = new Louvain()
 
    val graph = louvain.run(sc, config)
    val users = sc.textFile("/usr/local/dga/spark-distributed-louvain-modularity/dga-graphx/examples/user.tsv").map { line =>
      val fields = line.split(",")
      (fields(0).toLong, (fields(1), fields(2).toDouble, fields(3).toDouble))
    }
    graph.foreach(f=>{
      val attrs = f.vertices.map { case (_, attr) => attr.community }.distinct.collect
println(attrs.mkString(","))
    /*
    val grahps = attrs.map(attr => {
      val vertices = f.vertices.filter {
        case (_, someAttr) =>
          someAttr.community == attr
      }
      val con = vertices.map(f => f._1).collect()
      // vertices.foreach(x=>println(x._1))
      // println()
      Graph(vertices, f.edges.filter { x => con.contains(x.srcId) && con.contains(x.dstId) });
    })
    println(grahps.length)
    grahps.foreach(f =>
      {
        println("---")
        println(f.vertices.collect().mkString(","))
      })
      * 
      */
    })
    
    /*
    grahps.foreach(f => {
      val ranks = f.pageRank(0.0001)
      val sumup = f.outerJoinVertices(users) {
        case (uid, deg, Some(attrList)) => attrList
      }
      val ranksByUsername = ranks.outerJoinVertices(users) {
        case (id, rank, Some(attrList)) => (attrList, rank)
      }
      val nodecount = f.vertices.count()
      val triCounts = f.triangleCount().vertices
      println("Edge Number: " + f.edges.count())
      println("Nodes Count: " + nodecount)
      if (nodecount >= 3) {
        val totalcount = nodecount * (nodecount - 1) * (nodecount - 2)
        println("Triangles: " + triCounts.map {
          case (vid, data) => data.toLong
        }.reduce(_ + _) / totalcount)

      }
      val pic = ranksByUsername.vertices.map {
        case (vid, ((username, v1, v2), rank)) => (v1, v2)
      }.reduce((a, b) => (a._1 + b._1, a._2 + b._2))
      println("" + (pic._1.toDouble / nodecount) + "," + (pic._2.toDouble / nodecount))

      println(ranksByUsername.vertices.top(ranksByUsername.vertices.collect().length)(Ordering.by(_._2._1)).mkString("\n"))

      //   println(f.vertices.collect().mkString("\n"))

    })*/
  }
}