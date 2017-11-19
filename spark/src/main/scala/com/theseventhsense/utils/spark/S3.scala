package com.theseventhsense.utils.spark

import com.amazonaws.services.s3.AmazonS3Client

import scala.io.Source

object S3 {
  private lazy val client = new AmazonS3Client()

  //private val S3UrlRegex = """/s3:\/\/(\\w+)\/(.*)/""".r
  private val S3UrlRegex = """s3://([-\w]+)/(.*)""".r
  def parseS3Url(urlString: String): Option[(String, String)] = {
    S3UrlRegex.findFirstMatchIn(urlString).map { m =>
      (m.group(1), m.group(2))
    }
  }

  def loadFromS3Url(url: String): String =
    (loadFromS3 _).tupled(
      parseS3Url(url)
        .getOrElse(throw new RuntimeException(s"Invalid s3 url: $url"))
    )

  def loadFromS3(bucketName: String, key: String): String = {
    val s3Object = client.getObject(bucketName, key)
    Source.fromInputStream(s3Object.getObjectContent).mkString
  }

}
