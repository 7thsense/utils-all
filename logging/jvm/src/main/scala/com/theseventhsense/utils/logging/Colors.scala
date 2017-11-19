package com.theseventhsense.utils.logging

object Colors {
  import scala.Console._

  def red(str: String): String = RED + str + RESET
  def blue(str: String): String = BLUE + str + RESET
  def cyan(str: String): String = CYAN + str + RESET
  def green(str: String): String = GREEN + str + RESET
  def magenta(str: String): String = MAGENTA + str + RESET
  def white(str: String): String = WHITE + str + RESET
  def black(str: String): String = BLACK + str + RESET
  def yellow(str: String): String = YELLOW + str + RESET

}
