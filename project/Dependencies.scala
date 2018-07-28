import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._
import sbt._

object Dependencies {

  object Versions {
    val Akka: String = "2.5.12"
    val AkkaHTTP: String = "10.1.1"
    val AkkaAWSECS = "0.0.8"
    val AkkaJS: String = "0.2.4.10"
    val AkkaPersistenceRedis = "0.8.0"
    val AkkaPersistenceInMemory = "2.5.1.1"
    val AkkaSSE: String = "1.8.0"
    val AkkaStreamExtensions = "0.10.0"
    val Alluxio = "1.0.1"
    val Avro4s = "1.8.0"
    val AWS = "1.11.329"
    //    val angularVersion = "1.4.4"
    val AsyncHttpClient = "2.0.19"
    val BetterFiles = "2.17.1"
    val BooPickle = "1.1.0"
    val Bootstrap4 = "4.0.0-alpha.3"
    val BouncyCastle = "1.46"
    val Cats = "1.1.0"
    val CatsScalatest = "2.3.1"
    val Chill = "0.9.2"
    val Circe = "0.9.1"
    val CloudWatchMetrics = "0.4.0"
    val CommonsHttp = "4.5.2"
    val CommonsIo = "2.4"
    val Diode = "0.5.2"
    val Elastic4s = "6.2.3"
    val ElasticSearchSpark20 = "6.2.1"
    val Enums = "3.1"
    val Flink = "1.1.4"
    val FlyWay = "3.0.0"
    //    val jacksonVersion: String = "2.4.4"
    //    val kamonVersion: String = "0.4.0"
    val Guice = "4.1.0"
    val Hadoop = "2.7.3"
    val JBCrypt = "0.3m"
    val JodaConvert = "1.8"
    val JodaTime = "2.9.2"
    val Logback = "1.7.1"
    val LogbackLogstashEncoder = "4.11"
    val MacWire = "2.2.0"
    val Mockito = "1.9.5"
    val Netty3 = "3.10.6.Final"
    val Netty40 = "4.0.42.Final"
    val Netty41 = "4.1.5.Final"
    val Octopus = "0.3.3"
    val Play = "2.6.11"
    val PlayJson = "2.6.9"
    val PlayMockWs = "2.6.0"
    val PlayCirce = "2609.1"
    val PlayRedis = "1.5.1"
    val PlayMetrics = "2.6.2_0.5.1.0.ss.0"
    val PPrint = "0.5.2"
    val PureConfig = "0.8.0"
    val RosHTTP = "2.0.2"
    val Parquet = "2.1.0"
    val PostgresDriver = "42.1.4"
    val React = "15.5.4"
    val RedisScala = "1.8.0"
    val ScalaCsv = "1.3.3"
    val ScalaGLM = "0.3"
    val ScalaIO = "0.4.3"
    val ScalaJsAngular = "0.8.0.ss.1"
    val ScalaJsCharts = "0.4"
    val ScalaJsDom = "0.9.3"
    val ScalaJsJquery = "0.8.0"
    val ScalaJsJQueryFacade = "1.0-RC6"
    val ScalaJsMoment = "0.9.0"
    val ScalaJsReact = "1.1.1"
    val ScalaJsReactComponents = "0.4.1"
    val ScalaJsScripts = "1.0.0"
    val ScalaCache = "0.9.4"
    val ScalaCheck = "1.13.1"
    val ScalaCss = "0.5.3"
    val Scalactic = "3.0.0"
    val ScalaJsReactBridge = "0.4.0-SNAPSHOT"
    val ScalaTest = "3.0.4"
    val ScalaTestPlusPlay = "3.1.1"
    val SCodec = "1.9.0"
    val Scopt = "3.5.0"
    val Shapeless = "2.3.2"
    val SigarLoader = "1.6.6-rev002"
    val Slf4j = "1.7.21"
    val Slick = "3.2.1"
    val SlickPg = "0.15.2"
    val Slogging = "0.5.3"
    val SodaTime = "0.0.1.ss.0"
    val Spark = "2.3.0"
    val SparkLink = "1.0.4"
    val SparkkaStreams = "1.5"
    val SpringSecurity = "4.2.3.RELEASE"
    val SpringSecurityAuth0 = "1.0.0-rc.2"
    val Squants = "0.6.2"
    val ThreetenBp = "1.3.3-SNAPSHOT"
    val ScalaLogging = "3.7.2"
    val ScalaLoggingSlf4j = "2.1.2"
    val Univocity = "2.2.1"
    val XDotAiDiff = "1.3.0-0.ss.1"
    //    val sparkVersion: String = "1.4.1"

  }

