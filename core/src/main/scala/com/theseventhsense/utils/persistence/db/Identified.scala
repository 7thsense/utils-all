package com.theseventhsense.utils.persistence.db

trait BaseId extends Any {
  def id: Long

  override def toString: String = id.toString
}

abstract class Identified[Id <: BaseId, T] extends Product with Serializable {
  def id: Id

  def withId(id: Id): T
}
