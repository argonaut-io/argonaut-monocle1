import sbt._
import Keys._
import com.jsuereth.sbtpgp.PgpKeys._
import sbtrelease.ReleasePlugin
import sbtrelease.ReleasePlugin.autoImport._
import com.typesafe.tools.mima.plugin.MimaPlugin._
import com.typesafe.tools.mima.plugin.MimaKeys._
import sbtcrossproject.{CrossProject, Platform}
import sbtcrossproject.CrossPlugin.autoImport._
import scalajscrossproject.ScalaJSCrossPlugin.autoImport._
import org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._

object build {
  type Sett = Def.Setting[_]

  val base = ScalaSettings.all ++ Seq[Sett](
      organization := "io.argonaut"
  )

  val monocleVersion             = "1.7.3"

  private[this] val tagName = Def.setting {
    s"v${if (releaseUseGlobalVersion.value) (ThisBuild / version).value else version.value}"
  }

  private[this] val tagOrHash = Def.setting {
    if(isSnapshot.value) {
      sys.process.Process("git rev-parse HEAD").lineStream_!.head
    } else {
      tagName.value
    }
  }

  private[this] val previousVersions = Def.setting {
    val last = 6
    (0 to last).map(n => s"6.3.$n")
  }

  val commonSettings = base ++
    ReplSettings.all ++
    ReleasePlugin.projectSettings ++
    PublishSettings.all ++
    Def.settings(
      (Compile / doc / scalacOptions) ++= {
        val tag = tagOrHash.value
        val base = (LocalRootProject / baseDirectory).value.getAbsolutePath
        Seq("-sourcepath", base, "-doc-source-url", "https://github.com/argonaut-io/argonaut-monocle1/tree/" + tag + "€{FILE_PATH}.scala")
      }
    , releaseTagName := tagName.value
    , ThisBuild / mimaReportSignatureProblems := true
    /*
    , mimaBinaryIssueFilters ++= {
      import com.typesafe.tools.mima.core._
      import com.typesafe.tools.mima.core.ProblemFilters._
      /* adding functions to sealed traits is binary incompatible from java, but ok for scala, so ignoring */
      Seq(
      ) map exclude[MissingMethodProblem]
    }
    */
  )

  def argonautCrossProject(name: String, platforms: Seq[Platform]) = {
    CrossProject(name, file(name))(platforms: _*)
      .crossType(CrossType.Full)
      .settings(commonSettings)
      .jvmSettings(
        // https://github.com/scala/scala-parser-combinators/issues/197
        // https://github.com/sbt/sbt/issues/4609
        Test / fork := true,
        (Test / baseDirectory) := (LocalRootProject / baseDirectory).value,
        mimaPreviousArtifacts := {
          previousVersions.value.map { n =>
            organization.value %% Keys.name.value % n
          }.toSet
        },
      )
      .settings(
        libraryDependencies += "org.specs2" %%% "specs2-scalacheck" % "4.20.4" % "test",
      )
      .jsSettings(
        Test / parallelExecution := false,
        mimaPreviousArtifacts := previousVersions.value.map { n =>
          organization.value %% s"${Keys.name.value}_sjs1" % n
        }.toSet,
        scalacOptions += {
          val a = (LocalRootProject / baseDirectory).value.toURI.toString
          val g = "https://raw.githubusercontent.com/argonaut-io/argonaut-monocle1/" + tagOrHash.value
          val key = CrossVersion.partialVersion(scalaVersion.value) match {
            case Some((3, _)) =>
              "-scalajs-mapSourceURI"
            case _ =>
              "-P:scalajs:mapSourceURI"
          }
          s"${key}:$a->$g/"
        }
      )
  }
}
