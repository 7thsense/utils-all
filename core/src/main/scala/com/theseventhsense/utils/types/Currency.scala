package com.theseventhsense.utils.types

/**
  * Created by erik on 11/11/15.
  */
case class Money(amount: Long, unit: Currency = Currency.USD) {
  override def toString: String = {
    val major = amount / Math.pow(10D, unit.decimals.toDouble).toLong
    val minor: Int = (amount % Math.pow(10D, unit.decimals.toDouble)).toInt
    f"${unit.symbol}%s$major%d.$minor%02d"
  }
}

sealed trait Currency {
  def decimals: Int

  def symbol: String

  def name: String
}

object Currency {

  case object USD extends Currency {
    val decimals = 2
    val symbol = "$"
    val name = "USD"
  }

  val all = Seq(USD)

  val default = USD

  def from(s: String): Currency = all.find(_.name == s).getOrElse(default)

}
