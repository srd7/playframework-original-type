name := """playframework-original-type"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  filters,
  "org.scalatestplus.play" %% "scalatestplus-play"    % "1.5.1" % Test,
  "com.typesafe.play"      %% "play-slick"            % "2.0.0",
  "com.typesafe.play"      %% "play-slick-evolutions" % "2.0.0",
  "com.h2database"         %  "h2"                    % "1.4.193"
)

// オリジナルID型を conf/routes でも読めるようにする
routesImport ++= Seq(
  "models.typesafe._",
  "models.typesafe.PathBindableImplicits._"
)

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"
