import sbt._

object AppDependencies {
  import play.core.PlayVersion

  private val bootstrapVersion = "8.3.0"
  private val hmrcMongoVersion = "1.7.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"        %% "play-frontend-hmrc-play-29"             % "8.3.0",
    "uk.gov.hmrc"        %% "play-conditional-form-mapping-play-29"  % "2.0.0",
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-29"             % bootstrapVersion,
    "uk.gov.hmrc"        %% "tax-year"                               % "3.0.0",
    "uk.gov.hmrc"        %% "domain-play-29"                         % "9.0.0",
    "uk.gov.hmrc"        %% "crypto-json-play-29"                    % "7.6.0",
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-29"                     % hmrcMongoVersion,
    "org.typelevel"      %% "cats-core"                              % "2.10.0",
    "eu.timepit"         %% "refined"                                % "0.11.0",
    "com.lightbend.akka" %% "akka-stream-alpakka-csv"                % "4.0.0"
  )

  val test: Seq[ModuleID] = Seq(
    "com.typesafe.play"       %% "play-test"               % PlayVersion.current,
    "uk.gov.hmrc"             %% "bootstrap-test-play-29"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-29" % hmrcMongoVersion,
    "org.scalatest"           %% "scalatest"               % "3.2.15",
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.15.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "5.1.0",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.15.4",
    "org.mockito"             %% "mockito-scala-scalatest" % "1.17.30",
    "org.mockito"             %% "mockito-scala"           % "1.17.30",
    "org.scalacheck"          %% "scalacheck"              % "1.17.0",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.64.6",
    "com.softwaremill.diffx"  %% "diffx-scalatest-should"  % "0.9.0",
  ).map(_ % "test, it")

  def apply(): Seq[ModuleID] = compile ++ test
}
