package com.theseventhsense.utils.logging.logback
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import com.theseventhsense.utils.logging.Colors

class ColoredLevel extends ClassicConverter {
  def convert(event: ILoggingEvent): String = {
    event.getLevel match {
      case Level.TRACE => "[" + Colors.blue("trace") + "]"
      case Level.DEBUG => "[" + Colors.cyan("debug") + "]"
      case Level.INFO  => "[" + Colors.white("info") + "]"
      case Level.WARN  => "[" + Colors.yellow("warn") + "]"
      case Level.ERROR => "[" + Colors.red("error") + "]"
    }
  }
}