  val Akka = Def.setting(
    Seq(
      "com.typesafe.akka" %% "akka-actor" % Versions.Akka,
      "com.typesafe.akka" %% "akka-cluster" % Versions.Akka,
      "com.typesafe.akka" %% "akka-cluster-sharding" % Versions.Akka,
      "com.typesafe.akka" %% "akka-cluster-metrics" % Versions.Akka,
      "com.typesafe.akka" %% "akka-http" % Versions.AkkaHTTP,
      //      "com.typesafe.akka" %% "akka-http-experimental" % Versions.AkkaHTTP,
      //      "com.typesafe.akka" %% "akka-parsing" % Versions.Akka,
      "com.typesafe.akka" %% "akka-persistence" % Versions.Akka,
      "com.typesafe.akka" %% "akka-stream" % Versions.Akka,
      "com.typesafe.akka" %% "akka-slf4j" % Versions.Akka,
      "io.github.easel" %% "utils-akka" % "0.0.2"
      //    "de.heikoseeberger" %% "akka-sse" % Versions.AkkaSSE
      //"com.mfglabs" %% "akka-stream-extensions-elasticsearch" % "0.7.3" intransitive(),
      //"com.mfglabs" %% "akka-stream-extensions-postgres" % "0.7.3" intransitive(),
      //"com.mfglabs" %% "commons-aws" % "0.7.2",
      //"org.querki" %% "requester" % "2.1"
    )
  )

  val AkkaClusterManagementHttp = "com.lightbend.akka" %% "akka-management-cluster-http" % "0.13.0"

  val AlpakkaS3 = "com.lightbend.akka" %% "akka-stream-alpakka-s3" % "0.13"

  val AkkaJS = Def.setting(
    Seq(
      "eu.unicredit" %%% "akkajsactor" % "0.2.4.10",
      "eu.unicredit" %%% "akkajsactorstream" % "0.2.4.10"
    )
  )

  val AkkaPersistenceRedis = Def.setting(
    Seq(
      "com.hootsuite" %% "akka-persistence-redis" % Versions.AkkaPersistenceRedis
    )
  )

  val AkkaStreamExtensions = Def.setting(
    Seq(
      "com.mfglabs" %% "akka-stream-extensions" % Versions.AkkaStreamExtensions
    )
  )

  val AkkaTestKit = Def.setting(
    Seq(
      "com.typesafe.akka" %% "akka-testkit" % Versions.Akka,
      "com.typesafe.akka" %% "akka-multi-node-testkit" % Versions.Akka,
      "com.github.dnvriend" %% "akka-persistence-inmemory" % Versions.AkkaPersistenceInMemory
    )
  )

  val Alluxio =
    Def.setting(Seq("org.alluxio" % "alluxio-core-client" % Versions.Alluxio))

  val Ammonite = Def.setting(
    Seq("com.lihaoyi" % "ammonite" % "1.0.3" % "test" cross CrossVersion.full)
  )

  val Auth0JavaJwt =
    Seq("com.auth0" % "java-jwt" % "3.2.0", "com.auth0" % "jwks-rsa" % "0.2.0")

  val Avro4s =
    Def.setting(Seq("com.sksamuel.avro4s" %% "avro4s-core" % Versions.Avro4s))

  val AWS = Def.setting(Seq("com.amazonaws" % "aws-java-sdk" % Versions.AWS))

