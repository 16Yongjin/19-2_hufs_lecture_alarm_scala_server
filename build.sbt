lazy val CatsEffectVersion   = "2.0.0-RC2"
lazy val Fs2Version          = "1.0.3"
lazy val Http4sVersion       = "0.20.0-RC1"
lazy val CirceVersion        = "0.11.1"
lazy val DoobieVersion       = "0.6.0"
lazy val H2Version           = "1.4.196"
lazy val FlywayVersion       = "5.0.5"
lazy val LogbackVersion      = "1.2.3"
lazy val ScalaTestVersion    = "3.0.7"
lazy val ScalaCheckVersion   = "1.13.4"
lazy val ScalaScraperVersion = "2.1.0"

lazy val root = (project in file("."))
  .settings(
    organization := "org.yongj.in",
    name := "hufs_lecture_alarm",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.8",
    scalacOptions := Seq(
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-Ypartial-unification"
    ),
    libraryDependencies ++= Seq(
      "org.typelevel"     %% "cats-core"           % "2.0.0-RC1",
      "org.typelevel"     %% "cats-effect"         % CatsEffectVersion,
      "co.fs2"            %% "fs2-core"            % Fs2Version,
      "org.http4s"        %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"        %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s"        %% "http4s-circe"        % Http4sVersion,
      "org.http4s"        %% "http4s-dsl"          % Http4sVersion,
      "io.circe"          %% "circe-core"          % CirceVersion,
      "io.circe"          %% "circe-generic"       % CirceVersion,
      "com.h2database"    % "h2"                   % H2Version,
      "org.flywaydb"      % "flyway-core"          % FlywayVersion,
      "org.tpolecat"      %% "doobie-core"         % DoobieVersion,
      "org.tpolecat"      %% "doobie-postgres"     % DoobieVersion,
      "org.tpolecat"      %% "doobie-h2"           % DoobieVersion,
      "ch.qos.logback"    % "logback-classic"      % LogbackVersion,
      "org.scalatest"     %% "scalatest"           % ScalaTestVersion % Test,
      "org.scalacheck"    %% "scalacheck"          % ScalaCheckVersion % Test,
      "org.tpolecat"      %% "doobie-scalatest"    % DoobieVersion % Test,
      "net.ruippeixotog"  %% "scala-scraper"       % ScalaScraperVersion,
      "io.chrisdavenport" %% "cats-par"            % "1.0.0-RC2",
      "dev.zio" %% "zio"                 % "1.0.0-RC11-1"

    )
  )

enablePlugins(DockerPlugin)

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File     = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:8-jre")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

// sbt docker & docker tag org.yongj.in/hufs_lecture_alarm yongijn0802/hufs-lecture-alarm & docker push yongijn0802/hufs-lecture-alarm
