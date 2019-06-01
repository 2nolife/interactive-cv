name := "interactive-cv"

Common.settings

libraryDependencies ++= Dependencies.icv

mainClass in (Compile, run) := Some("com.coldcore.icv.Runner")

lazy val root = (project in file(".")).
  configs(IntegrationTest).
  settings(Defaults.itSettings: _*)

parallelExecution in Test := false