lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.dfrancoeur",
      scalaVersion := "2.12.2",
      version      := "0.1.0-SNAPSHOT",
      scalacOptions in ThisBuild ++= Seq(
        "-language:_",
        "-Ypartial-unification"
      )
    )),
    name := "fp-learning",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % "0.7.0") ++ Seq(
      "org.typelevel" %% "cats" % "0.9.0",
      "com.47deg"     %% "freestyle" % "0.1.0",
      "com.spinoco"   %% "fs2-http"  % "0.1.6"
    ),
    addCompilerPlugin(
      "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
    )
  )
