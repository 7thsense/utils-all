// Comment to get more information during initialization
logLevel := Level.Warn

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.10.0")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.22")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.2")
