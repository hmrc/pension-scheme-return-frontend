import play.sbt.routes.RoutesKeys
import sbt.Def
import scoverage.ScoverageKeys
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

import scala.sys.process.*

lazy val appName: String = "pension-scheme-return-frontend"

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings) *)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(itSettings) *)
  .settings(majorVersion := 0)
  .settings(ThisBuild / useSuperShell := false)
  .settings(
    scalaVersion := "2.13.8",
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "models._",
      "models.CheckOrChange._",
      "models.ManualOrUpload._",
      "models.SchemeId._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
      "config.Binders._",
      "config.Refined._",
      "eu.timepit.refined.refineMV",
      "eu.timepit.refined.auto._"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "views.ViewUtils._",
      "models.Mode",
      "controllers.routes._",
      "viewmodels.govuk.all._",
      "viewmodels._",
      "viewmodels.models._",
      "views.components.Components._",
      "utils.ListUtils._"
    ),
    PlayKeys.playDefaultPort := 10701,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*handlers.*;.*components.*;" +
      ".*Routes.*;.*viewmodels.govuk.*;",
    ScoverageKeys.coverageMinimumStmtTotal := 78,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions ++= Seq(
      "-feature",
      "-rootdir",
      baseDirectory.value.getCanonicalPath,
      "-Wconf:cat=deprecation:e,cat=feature:ws,cat=optimizer:ws,src=target/.*:s"
    ),
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    update / evictionWarningOptions :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    resolvers ++= Seq(
      Resolver.jcenterRepo,
      "HMRC-open-artefacts-maven".at("https://open.artefacts.tax.service.gov.uk/maven2")
    ),
    // concatenate js
    Concat.groups := Seq(
      "javascripts/application.js" ->
        group(
          Seq(
            "javascripts/accessible-autocomplete.min.js",
            "javascripts/app.js"
          )
        )
    ),
    // prevent removal of unused code which generates warning errors due to use of third-party libs
    uglifyCompressOptions := Seq("unused=false", "dead_code=false"),
    pipelineStages := Seq(digest),
    // below line required to force asset pipeline to operate in dev rather than only prod
    Assets / pipelineStages := Seq(concat, uglify),
    // only compress files generated by concat
    uglify / includeFilter := GlobFilter("application.js"),
    // auto-run migrate script after g8Scaffold task
    g8Scaffold := {
      g8Scaffold.evaluated
      streams.value.log.info("Running migrate script")
      val scriptPath = baseDirectory.value.getCanonicalPath + "/migrate.sh"
      s"bash -c $scriptPath".!
    },
    scalafmtOnCompile := true,
    addCommandAlias("runLocal", "run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes")
  )

lazy val testSettings: Seq[Def.Setting[?]] = Seq(
  fork := false,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val itSettings = Defaults.itSettings ++ Seq(
  unmanagedSourceDirectories := Seq(
    baseDirectory.value / "it",
    baseDirectory.value / "test-utils"
  ),
  unmanagedResourceDirectories := Seq(
    baseDirectory.value / "it" / "resources"
  ),
  parallelExecution := false,
  fork := false
)
