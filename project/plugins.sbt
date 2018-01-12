// Comment to get more information during initialization
logLevel := Level.Warn

addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.21")
addSbtPlugin("org.portable-scala" % "sbt-crossproject"         % "0.3.0")  // (1)
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.3.0")  // (2)
addSbtPlugin("org.scala-native"   % "sbt-scala-native"         % "0.3.6")  // (3)
addSbtPlugin("org.jetbrains" % "sbt-ide-settings" % "1.0.0")
addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.2")
