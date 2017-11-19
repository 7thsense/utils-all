resolvers in ThisBuild ++= Seq(
  Resolver.jcenterRepo,
  Resolver.bintrayRepo("7thsense", "maven")
)

lazy val `utils-all` = project
  .in(file("."))
  .settings(Common.settings)
  .aggregate(`utils-cats`.jvm)
  .aggregate(`utils-cats`.js)
  .aggregate(`utils-core`.jvm)
  .aggregate(`utils-core`.js)
  .aggregate(`utils-logging`.jvm)
  .aggregate(`utils-logging`.js)
  .aggregate(`utils-testing`)
  .aggregate(`utils-play`)
  .aggregate(`utils-play-testing`)
  .aggregate(`utils-oauth2`)
  .aggregate(`utils-slick`)
  .aggregate(`utils-slick-testing`)

lazy val `utils-testing` = project
  .in(file("testing"))
  .settings(Common.settings)
  .dependsOn(`utils-logging`.jvm)
  .settings(libraryDependencies ++= Dependencies.CatsScalatestCompile.value)
  .settings(libraryDependencies ++= Dependencies.ScalaTestCompile.value)
  .settings(libraryDependencies ++= Dependencies.Akka.value)
  .settings(libraryDependencies ++= Dependencies.AkkaTestKit.value)
  .settings(libraryDependencies ++= Dependencies.Mockito.value)

lazy val `utils-core` = crossProject
  .crossType(CrossType.Pure)
  .in(file("core"))
  .dependsOn(`utils-datetime-circe`)
  .settings(
    libraryDependencies ++= Dependencies.Circe.value ++
      Dependencies.ScalaTest.value
  )
  //.settings(libraryDependencies += Dependencies.SSDateTime.value)
  .settings(Common.settings)
  .jsSettings(Common.jsSettings: _*)

lazy val `utils-core-jvm` = `utils-core`.jvm

lazy val `utils-core-js` = `utils-core`.js

lazy val `utils-datetime` = crossProject
  .crossType(CrossType.Full)
  .in(file("datetime"))
  .settings(Common.settings)
  .jsSettings(Common.jsSettings: _*)
  //  .enablePlugins(ScalaJSBundlerPlugin)
  .settings(
    name := "utils-datetime",
    libraryDependencies ++= Dependencies.Cats.value ++ Dependencies.ScalaJavaTime.value
  )
  .jsSettings(libraryDependencies ++= Dependencies.ScalaJsMoment.value)

lazy val `utils-datetime-jvm` = `utils-datetime`.jvm

lazy val `utils-datetime-js` = `utils-datetime`.js

lazy val `utils-datetime-circe` = crossProject
  .crossType(CrossType.Pure)
  .in(file("datetime/codecs-circe"))
  .dependsOn(`utils-datetime`)
  .settings(Common.settings)
  .jsSettings(Common.jsSettings: _*)
  .settings(
    name := "utils-datetime-circe",
    libraryDependencies ++= Dependencies.Circe.value
  )

lazy val `utils-datetime-circe-jvm` = `utils-datetime-circe`.jvm

lazy val `utils-datetime-circe-js` = `utils-datetime-circe`.js

lazy val `utils-datetime-playjson` = project
  .in(file("datetime/codecs-playjson"))
  .dependsOn(`utils-datetime`.jvm)
  .settings(Common.settings)
  .settings(
    name := "utils-datetime-playjson",
    libraryDependencies ++= Dependencies.PlayJson.value
  )

lazy val `utils-cats` = crossProject
  .crossType(CrossType.Pure)
  .in(file("cats"))
  .settings(Common.settings)
  .jsSettings(Common.jsSettings: _*)
  .settings(
    libraryDependencies ++= Dependencies.Cats.value ++
      Dependencies.ScalaTest.value
  )

lazy val `utils-cats-jvm` = `utils-cats`.jvm

lazy val `util-cats-js` = `utils-cats`.js

lazy val `utils-logging` = crossProject
  .crossType(CrossType.Full)
  .in(file("logging"))
  .settings(Common.settings)
  .jsSettings(Common.jsSettings: _*)
  .jsSettings(libraryDependencies ++= Dependencies.Slogging.value)
  .jvmSettings(
    libraryDependencies ++= Dependencies.ScalaLogging.value ++
      Dependencies.Logback.value
  )

lazy val `utils-logging-jvm` = `utils-logging`.jvm

lazy val `utils-logging-js` = `utils-logging`.js

lazy val `utils-play` =
  project
    .in(file("play"))
    .settings(Common.settings)

//lazy val `utils-spark` =
//  project
//    .in(file("spark"))
//    .settings(Common.sparkSettings)
//    .dependsOn(`utils-logging`.jvm)
//    .dependsOn(`utils-datetime-circe`.jvm)
//    .settings(libraryDependencies ++= Dependencies.Akka.value)
//    .settings(libraryDependencies ++= Dependencies.Spark.value)
//    .settings(libraryDependencies ++= Dependencies.SSDateTimeCirce.value)

lazy val `utils-play-testing` =
  project
    .in(file("play-testing"))
    .settings(Common.settings)
    .settings(libraryDependencies ++= Dependencies.PlaySlick.value)
    .settings(libraryDependencies ++= Dependencies.ScalaTestPlusPlay.value)

lazy val `utils-slick` = project
  .in(file("slick"))
  .settings(Common.settings)
  .dependsOn(`utils-logging`.jvm, `utils-core`.jvm, `utils-datetime`.jvm)
  .settings(parallelExecution in Test := false, fork in Test := true)
  .settings(libraryDependencies ++= Dependencies.Akka.value)
  .settings(libraryDependencies ++= Dependencies.SlickPg.value)
  .settings(libraryDependencies ++= Dependencies.PostgresDriver.value)
  .settings(libraryDependencies ++= Dependencies.Guice.value)

lazy val `utils-slick-testing` = project
  .in(file("slick-testing"))
  .settings(Common.settings)
  .dependsOn(
    `utils-slick`,
    `utils-testing`,
    `utils-logging`.jvm,
    `utils-core`.jvm
  )
  .settings(parallelExecution in Test := false, fork in Test := true)
  .settings(libraryDependencies ++= Dependencies.ScalaTestCompile.value)

lazy val `utils-oauth2` = project
  .in(file("oauth2"))
  .settings(Common.settings)
  .settings(parallelExecution in Test := false, fork in Test := true)
  .dependsOn(
    `utils-testing` % "test",
    `utils-slick-testing` % "test",
    `utils-play-testing` % "test",
    `utils-cats`.jvm,
    `utils-core`.jvm,
    `utils-logging`.jvm,
    `utils-slick`,
    `utils-datetime-circe`.jvm,
    `utils-datetime-playjson`
  )
  .settings(libraryDependencies ++= Dependencies.Auth0JavaJwt)
  .settings(libraryDependencies ++= Dependencies.Enums.value)
  .settings(libraryDependencies ++= Dependencies.RedisScala.value)
  .settings(libraryDependencies ++= Dependencies.PlaySlick.value)
  .settings(libraryDependencies ++= Dependencies.PlayCache.value)
  .settings(libraryDependencies ++= Dependencies.PlayWs.value)
  .settings(libraryDependencies ++= Dependencies.PlayJson.value)
  .settings(libraryDependencies ++= Dependencies.PlayCirce.value)
  .settings(libraryDependencies ++= Dependencies.PlayMockWs.value)
  .settings(libraryDependencies ++= Dependencies.PureConfig.value)



