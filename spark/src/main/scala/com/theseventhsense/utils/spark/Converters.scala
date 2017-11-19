package com.theseventhsense.utils.spark

import org.apache.spark.sql.functions._

object Converters {
  val toInt = udf[Int, String](_.toInt)
  val stripString = udf[String, String](_.trim)
  val toTimestamp = udf((first: String, second: String) => {
    java.sql.Timestamp.valueOf(first + " " + second)
  })
}
