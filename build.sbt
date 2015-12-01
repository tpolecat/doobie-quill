name := "doobie-quill"

organization := "org.tpolecat"

version := "0.0.1-SNAPSHOT"

scalaVersion in ThisBuild := "2.11.7"

scalacOptions in ThisBuild ++= Seq(
  "-encoding", "UTF-8", // 2 args
  "-feature",                
  "-language:existentials",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:experimental.macros",
  "-unchecked",
  "-Xlint",
  "-Yno-adapted-args",       
  "-Ywarn-dead-code",       
  "-Ywarn-value-discard"     
)

libraryDependencies ++= Seq(
  "org.tpolecat" %% "doobie-core"                % "0.2.3",
  "org.tpolecat" %% "doobie-contrib-postgresql"  % "0.2.3",
  "io.getquill"  %% "quill-jdbc"                 % "0.1.0"
)
