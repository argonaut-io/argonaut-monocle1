import sbt._
import Keys._
import com.jsuereth.sbtpgp.PgpKeys._
import sbtrelease.ReleasePlugin.autoImport._

object PublishSettings {
  type Sett = Def.Setting[?]

  lazy val all = Seq[Sett](
    pom
  , publish
  , publishMavenStyle := true
  , Test / publishArtifact := false
  , pomIncludeRepository := { _ => false }
  , releasePublishArtifactsAction := publishSigned.value
  , releaseProcess := {
     import sbtrelease.ReleasePlugin.autoImport.ReleaseTransformations._
     Seq[ReleaseStep](
       checkSnapshotDependencies,
       inquireVersions,
       runTest,
       setReleaseVersion,
       commitReleaseVersion,
       tagRelease,
       publishArtifacts,
       releaseStepCommandAndRemaining("sonaRelease"),
       setNextVersion,
       commitNextVersion,
       pushChanges
    )}
  , releaseCrossBuild := true
  , licenses := Seq(License("BSD-3-Clause", uri("http://www.opensource.org/licenses/BSD-3-Clause")))
  , homepage := Some(uri("https://github.com/argonaut-io/argonaut-monocle1"))
  , autoAPIMappings := true
  , useGpg := true
  )

  lazy val pom: Sett =
    pomExtra := (
      <scm>
        <url>git@github.com:argonaut-io/argonaut-monocle1.git</url>
        <connection>scm:git:git@github.com:argonaut-io/argonaut-monocle1.git</connection>
      </scm>
      <developers>
        <developer>
          <id>tonymorris</id>
          <name>Tony Morris</name>
          <url>http://tmorris.net</url>
        </developer>
        <developer>
          <id>mth</id>
          <name>Mark Hibberd</name>
          <url>http://mth.io</url>
        </developer>
        <developer>
          <id>seanparsons</id>
          <name>Sean Parsons</name>
        </developer>
      </developers>
    )

  lazy val publish: Sett =
    publishTo := (if (isSnapshot.value) None else localStaging.value)
}
