package com.theseventhsense.datetime

import java.util.Date

/**
  * Created by erik on 6/15/16.
  */
abstract class AbstractImplicits extends Serializable {

  implicit def dateOrdering: Ordering[Date] =
    Ordering.fromLessThan(_.getTime < _.getTime)
}
