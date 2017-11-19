package com.theseventhsense.utils.persistence

/**
  * Marker class for external messages, allowing us to avoid reconfiguring Kryo
  */
trait AkkaMessage extends Serializable
