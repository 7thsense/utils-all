package com.theseventhsense.utils.models

abstract class TLogContext extends Serializable {
  def context: Map[String, String]
  def shouldLog: Any => Boolean

  def keys: Iterable[String] = context.keys
}
