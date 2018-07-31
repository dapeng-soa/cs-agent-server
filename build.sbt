import java.io.{FileInputStream, FileOutputStream}

name := "agent_server"

resolvers += Resolver.mavenLocal

lazy val commonSettings = Seq(
  organization := "com.github.dapeng",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.12.2"
)

javacOptions ++= Seq("-encoding", "UTF-8")

lazy val api = (project in file("agent_server-api"))
  .settings(
    commonSettings,
    name := "agent_server-api",
    libraryDependencies ++= Seq(
      "com.github.wangzaixiang" %% "scala-sql" % "2.0.6",
      "com.google.code.gson" % "gson" % "2.3.1"
    )
  )

/**
  * <dependency>
  * <groupId>com.google.code.gson</groupId>
  * <artifactId>gson</artifactId>
  * <version>2.3.1</version>
  * </dependency>
  */
lazy val service = (project in file("agent_server-service"))
  .dependsOn( api )
  .settings(
    commonSettings,
    name := "agent_server_service",
    libraryDependencies ++= Seq(
      "org.yaml" % "snakeyaml" % "1.17",
      "com.corundumstudio.socketio" % "netty-socketio" % "1.7.12",
      "io.socket" % "socket.io-client" % "0.8.1",
      "com.github.wangzaixiang" %% "scala-sql" % "2.0.6",
      "com.google.code.gson" % "gson" % "2.3.1"
    ))


mainClass in assembly := Some("com.github.dapeng.socket.server.Main")
lazy val dist = taskKey[File]("make a dist scompose file")

dist := {
  val assemblyJar = assembly.value

  val distJar = new java.io.File(target.value, "agentServer")
  val out = new FileOutputStream(distJar)

  out.write(
    """#!/usr/bin/env sh
      |exec java -jar -XX:+UseG1GC "$0" "$@"
      |""".stripMargin.getBytes)

  val inStream = new FileInputStream(assemblyJar)
  val buffer = new Array[Byte](1024)

  while( inStream.available() > 0) {
    val length = inStream.read(buffer)
    out.write(buffer, 0, length)
  }

  out.close

  distJar.setExecutable(true, false)
  println("=================================")
  println(s"build agent at ${distJar.getAbsolutePath}" )

  distJar
}