import play.sbt.routes.RoutesKeys
import sbt.Def
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

import scala.sys.process.*
import complete.*
import complete.DefaultParsers.*
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.12"

lazy val appName: String = "pension-scheme-return-frontend"

addCompilerPlugin(("org.typelevel" % "kind-projector" % "0.13.2").cross(CrossVersion.full))

lazy val root = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings) *)
  .settings(ThisBuild / useSuperShell := false)
  .settings(
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    RoutesKeys.routesImport ++= Seq(
      "models._",
      "models.ManualOrUpload._",
      "models.SchemeId._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl",
      "config.Binders._",
      "config.RefinedTypes._",
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
    scalacOptions ++= Seq(
      "-feature",
      "-rootdir",
      baseDirectory.value.getCanonicalPath,
      "-Wconf:cat=deprecation:e,cat=feature:ws,cat=optimizer:ws,src=target/.*:s"
    ),
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
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
      scaffoldTask.evaluated
      streams.value.log.info("Running migrate script")
      val scriptPath = baseDirectory.value.getCanonicalPath + "/migrate.sh"
      s"bash -c $scriptPath".!
    },
    scalafmtOnCompile := true,
    scalafixOnCompile := true,
    addCommandAlias("runLocal", "run -Dapplication.router=testOnlyDoNotUseInAppConf.Routes")
  )
  .settings(CodeCoverageSettings.settings *)

lazy val testSettings: Seq[Def.Setting[?]] = Seq(
  scalafmtOnCompile := true,
  scalafixOnCompile := true,
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val it = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(root % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(
    libraryDependencies ++= AppDependencies.test,
    Test / fork := true,
    Test / scalafmtOnCompile := true,
    Test / unmanagedResourceDirectories += baseDirectory.value / "it" / "test" / "resources"
  )

lazy val scaffoldParser: Def.Initialize[State => Parser[(String, List[String])]] =
  Def.setting {
    val dir = g8ScaffoldTemplatesDirectory.value
    (state: State) =>
      val templateFiles: List[String] = Option(dir.listFiles).toList.flatten
        .filter(f => f.isDirectory && !f.isHidden && f.name != "scripts")
        .map(_.getName)

      val templates: List[Parser[String]] = templateFiles.map(s => s: Parser[String])

      // optionally captures a --key=value arg
      val optionalTemplateArgs = StringBasic.examples(
        FixedSetExamples(List("--key=value")),
        maxNumberOfExamples = 0,
        removeInvalidExamples = true
      )

      (Space ~> templates.reduce(_ | _).examples(templateFiles: _*) ~
        (Space ~> optionalTemplateArgs).*).map {
        case tmp ~ args => (tmp, args.toList)
      }
  }

lazy val scaffoldTask =
  Def.inputTask {
    val (name, args) = scaffoldParser.parsed
    val folder = g8ScaffoldTemplatesDirectory.value
    giter8.G8
      .fromDirectoryRaw(folder / name, baseDirectory.value, args, forceOverwrite = false)
      .fold(
        e => sys.error(e),
        r => println(s"Success : $r)")
      )
  }

addCommandAlias("testc", "; clean ; coverage ; test ; it/test ; coverageReport ;")
