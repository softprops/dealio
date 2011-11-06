import sbt._
import sbt.Keys._

object Build extends sbt.Build {
  lazy val server = Project("server", file("server"),
    settings = Defaults.defaultSettings ++Seq(
      name := "dealio-server",
      version := "0.1.0-SNAPSHOT",
      organization := "me.lessis",
      resolvers ++= Seq(
        "coda" at "http://repo.codahale.com",
        "mvn-admangent" at "http://mvn-adamgent.googlecode.com/svn/maven/release"
      ),
      libraryDependencies ++= Seq(
        "net.databinder" %% "dispatch-http" % "0.8.5",
        "net.databinder" %% "unfiltered-netty-server" % "0.5.1",
        "com.codahale" %% "jerkson" % "0.5.0",
        "redis.clients" % "jedis" % "2.0.0",
        "com.google.zxing" % "core" % "1.6",
        "com.google.zxing" % "javase" % "1.6"
      )
    ))
}
