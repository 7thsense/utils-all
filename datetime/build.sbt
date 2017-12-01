val scala211Version = "2.11.8"
val scala212Version = "2.12.4"

resolvers in ThisBuild ++= Seq(
  Resolver.bintrayRepo("easel", "maven"),
  Resolver.bintrayRepo("7thsense", "maven")
)

lazy val ssUtilsDatetimeRoot = project
  .in(file("."))
  .aggregate(ssUtilsDatetimeJS,
             ssUtilsDatetimeJVM,
             ssUtilsDatetimeCodecsCirceJS,
             ssUtilsDatetimeCodecsCirceJVM,
             ssUtilsDatetimeCodecsPlay)
  .settings(
    publish := {},
    publishLocal := {},
    crossScalaVersions := Seq(scala211Version, scala212Version),
    scalaVersion := scala212Version
  )

val CommonSettings = Seq(
  organization := "com.theseventhsense",
  version := "0.1.16",
  isSnapshot := version.value.contains("-SNAPSHOT"),
  publishMavenStyle := true,
  bintrayOrganization := Some("7thsense"),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
  crossScalaVersions := Seq(scala211Version, scala212Version),
  scalaVersion := scala212Version
)

lazy val ssUtilsDatetime = crossProject
  .crossType(CrossType.Full)
  .in(file("."))
  .settings(CommonSettings: _*)
  .settings(
    name := "utils-datetime",
    libraryDependencies ++= Dependencies.Cats.value ++ Dependencies.ScalaJavaTime.value
  )
  .jsSettings(jsEnv := new org.scalajs.jsenv.nodejs.NodeJSEnv())
  .jsSettings(
    libraryDependencies += "ru.pavkin" %%% "scala-js-momentjs" % "0.9.0"
  )

lazy val ssUtilsDatetimeJVM = ssUtilsDatetime.jvm

lazy val ssUtilsDatetimeJS = ssUtilsDatetime.js

lazy val ssUtilsDatetimeCodecsCirce = crossProject
  .crossType(CrossType.Pure)
  .in(file("./codecs-circe"))
  .dependsOn(ssUtilsDatetime)
  .settings(CommonSettings: _*)
  .settings(
    name := "utils-datetime-circe",
    libraryDependencies ++= Dependencies.Circe.value
  )

lazy val ssUtilsDatetimeCodecsCirceJVM = ssUtilsDatetimeCodecsCirce.jvm

lazy val ssUtilsDatetimeCodecsCirceJS = ssUtilsDatetimeCodecsCirce.js

lazy val ssUtilsDatetimeCodecsPlay = project
  .in(file("./codecs-playjson"))
  .dependsOn(ssUtilsDatetime.jvm)
  .settings(CommonSettings: _*)
  .settings(
    name := "utils-datetime-playjson",
    libraryDependencies ++= Dependencies.PlayJson.value
  )

lazy val examples = project
  .in(file("./examples"))
  .dependsOn(ssUtilsDatetime.jvm)
  .settings(CommonSettings: _*)
  .settings(libraryDependencies ++= Dependencies.ScalaTest.value)
