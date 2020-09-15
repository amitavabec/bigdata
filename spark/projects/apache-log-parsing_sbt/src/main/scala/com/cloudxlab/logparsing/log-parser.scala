package com.cloudxlab.logparsing

import org.apache.spark._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext._

class Utils extends Serializable {
    val PATTERN = """^(\S+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] "(\S+) (\S+)(.*)" (\d{3}) (\S+)""".r

    def containsIP(line:String):Boolean = return line matches "^([0-9\\.]+) .*$"
    //Extract only IP
    def extractIP(line:String):(String) = {
        val pattern = "^([0-9\\.]+) .*$".r
        val pattern(ip:String) = line
        return (ip.toString)
    }

    def gettop10(accessLogs:RDD[String], sc:SparkContext, topn:Int):Array[(String,Int)] = {
        //Keep only the lines which have IP
        var ipaccesslogs = accessLogs.filter(containsIP)
        var cleanips = ipaccesslogs.map(extractIP(_))
        var ips_tuples = cleanips.map((_,1));
        var frequencies = ips_tuples.reduceByKey(_ + _);
        var sortedfrequencies = frequencies.sortBy(x => x._2, false)
        return sortedfrequencies.take(topn)
    }
	
	
	def parseLogLine(log: String):
	LogRecord = {
		val res = PATTERN.findFirstMatchIn(log) 
		if (res.isEmpty)
		{
			println("Rejected Log Line: " + log)
			LogRecord("Empty", "", "",  -1 )
		}
		else 
		{
			val m = res.get
			LogRecord(m.group(1), m.group(4),m.group(6), m.group(8).toInt)
		}
	}
}

object EntryPoint {
    val usage = """
        Usage: EntryPoint <how_many> <file_or_directory_in_hdfs>
        Eample: EntryPoint 10 /data/spark/project/NASA_access_log_Aug95.gz
    """
    
    def main(args: Array[String]) {
        
        if (args.length != 3) {
            println("Expected:3 , Provided: " + args.length)
            println(usage)
            return;
        }

        var utils = new Utils

        // Create a local StreamingContext with batch interval of 10 second
        val conf = new SparkConf().setAppName("WordCount")
        val sc = new SparkContext(conf);
        sc.setLogLevel("WARN")

        // var accessLogs = sc.textFile("/data/spark/project/access/access.log.45.gz")
        var accessLogs = sc.textFile(args(2))
        val top10 = utils.gettop10(accessLogs, sc, args(1).toInt)
        println("===== TOP 10 IP Addresses =====")
        for(i <- top10){
            println(i)
        }
		
		var accessLog = logFile.map(parseLogLine)
		val accessDf = accessLog.toDF()
		accessDf.printSchema
		accessDf.createOrReplaceTempView("nasalog")
		val output = spark.sql("select * from nasalog")
		output.createOrReplaceTempView("nasa_log")
		spark.sql("cache TABLE nasa_log")
		spark.sql("select url,count(*) as req_cnt from nasa_log where upper(url) like '%HTML%' group by url order by req_cnt desc LIMIT 10").show

		spark.sql("select host,count(*) as req_cnt from nasa_log group by host order by req_cnt desc LIMIT 5").show

		spark.sql("select substr(timeStamp,1,14) as timeFrame,count(*) as req_cnt from nasa_log group by substr(timeStamp,1,14) order by req_cnt desc LIMIT 5").show

		spark.sql("select substr(timeStamp,1,14) as timeFrame,count(*) as req_cnt from nasa_log group by substr(timeStamp,1,14) order by req_cnt  LIMIT 5").show

		spark.sql("select httpCode,count(*) as req_cnt from nasa_log group by httpCode ").show
    }
}
