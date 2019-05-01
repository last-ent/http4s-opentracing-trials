import Dependencies._

lazy val root = (project in file("."))
  .settings(
    inThisBuild(
      List(
        organization := "com.example",
        scalaVersion := "2.12.8",
        version := "0.0.1"
      )
    ),
    name := "Http4s OpenTracing",
    scalacOptions ++= Seq(
        "-encoding",
        "UTF-8", // source files are in UTF-8
        "-deprecation", // warn about use of deprecated APIs
        "-unchecked", // warn about unchecked type parameters
        "-feature", // warn about misused language features
        "-language:higherKinds", // allow higher kinded types without `import scala.language.higherKinds`
        "-Xlint", // enable handy linter warnings
        "-Xfatal-warnings", // turn compiler warnings into errors
        "-Ypartial-unification" // allow the compiler to unify type constructors of different arities
      ),
    libraryDependencies ++= appDeps ++ testDeps,
    scalafmtOnCompile := true,
    logLevel := Level.Info
  )
