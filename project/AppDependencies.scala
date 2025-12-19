import sbt.*

object AppDependencies {

  private val  bootstrapVersion = "10.5.0"
  private val hmrcMongoVersion = "2.11.0"
  private val pekkoVersion = "1.4.0"

  private val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"        %% "play-frontend-hmrc-play-30"             % "12.25.0",
    "uk.gov.hmrc"        %% "play-conditional-form-mapping-play-30"  % "3.4.0",
    "uk.gov.hmrc"        %% "bootstrap-frontend-play-30"             % bootstrapVersion,
    "uk.gov.hmrc"        %% "tax-year"                               % "6.0.0",
    "uk.gov.hmrc"        %% "domain-play-30"                         % "13.0.0",
    "uk.gov.hmrc"        %% "crypto-json-play-30"                    % "8.4.0",
    "uk.gov.hmrc.mongo"  %% "hmrc-mongo-play-30"                     % hmrcMongoVersion,
    "org.typelevel"      %% "cats-core"                              % "2.13.0",
    "eu.timepit"         %% "refined"                                % "0.11.3",
    "org.apache.pekko"   %% "pekko-connectors-csv"                   % "1.2.0",
    "org.apache.pekko"   %% "pekko-slf4j"                            % pekkoVersion,
    "org.apache.pekko"   %% "pekko-actor-typed"                      % pekkoVersion,
    "org.apache.pekko"   %% "pekko-serialization-jackson"            % pekkoVersion,
    "org.apache.pekko"   %% "pekko-protobuf-v3"                      % pekkoVersion,
    "org.apache.pekko"   %% "pekko-stream"                           % pekkoVersion,
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus"       %% "scalacheck-1-18"         % "3.2.19.0",
    "org.jsoup"               %  "jsoup"                   % "1.21.2",
    "com.softwaremill.diffx"  %% "diffx-scalatest-should"  % "0.9.0"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
