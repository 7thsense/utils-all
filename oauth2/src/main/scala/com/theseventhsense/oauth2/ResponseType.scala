package com.theseventhsense.oauth2

private sealed trait ResponseType {
  def name: String
  override def toString: String = name
}
private object ResponseType {
  case object Code extends ResponseType {
    val name = "code"
  }
}
