package com.theseventhsense.utils.spark

import java.io.IOException

import org.apache.commons.compress.utils.Charsets
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapred._

import com.theseventhsense.utils.models.TLogContext

/**
  * Created by erik on 10/27/16.
  */
class TolerantTextInputFormat(implicit lc: TLogContext) extends TextInputFormat {
  override def configure(conf: JobConf): Unit = super.configure(conf)

  override def getSplits(job: JobConf, numSplits: Int): Array[InputSplit] =
    try {
      super.getSplits(job, numSplits)
    } catch {
      case ex: InvalidInputException => Array.empty
    }

  @throws[IOException]
  override def getRecordReader(
    genericSplit: InputSplit,
    job: JobConf,
    reporter: Reporter
  ): RecordReader[LongWritable, Text] = {
    reporter.setStatus(genericSplit.toString)
    val delimiter: String = job.get("textinputformat.record.delimiter")
    var recordDelimiterBytes: Array[Byte] = null
    if (null != delimiter)
      recordDelimiterBytes = delimiter.getBytes(Charsets.UTF_8)
    new TolerantLineRecordReader(
      job,
      genericSplit.asInstanceOf[FileSplit],
      recordDelimiterBytes
    )
  }
}
