package com.theseventhsense.utils

/**
  * Created by erik on 7/19/17.
  */
object Formatting {
  def formatIntWithCommas(num: Int): String =
    num.toString.reverseIterator.grouped(3).map(_.mkString).mkString(",").reverse

  def formatLongWithCommas(num: Long): String =
    num.toString.reverseIterator.grouped(3).map(_.mkString).mkString(",").reverse


  object Implicits {
    implicit class RichLong(val num: Long) extends AnyVal {
      def withCommas: String = formatLongWithCommas(num)
    }

    implicit class RichInt(val num: Int) extends AnyVal {
      def withCommas: String = formatIntWithCommas(num)
    }
  }

}
