package com.theseventhsense.utils.persistence

/**
  * Marker class for domain objects
  */
object DomainObject {
  type Id = Long
}

trait DomainObject extends AkkaMessage {}
