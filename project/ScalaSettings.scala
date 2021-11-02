import sbt._
import Keys._

object ScalaSettings {
  type Sett = Def.Setting[_]

  private[this] val unusedWarnings = Def.setting {
    Seq("-Ywarn-unused:imports")
  }

  def Scala212 = "2.12.15"

  lazy val all: Seq[Sett] = Def.settings(
    scalaVersion := Scala212
  , crossScalaVersions := Seq(Scala212, "2.13.7")
  , test / fork := true
  , scalacOptions ++= Seq(
      unusedWarnings.value,
      Seq(
        "-Xlint"
      )
    ).flatten
  , scalacOptions ++= Seq(
      "-deprecation"
    , "-unchecked"
    , "-feature"
    , "-language:implicitConversions,higherKinds"
    )
  , scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11 | 12)) =>
          Seq("-Xfuture")
        case _ =>
          Nil
      }
    }
  ) ++ Seq(Compile, Test).flatMap(c =>
    (c / console / scalacOptions) --= unusedWarnings.value
  )
}