  val AsyncHttpClient = Def.setting(
    Seq("org.asynchttpclient" % "async-http-client" % Versions.AsyncHttpClient)
  )

  val BetterFiles = Def.setting(
    Seq(
      "com.github.pathikrit" %% "better-files" % Versions.BetterFiles
      //"com.github.pathikrit" %% "better-files-akka" % Versions.BetterFiles,
    )
  )

  val BooPickle =
    Def.setting(Seq("me.chrons" %%% "boopickle" % Versions.BooPickle))

  val Bootstrap4 =
    Def.setting(Seq("org.webjars.npm" % "bootstrap" % Versions.Bootstrap4))

  val BouncyCastle = Def.setting(
    Seq("org.bouncycastle" % "bcprov-jdk16" % Versions.BouncyCastle)
  )

  val Cats = Def.setting(Seq("org.typelevel" %%% "cats-core" % Versions.Cats))

  val CatsScalatestCompile = Def.setting(
    Seq("com.ironcorelabs" %% "cats-scalatest" % Versions.CatsScalatest)
  )

  val CatsScalatest = Def.setting(
    Seq(
      "com.ironcorelabs" %% "cats-scalatest" % Versions.CatsScalatest % "test"
    )
  )

  val Chill = Def.setting(Seq("com.twitter" %% "chill-akka" % Versions.Chill))
  val Circe = Def.setting(
    Seq(
      "io.circe" %%% "circe-core" % Versions.Circe,
      "io.circe" %%% "circe-generic" % Versions.Circe,
      "io.circe" %%% "circe-generic-extras" % Versions.Circe,
      "io.circe" %%% "circe-parser" % Versions.Circe
    )
  )

  val CloudWatchMetrics = Def.setting(
    Seq("com.blacklocus" % "metrics-cloudwatch" % Versions.CloudWatchMetrics)
  )

  val CommonsHttp = Def.setting(
    Seq(
      "org.apache.httpcomponents" % "httpcore" % Versions.CommonsHttp,
      "org.apache.httpcomponents" % "httpclient" % Versions.CommonsHttp
    )
  )

  val CommonsIo =
    Def.setting(Seq("commons-io" % "commons-io" % Versions.CommonsIo))

  val Elastic4s = Def.setting(
    Seq(
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Dependencies.Versions.Elastic4s,
      "com.sksamuel.elastic4s" %% "elastic4s-http-streams" % Dependencies.Versions.Elastic4s
      //      "org.elasticsearch"      % "elasticsearch"      % Dependencies.Versions.ElasticSearch,
      //      "com.vividsolutions"     % "jts"                % "1.13"
    )
  )

  val Deadbolt2 = Seq("be.objectify" %% "deadbolt-scala" % "2.6.0")

  val Diode = Def.setting(
    Seq(
      "me.chrons" %%% "diode" % Versions.Diode,
      "me.chrons" %%% "diode-react" % Versions.Diode
    )
  )

  val ElasticSearchSpark20 = Def.setting(
    Seq(
      "org.elasticsearch" %% "elasticsearch-spark-20" % Versions.ElasticSearchSpark20
    )
  )

  val Enums = Def.setting(Seq("org.julienrf" %%% "enum" % Versions.Enums))

  val Flink = Def.setting(
    Seq(
      "org.apache.flink" %% "flink-streaming-scala" % Versions.Flink % "provided",
      "org.apache.flink" %% "flink-scala" % Versions.Flink % "provided",
      "org.apache.flink" %% "flink-clients" % Versions.Flink % "provided"
    )
  )

  val FlyWay =
    Def.setting(Seq("org.flywaydb" %% "flyway-play" % Versions.FlyWay))

  val Guice = Def.setting(
    Seq(
      "net.codingwell" %% "scala-guice" % Versions.Guice,
      "com.google.inject" % "guice" % Versions.Guice,
      "com.google.inject.extensions" % "guice-assistedinject" % Versions.Guice
    )
  )

