package com.theseventhsense.utils.spark

import java.io.IOException

import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag

import com.theseventhsense.utils.logging.{LogContext, Logging}
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapred.{FileSplit, JobConf, LineRecordReader}

/**
  * Created by erik on 10/27/16.
  */
class TolerantLineRecordReader(job: JobConf,
                               split: FileSplit,
                               delimiterBytes: Array[Byte])(implicit lc: LogContext)
    extends LineRecordReader(job, split, delimiterBytes)
    with Logging {
  override def next(key: LongWritable, value: Text): Boolean =
    try {
      super.next(key, value)
    } catch {
      case e: IOException =>
        logger.trace("Captured IOException", e)
        false
    }
}
