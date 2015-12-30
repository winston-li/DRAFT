lazy val common = (project in file("./Common")).
  settings(Common.settings:_*).
  settings(
    libraryDependencies ++= Dependencies.common,
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources"
  )

lazy val kafkadatafeeder = (project in file("./DataFeeder")).
  dependsOn(common % "test->test; compile->compile").
  settings(Common.settings:_*).
  settings(
    libraryDependencies ++= Dependencies.datafeeder,
    assemblyJarName in assembly := "KafkaDataFeederAssembly-" + version.value + ".jar",
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources"
  )

lazy val streamanalyzer = (project in file("./StreamAnalyzer")).
  dependsOn(common % "test->test; compile->compile").
  settings(Common.settings:_*).
  settings(
    libraryDependencies ++= Dependencies.streamanalyzer,
    assemblyJarName in assembly := "SparkStreamingAnalyzerAssembly-" + version.value + ".jar"
  )

lazy val dataaggregator = (project in file("./DataAggregator")).
  dependsOn(common % "test->test; compile->compile").
  settings(Common.settings:_*).
  settings(
    libraryDependencies ++= Dependencies.dataaggregrator,
    assemblyJarName in assembly := "CassandraDataAggregatorAssembly-" + version.value + ".jar"
  )