  val JBCrypt = Def.setting(Seq("org.mindrot" % "jbcrypt" % Versions.JBCrypt))

  val JodaTime = Def.setting(
    Seq(
      "joda-time" % "joda-time" % Versions.JodaTime,
      "org.joda" % "joda-convert" % Versions.JodaConvert
    )
  )

  val Logback = Def.setting(
    Seq(
      "ch.qos.logback" % "logback-access" % "1.2.3",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "ch.qos.logback" % "logback-core" % "1.2.3"
    )
  )

  val LogbackLogstashEncoder = Def.setting(
    Seq(
      "net.logstash.logback" % "logstash-logback-encoder" % Versions.LogbackLogstashEncoder
    )
  )

  val PlayCirce =
    Def.setting(Seq("com.dripower" %% "play-circe" % Versions.PlayCirce))

  val PlayMetrics =
    Def.setting(Seq("com.kenshoo" %% "metrics-play" % Versions.PlayMetrics))

  val PlayMockWs = Def.setting(
    Seq(
      "de.leanovate.play-mockws" %% "play-mockws" % Versions.PlayMockWs % "test"
    )
  )

  val PlayRedis = Def.setting(
    Seq(
      //"com.typesafe.play.modules" %% "play-modules-redis" % Versions.PlayRedis
      "com.github.karelcemus" %% "play-redis" % Versions.PlayRedis
    )
  )

  val PPrint = Def.setting(Seq("com.lihaoyi" %%% "pprint" % Versions.PPrint))

  val PureConfig = Def.setting(
    Seq(
      "com.github.pureconfig" %% "pureconfig" % Versions.PureConfig,
      "com.github.pureconfig" %% "pureconfig-enum" % Versions.PureConfig,
      "com.github.pureconfig" %% "pureconfig-squants" % Versions.PureConfig
    )
  )

  val RosHTTP = Def.setting(Seq("fr.hmil" %%% "roshttp" % Versions.RosHTTP))

  val ScalaCsv =
    Def.setting(Seq("com.github.tototoshi" %% "scala-csv" % Versions.ScalaCsv))

  val ScalaOAuth2Provider = Def.setting(
    Seq(
      "com.nulab-inc" %% "scala-oauth2-core" % "1.3.0",
      "com.nulab-inc" %% "play2-oauth2-provider" % "1.3.0"
    )
  )

  val Scopt = Def.setting(Seq("com.github.scopt" %% "scopt" % Versions.Scopt))

  val Spark = Def.setting(
    Seq(
      "org.apache.spark" %% "spark-mllib" % Versions.Spark % "provided",
      "org.apache.hadoop" % "hadoop-aws" % Versions.Hadoop % "provided"
    )
  )

  val SparkLint =
    Def.setting(Seq("com.groupon.sparklint" %% "sparklint-spark202" % "1.0.4"))

  val SparkkaStreams = Def.setting(
    Seq("com.beachape" %% "sparkka-streams" % Versions.SparkkaStreams)
  )

  val Squants = Def.setting(Seq("com.squants" %% "squants" % Versions.Squants))

  val MapDB = Def.setting(Seq("org.mapdb" % "mapdb" % "1.0.9"))

  val Mockito = Def.setting(
    Seq(
      "org.mockito" % "mockito-core" % Versions.Mockito,
      "org.mockito" % "mockito-all" % Versions.Mockito
    )
  )

  val Netty3 = Def.setting(Seq("io.netty" % "netty" % Versions.Netty3))

  val Netty40 = Def.setting(
    Seq(
      "io.netty" % "netty-buffer" % Versions.Netty40,
      "io.netty" % "netty-codec" % Versions.Netty40,
      "io.netty" % "netty-codec-http" % Versions.Netty40,
      "io.netty" % "netty-common" % Versions.Netty40,
      "io.netty" % "netty-handler" % Versions.Netty40,
      "io.netty" % "netty-transport" % Versions.Netty40,
      "io.netty" % "netty-all" % Versions.Netty40
    )
  )

