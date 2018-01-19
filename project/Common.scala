import sbt.Keys._
import sbt._
import bintray.BintrayKeys._

object Common {
  val scalaV211 = "2.11.12"
  val scalaV212 = "2.12.4"
  val scalaV = scalaV211
  val scalaVersions = Seq(scalaV211, scalaV212)

  val scalatestVersion: String = Dependencies.Versions.ScalaTest
  val scalatestPlayVersion: String = Dependencies.Versions.ScalaTestPlusPlay

  val sharedScalacOptions = Seq(
    //    "-Xfatal-warnings", // need play to stop emitting unused imports to turn this on
    "-deprecation",
    //"-explaintypes",                     // Explain type errors in more detail.
    //"-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    //"-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    //"-Xlint",
    //"-Xlint:-unused",
    "-unchecked",
    "-encoding",
    "UTF-8", // yes, this is 2 args
    "-feature",
    "-Xfuture",
    "-Xsource:2.12",
    "-Yno-adapted-args",
    "-Ywarn-value-discard",
    "-Ypartial-unification"
  )

  val sharedSettings: Seq[Setting[_]] =
    List(
      addCompilerPlugin(
        "org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary
      ),
      isSnapshot := version.value.contains("-SNAPSHOT"),
      publishMavenStyle := true,
      bintrayOrganization := Some("7thsense"),
      bintrayVcsUrl := Some("git@github.com:7thsense/utils-all.git"),
      licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
      organization := "com.theseventhsense",
      javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation"),
      maxErrors := 1,
      scalacOptions := sharedScalacOptions,

      // Turn off building docs as a part of the standard distribution
      sources in (Compile, doc) := Seq.empty,
      publishArtifact in (Compile, packageDoc) := false
    )

  val settings: Seq[Setting[_]] = sharedSettings ++ List(
    crossScalaVersions := scalaVersions,
    scalaVersion := scalaV
  )

  val sparkSettings: Seq[Setting[_]] = sharedSettings ++ List(
    crossScalaVersions := Seq(scalaV211),
    scalaVersion := scalaV211
  )

  val jsSettings: Seq[Setting[_]] = sharedSettings ++ List(
    crossScalaVersions := scalaVersions,
    fork := false,
    scalaVersion := scalaV212
  )
}
