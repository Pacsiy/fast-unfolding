package graph.AssociateRule

import Array._
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import org.apache.spark.graphx._
import org.apache.spark.graphx.lib.LabelPropagation
import org.apache.spark.rdd.RDD
import org.apache.spark._
import org.apache.spark.SparkContext._
import scala.io.Source
import org.apache.spark.mllib.fpm.PrefixSpan
import org.apache.spark.api.java.JavaSparkContext

object MainSP {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("GraphXExample")
    conf.setMaster("local");

    val sc = new SparkContext(conf)
    val teststr = "aaa,bbb"

    val inputHashFunc = (id: String) => id.toLong
    var edgeRDD = sc.textFile("/usr/local/dga/spark-distributed-louvain-modularity/dga-graphx/examples/testfile.tsv").map(row => {
      val tokens = row.split("\t").map(_.trim())
      tokens.length match {
        // case 2 => { new Edge(inputHashFunc(tokens(0)), inputHashFunc(tokens(1)), ) }
        case 4 => { new Edge(inputHashFunc(tokens(0)), inputHashFunc(tokens(1)), Array(tokens(2), tokens(3))) }
        case _ => { throw new IllegalArgumentException("invalid input line: " + row) }
      }
    })
    var typenames = Array("message", "call")
    // var typenames = sc.parallelize(Source.fromFile("/usr/local/dga/spark-distributed-louvain-modularity/dga-graphx/examples/type.tsv").getLines());
    //  println(typenames.length)
    //   var str = sc.parallelize(teststr)
    val typenumber = 2
    val interval = 1 //hour
    val graph = Graph.fromEdges(edgeRDD, None)

    var messages: VertexRDD[Array[Relation]] = graph.aggregateMessages(triplet => {
      val arr = getRelations(triplet.attr(0), triplet.attr(1), triplet.srcId, triplet.dstId, typenumber)
      triplet.sendToDst(arr(1))
      triplet.sendToSrc(arr(0))
    },
      (a, b) => concat(a, b))

    val assoiate_sets = messages /*.filter(g => g._1 == 2)*/ .map /*[Seq[Seq[Array[Array[Array[Long]]]]]]*/ (f => {
      // println("start")
      val ab = f._2.sortWith((a, b) => a.time.before(b.time))

      val abnumber = collection.mutable.ArrayBuffer[Long]()
      val abset = collection.mutable.HashSet[Long]()
      for (i <- 0 until ab.length) {
        val intval = ab(i).id
        abnumber += intval
        abset += intval
      }
      println("aaa--" + abnumber.mkString(","))

      val result = collection.mutable.ArrayBuffer[Seq[Array[Array[Long]]]]()
      for (typeedge <- abset) {
        var j = 0
        val subarr = collection.mutable.ArrayBuffer[Array[Array[Long]]]()

        while (j < abnumber.length) {
          val longarr = collection.mutable.ArrayBuffer[Array[Long]]()
          val index = j
          while (j < abnumber.length && abnumber(j) != typeedge && (ab(j).time.getTime.-(ab(index).time.getTime)) < interval * 1000 * 60 * 60) {
            longarr.+=(Array(abnumber(j)))
            j += 1
          }
          while (j < abnumber.length && abnumber(j) != typeedge) { j += 1 }
          subarr.+=(longarr.toArray)
          j += 1
        }

        //id,subarr
        val idarr = Array(typeedge)
        val subarrpre = collection.mutable.ArrayBuffer[Array[Array[Long]]]()
        val subarrpost = collection.mutable.ArrayBuffer[Array[Array[Long]]]()

        for (k <- 0 until subarr.length) {
          if (k == 0) {
            subarrpre.+=(subarr(k))
          } else {
            val pre = collection.mutable.ArrayBuffer[Array[Long]]()
            pre.++=(subarr(k))
            pre.+=:(idarr)
            subarrpre.+=(pre.toArray)
          }
          if (k == subarr.length - 1) {

            subarrpost.+=(subarr(k))
          } else {
            val post = collection.mutable.ArrayBuffer[Array[Long]]()
            post.++=(subarr(k))
            post.+=(idarr)
            subarrpost.+=(post.toArray)
          }

        }

        result.+=(subarrpre.toSeq)

        result.+=(subarrpost.toSeq)

      }
      //   println(result.length)
      (f._1, result.toSeq)
    }).reduceByKey((a, b) => (a.++(b)))

