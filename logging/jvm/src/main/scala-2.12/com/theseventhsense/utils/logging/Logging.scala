package com.theseventhsense.utils.logging

import scala.collection.JavaConverters._

import com.typesafe.scalalogging.LazyLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Logging {
  def audit(auditType: String): Logger = {
    LoggerFactory.getLogger(s"audit.$auditType")
  }

  def toMap(
    cc: Product,
    extra: Map[String, String] = Map.empty
  ): java.util.Map[String, String] = {
    val values = cc.productIterator
    val ccMap = try {
      cc.getClass.getDeclaredFields
        .map(_.getName -> values.next().toString)
        .toMap
    } catch {
      case ex: IndexOutOfBoundsException =>
        val ccMap: Map[String, String] = Map.empty
        ccMap
    }
    val combinedMap: Map[String, String] = ccMap ++ extra
    val obfuscatedMap: Map[String, String] =
      combinedMap map {
        case (k, v) =>
          if (k == "password") {
            (k, v.hashCode.toString)
          } else {
            (k, v)
          }
      }
    toMap(obfuscatedMap)
  }

  def toMap(map: Map[String, String]): java.util.Map[String, String] = {
    map.asJava
  }
}

trait Logging extends LazyLogging