  val Netty41 = Def.setting(
    Seq(
      "io.netty" % "netty-buffer" % Versions.Netty41,
      "io.netty" % "netty-codec" % Versions.Netty41,
      "io.netty" % "netty-codec-http" % Versions.Netty41,
      "io.netty" % "netty-common" % Versions.Netty41,
      "io.netty" % "netty-handler" % Versions.Netty41,
      "io.netty" % "netty-transport" % Versions.Netty41,
      "io.netty" % "netty-all" % Versions.Netty41
    )
  )

  val Octopus =
    Def.setting(Seq(
      "com.github.krzemin" %%% "octopus-cats" % Versions.Octopus,
      "com.github.krzemin" %%% "octopus" % Versions.Octopus
    ))

  val Parquet = Def.setting(Seq("com.twitter" % "parquet" % Versions.Parquet))

  val PlayCache =
    Def.setting(Seq("com.typesafe.play" %% "play-cache" % Versions.Play))

  val PlayDb =
    Def.setting(Seq("com.typesafe.play" %% "play-db" % Versions.Play))

  val PlaySlick =
    Def.setting(Seq("com.typesafe.play" %% "play-slick" % "3.0.1"))

  val PlayJson =
    Def.setting(Seq("com.typesafe.play" %% "play-json" % Versions.PlayJson))

  val PlayWs =
    Def.setting(Seq("com.typesafe.play" %% "play-ws" % Versions.Play))

  val PostgresDriver =
    Def.setting(Seq("org.postgresql" % "postgresql" % Versions.PostgresDriver))

  val RedisScala =
    Def.setting(Seq("com.github.etaty" %% "rediscala" % Versions.RedisScala))

  val ScalaGLM =
    Def.setting(Seq("com.github.darrenjw" %% "scala-glm" % Versions.ScalaGLM))

  val ScalaIO = Def.setting(
    Seq(
      "com.github.scala-incubator.io" %% "scala-io-core" % Versions.ScalaIO,
      "com.github.scala-incubator.io" %% "scala-io-file" % Versions.ScalaIO
    )
  )

  val ScalaJsAngular = Def.setting(
    Seq("com.greencatsoft" %%% "scalajs-angular" % Versions.ScalaJsAngular)
  )

  val ScalaJsCharts = Def.setting(
    Seq("com.github.easel" %%% "scalajs-charts" % Versions.ScalaJsCharts)
  )

  val ScalaJsDom =
    Def.setting(Seq("org.scala-js" %%% "scalajs-dom" % Versions.ScalaJsDom))

//  val ScalaJavaLocales =
//    Def.setting(Seq("io.github.cquiroz" %%% "scala-java-locales" % "2.0.0-M12"))

  val ScalaJavaTime =
    Def.setting(Seq("io.github.cquiroz" %%% "scala-java-time" % "2.0.0-M13"))

  val ScalaJsJquery = Def.setting(
    Seq(
      "be.doeraene" %%% "scalajs-jquery" % Versions.ScalaJsJquery intransitive ()
    )
  )

  val ScalaJsJQueryFacade = Def.setting(
    Seq("org.querki" %%% "jquery-facade" % Versions.ScalaJsJQueryFacade)
  )

  val ScalaJsReact = Def.setting(
    Seq(
      "com.github.japgolly.scalajs-react" %%% "core" % Versions.ScalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "extra" % Versions.ScalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "test" % Versions.ScalaJsReact,
      "com.github.japgolly.scalajs-react" %%% "ext-cats" % Versions.ScalaJsReact
    )
  )

  val ScalaJsScripts = Def.setting(
    Seq("com.vmunier" %% "scalajs-scripts" % Versions.ScalaJsScripts)
  )

  val ScalaJsReactComponents = Def.setting(
    Seq(
      "com.github.chandu0101.scalajs-react-components" %%% "core" % Versions.ScalaJsReactComponents
    )
  )

