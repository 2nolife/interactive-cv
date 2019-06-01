import sbt._

object Version {
  val scala = "2.12.8"
}

object Library {
  val scalaTest       = "org.scalatest"        %% "scalatest"       % "3.0.5"
  val logbackClassic  = "ch.qos.logback"       %  "logback-classic" % "1.2.3"
  val typesafeConfig  = "com.typesafe"         %  "config"          % "1.3.4"
  val jenaCode        = "org.apache.jena"      %  "jena-core"       % "3.10.0"
  val jenaTdb         = "org.apache.jena"      %  "jena-tdb"        % "3.10.0"
  val jenaArq         = "org.apache.jena"      %  "jena-arq"        % "3.10.0"
  val commonsLang     = "org.apache.commons"   %  "commons-lang3"   % "3.9"
  val commonsIo       = "commons-io"           %  "commons-io"      % "2.6"
  val mapdb           = "org.mapdb"            %  "mapdb"           % "3.0.7"
  val neo4j           = "org.neo4j"            %  "neo4j"           % "3.5.5"
  val freemarker      = "org.freemarker"       %  "freemarker"      % "2.3.28"
  val gson            = "com.google.code.gson" %  "gson"            % "2.8.5"
  val zip4j           = "net.lingala.zip4j"    %  "zip4j"           % "1.3.2"
}

object Dependencies {

  import Library._

  val icv = List(
    logbackClassic,
    typesafeConfig,
    jenaCode,
    jenaTdb,
    jenaArq,
    commonsLang,
    commonsIo,
    mapdb,
    neo4j,
    freemarker,
    gson,
    zip4j,
    scalaTest % "test"
  )
}
