// Comment to get more information during initialization
logLevel := Level.Warn

addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.13.1")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.24")
addSbtPlugin("org.portable-scala" % "sbt-crossproject"         % "0.5.0")  // (1)
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.5.0")  // (2)
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.1.0-M6")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.7")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