    val keys = assoiate_sets.keys.distinct().collect()
    keys.map { x =>
      {
        val subseq = assoiate_sets.filter(f => f._1 == x).map(f => f._2.flatten).collect()
        val seqrdd = subseq.map { x => sc.parallelize(x) }
        val freqs = seqrdd.map { f =>
          {
            val prefixSpan = new PrefixSpan()
              .setMinSupport(0.5)
              .setMaxPatternLength(5)
            val model = prefixSpan.run(f)
            val re = model.freqSequences.collect().filter { f => f.sequence.length != 1 }.map { freqSequence =>
              (freqSequence.sequence.map(_.mkString("[", ", ", "]")).mkString("[", ", ", "]"), (freqSequence.freq.toDouble / f.count()))
            }
            // println(re.mkString(","))
            re
          }
        }.reduce((a, b) => concat(a, b))
        val freqrdd = sc.parallelize(freqs)
        val rules = freqrdd.reduceByKey((a, b) => {
          if (a > b) a
          else b
        }).sortBy(f => f._2, false).foreach(f => println("Vertex " + x + ":" + getOrginalInfo(f._1, x, typenames) + ":" + f._2))
        rules
      }
    } //.reduce((a,b)=>concat(a,b))
    //   val rulsrdd = sc.parallelize(ruls)

  }

  def getOrginalInfo(str: String, tar: Long, typenames: Array[String]): String = {
    val targetArr = collection.mutable.ArrayBuffer[Long]()

    val values = str.substring(1, str.length() - 1).split(",").map { x => (x.trim().substring(1, x.trim().length() - 1)).toLong }
    val strs = values.map { x => decode(x, tar, typenames) }
    strs.mkString(",")
  }
  def decode(id: Long, tar: Long, typenames: Array[String]): String = {
    val mark = id & 1

    val len = (typenames.length.toInt + 1) / 2
    val point = id >> (len + 1)
    val typename = typenames(((id >> 1) & (1 << len - 1)).toInt)
    var result = tar + "-" + typename + "->" + point
    if (mark == 0) {
      result = point + "-" + typename + "->" + tar
    }
    result

  }

  def getRelations(timestamp: String, types: String, src: Long, dst: Long, typenumber: Int): Array[Array[Relation]] = {
    val timestamps = timestamp.split(",")
    val typesarr = types.split(",")
    val ab = collection.mutable.ArrayBuffer[Relation]()
    val abdst = collection.mutable.ArrayBuffer[Relation]()
    for (i <- 0 until timestamps.length) {
      val ts = getTimestamp(timestamps(i))
      ab += new Relation(ts, typesarr(i).toInt, src, dst, typenumber, 1)
      abdst += new Relation(ts, typesarr(i).toInt, src, dst, typenumber, 0)
      //      println(src+" "+dst)
    }
    Array(ab.toArray, abdst.toArray)
  }
  /*
      * change date string to timestamp value
      */
  def getTimestamp(x: String): java.sql.Timestamp = {
    //       "20151021235349"
    val format = new SimpleDateFormat("yyyyMMddHHmmss")

    //var ts = new Timestamp(System.currentTimeMillis());
    try {
      if (x == "")
        return null
      else {
        val d = format.parse(x);
        val t = new Timestamp(d.getTime());
        return t
      }
    } catch {
      case e: Exception => println("cdr parse timestamp wrong")
    }
    return null
  }
}