  val ScalaCache = Def.setting(
    Seq(
      "com.github.cb372" %% "scalacache-redis" % Versions.ScalaCache,
      "com.github.cb372" %% "scalacache-caffeine" % Versions.ScalaCache
    )
  )

  val ScalaCss = Def.setting(
    Seq(
      "com.github.japgolly.scalacss" %%% "core" % Versions.ScalaCss,
      "com.github.japgolly.scalacss" %%% "ext-react" % Versions.ScalaCss,
      "com.github.japgolly.scalacss" %%% "ext-scalatags" % Versions.ScalaCss
    )
  )

  val ScalaJsMoment = Def.setting(
    Seq("ru.pavkin" %%% "scala-js-momentjs" % Versions.ScalaJsMoment)
  )

  val ScalaCheck = Def.setting(
    Seq("org.scalacheck" %% "scalacheck" % Versions.ScalaCheck % "test")
  )

  val Scalactic =
    Def.setting(Seq("org.scalactic" %%% "scalactic" % Versions.Scalactic))

  val ScalaJsReactBridge = Def.setting(
    Seq("com.payalabs" %%% "scalajs-react-bridge" % Versions.ScalaJsReactBridge)
  )

  val ScalaTestCompile =
    Def.setting(Seq("org.scalatest" %%% "scalatest" % Versions.ScalaTest))

  val ScalaTest = Def.setting(
    Seq(
      "org.scalactic" %%% "scalactic" % Versions.Scalactic,
      "org.scalatest" %%% "scalatest" % Versions.ScalaTest % "test"
    )
  )

  val ScalaTestPlusPlay = Def.setting(
    Seq(
      "org.scalatestplus.play" %% "scalatestplus-play" % Versions.ScalaTestPlusPlay
    )
  )

  val SCodec =
    Def.setting(Seq("org.scodec" %%% "scodec-core" % Versions.SCodec))

  val Shapeless: Seq[ModuleID] = Seq(
    "com.chuusai" %% "shapeless" % Versions.Shapeless
  )

  val SigarLoader =
    Def.setting(Seq("io.kamon" % "sigar-loader" % Versions.SigarLoader))

  val Slf4j = Def.setting(
    Seq(
      "org.slf4j" % "slf4j-api" % Versions.Slf4j,
      "org.slf4j" % "jcl-over-slf4j" % Versions.Slf4j,
      "org.slf4j" % "jul-to-slf4j" % Versions.Slf4j,
      "org.slf4j" % "log4j-over-slf4j" % Versions.Slf4j
    )
  )
  val Slick = Def.setting(Seq("com.typesafe.slick" %% "slick" % Versions.Slick))

  val SlickPg = Def.setting(
    Seq(
      "com.github.tminglei" %% "slick-pg" % Dependencies.Versions.SlickPg,
      "com.github.tminglei" %% "slick-pg_circe-json" % Dependencies.Versions.SlickPg
    )
  )

  val Slogging = Def.setting(
    Seq(
      "biz.enef" %%% "slogging" % Versions.Slogging,
      "biz.enef" %% "slogging-slf4j" % Versions.Slogging exclude ("org.slf4j", "log4j-over-slf4j")
    )
  )
  val ThreetenBp =
    Def.setting(Seq("org.threeten" %%% "threetenbp" % Versions.ThreetenBp))

  val ScalaLogging = Def.setting(
    Seq("com.typesafe.scala-logging" %% "scala-logging" % Versions.ScalaLogging)
  )

  val ScalaLoggingSlf4j = Def.setting(
    Seq(
      "com.typesafe.scala-logging" %% "scala-logging-slf4j" % Versions.ScalaLoggingSlf4j
    )
  )

  val ScalaURI = Def.setting(Seq("io.lemonlabs" %%% "scala-uri" % "1.1.1"))

  val Univocity =
    Def.setting(Seq("com.univocity" % "univocity-parsers" % Versions.Univocity))

  val XDotAiDiff =
    Def.setting(Seq("ai.x" %% "diff" % Versions.XDotAiDiff % "test"))
}
