import ScriptedPlugin._

name := "android-sdk-plugin"

version := "1.4.0-SNAPSHOT"

organization := "com.hanhuy.sbt"

sourceDirectories in Compile <<= baseDirectory(b => Seq(b / "src"))

scalacOptions ++= Seq("-deprecation","-Xlint")

scalaSource in Compile <<= baseDirectory(_ / "src")

scalaSource in Test <<= baseDirectory(_ / "test")

unmanagedBase <<= baseDirectory(_ / "libs")

resourceDirectory in Compile <<= baseDirectory(_ / "resources")

libraryDependencies ++= Seq(
  "org.ow2.asm" % "asm-all" % "5.0.2",
  "javassist" % "javassist" % "3.12.1.GA",
  "net.sf.proguard" % "proguard-base" % "5.0",
  "com.android.tools.build" % "builder" % "1.0.0"
)

sbtPlugin := true

// build info plugin

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

buildInfoKeys := Seq(name, version, scalaVersion, sbtVersion)

buildInfoPackage := "android"

publishTo <<= version { version =>
  val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
  val (name, url) = if (version contains "-SNAPSHOT")
    ("scala-sbt-snapshots", scalasbt + "sbt-plugin-snapshots")
  else
    ("scala-sbt-releases", scalasbt + "sbt-plugin-releases")
  Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

publishMavenStyle := false

// scripted-test settings
scriptedSettings

scriptedLaunchOpts ++= Seq("-Xmx1024m", "-XX:PermSize=512m")

//scriptedBufferLog := false

sbtTestDirectory <<= baseDirectory (_ / "sbt-test")

// TODO reorganize tests better, ditch android-sdk-plugin prefix
// group by test config type
scriptedDependencies <<= ( sbtTestDirectory
                         , streams
                         , organization
                         , name
                         , version
                         , sbtVersion) map {
  (dir,s, org, n, v, sbtv) =>
  val testBase = dir / "android-sdk-plugin"
  val tests = testBase.listFiles(DirectoryFilter) filter { d =>
    (d ** "*.sbt").get.size > 0 || (d / "project").isDirectory
  }
  tests foreach { test =>
    val project = test / "project"
    project.mkdirs()
    val pluginsFile = project / "auto_plugins.sbt"
    val propertiesFile = project / "build.properties"
    pluginsFile.delete()
    propertiesFile.delete()
    IO.write(pluginsFile,
      """addSbtPlugin("%s" %% "%s" %% "%s")""" format (org, n, v))
    IO.write(propertiesFile, """sbt.version=%s""" format sbtv)
  }
}

scriptedDependencies <<= scriptedDependencies dependsOn publishLocal
