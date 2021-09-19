import build._

val argonautMonocle = argonautCrossProject(
    "argonaut-monocle"
  , Seq(JVMPlatform, JSPlatform)
).settings(
    name := "argonaut-monocle"
  , libraryDependencies ++= Seq(
      "io.argonaut"                  %%% "argonaut-scalaz"           % "6.3.7"
    , "com.github.julien-truffaut"   %%% "monocle-core"              % monocleVersion
    , "com.github.julien-truffaut"   %%% "monocle-macro"             % monocleVersion
    , "com.github.julien-truffaut"   %%% "monocle-law"               % monocleVersion % "test"
    )
)

val argonautMonocleJVM = argonautMonocle.jvm
val argonautMonocleJS  = argonautMonocle.js

lazy val noPublish = Seq(
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {},
  publishLocal := {},
  Compile / publishArtifact := false,
  publish := {}
)

base
ReleasePlugin.projectSettings
mimaFailOnNoPrevious := false
PublishSettings.all
noPublish
name := "argonaut-parent"
run / fork := true
