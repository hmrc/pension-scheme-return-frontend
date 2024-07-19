import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.0.0"
  private val hmrcMongoVersion = "1.8.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"        %% "play-frontend-hmrc-play-30"             % "9.10.0",
    "uk.gov.hmrc"        %% "play-conditional-form-mapping-play-30"  % "2.0.0",
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc"        %% "tax-year"                               % "3.0.0",
    "uk.gov.hmrc"        %% "domain-play-30"                         % "9.0.0",
    "uk.gov.hmrc"        %% "crypto-json-play-30"                    % "7.6.0",
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-30"                     % hmrcMongoVersion,
    "org.typelevel"      %% "cats-core"                              % "2.10.0",
    "eu.timepit"         %% "refined"                                % "0.11.1",
    "org.apache.pekko"   %% "pekko-connectors-csv"                   % "1.0.2"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus"       %% "scalacheck-1-17"         % "3.2.18.0",
    "org.jsoup"               %  "jsoup"                   % "1.17.2",
    "com.softwaremill.diffx"  %% "diffx-scalatest-should"  % "0.9.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
