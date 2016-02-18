import CommonDependency.dependencies

lazy val root = (project in file("."))
  .settings(
    name := "akka-locator",
    version := "0.1-SNAPSHOT",
    scalaVersion := "2.11.7",
    libraryDependencies ++= dependencies.common,
    libraryDependencies ++= dependencies.akka,
    libraryDependencies ++= dependencies.curator
  )
