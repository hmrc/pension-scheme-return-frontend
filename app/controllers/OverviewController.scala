/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import services._
import utils.DateTimeUtils
import viewmodels.implicits.stringToText
import play.api.mvc._
import config.{Constants, FrontendAppConfig}
import cats.implicits.toShow
import controllers.actions._
import uk.gov.hmrc.time.TaxYear
import viewmodels.DisplayMessage.Message
import views.html.OverviewView
import models.SchemeId.Srn
import config.Constants.UNCHANGED_SESSION_PREFIX
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions}
import pages.nonsipp.WhichTaxYearPage
import play.api.Logger
import utils.nonsipp.SchemeDetailNavigationUtils
import models.backend.responses._
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import viewmodels.OverviewSummary

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.Inject

class OverviewController @Inject()(
  override val messagesApi: MessagesApi,
  config: FrontendAppConfig,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  createData: DataCreationAction,
  prePopulatedData: PrePopulationDataActionProvider,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrOverviewService: PsrOverviewService,
  val psrVersionsService: PsrVersionsService,
  val psrRetrievalService: PsrRetrievalService,
  saveService: SaveService,
  view: OverviewView,
  taxYearService: TaxYearService,
  prePopulationService: PrePopulationService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport
    with SchemeDetailNavigationUtils {

  private val logger = Logger(classOf[OverviewController])

  private val allDates = OverviewController.options(taxYearService.current)

  private def outstandingData(
    srn: Srn,
    overview: Option[Seq[OverviewResponse]],
    versionsForYears: Seq[PsrVersionsForYearsResponse]
  )(implicit messages: Messages): Seq[OverviewSummary] =
    overview.fold(Seq[OverviewSummary]())(
      values =>
        values
          .filter(
            overviewResponse =>
              (
                // if a NTF is issued
                overviewResponse.numberOfVersions.getOrElse(-1) == 0
                  && overviewResponse.compiledVersionAvailable.getOrElse(YesNo.Yes) == YesNo.No
                  && overviewResponse.submittedVersionAvailable.getOrElse(YesNo.Yes) == YesNo.No
              ) || (
                // the only available version for a tax year is a compiled one
                overviewResponse.numberOfVersions.getOrElse(-1) == 1
                  && overviewResponse.compiledVersionAvailable.getOrElse(YesNo.No) == YesNo.Yes
                  && overviewResponse.submittedVersionAvailable.getOrElse(YesNo.Yes) == YesNo.No
              )
          )
          .map { overviewResponse =>
            val yearFrom = overviewResponse.periodStartDate
            val yearTo = overviewResponse.periodEndDate
            val compiled = overviewResponse.compiledVersionAvailable.getOrElse(YesNo.No) == YesNo.Yes

            val (status, url, label) = if (compiled) {
              val fbNumber = versionsForYears
                .find(x => LocalDate.parse(x.startDate) == yearFrom)
                .flatMap(_.data.sortBy(_.reportVersion).lastOption)
                .map(_.reportFormBundleNumber)
              (
                ReportStatus.ReportStatusCompiled,
                controllers.routes.OverviewController
                  .onSelectContinue(
                    srn,
                    formatDateForApi(yearFrom),
                    "001",
                    fbNumber,
                    overviewResponse.psrReportType.get.name,
                    prePopulationService.findLastSubmittedPsrFbInPreviousYears(versionsForYears, yearFrom)
                  )
                  .url,
                messages("site.continue")
              )
            } else {
              (
                ReportStatus.NotStarted,
                controllers.routes.OverviewController
                  .onSelectStart(
                    srn,
                    formatDateForApi(yearFrom),
                    "001",
                    overviewResponse.psrReportType.get.name,
                    prePopulationService.findLastSubmittedPsrFbInPreviousYears(versionsForYears, yearFrom)
                  )
                  .url,
                messages("site.start")
              )
            }
            OverviewSummary(
              key = Message("site.to", Message(yearFrom.show), Message(yearTo.show)),
              firstValue = Message(s"overview.status.$status"),
              secondValue = overviewResponse.psrDueDate.fold("")(dt => DateTimeUtils.formatHtml(dt)),
              actions = Some(
                Actions(
                  items = Seq(
                    ActionItem(
                      content = label,
                      href = url
                    )
                  )
                )
              ),
              yearFrom = yearFrom
            )
          }
    )

  private def previousData(
    srn: Srn,
    versionsForYears: Seq[PsrVersionsForYearsResponse],
    overview: Option[Seq[OverviewResponse]],
    overviewSummary: Seq[OverviewSummary]
  )(implicit messages: Messages): Seq[OverviewSummary] =
    versionsForYears.flatMap { versionsForTheYear =>
      val yearFrom = LocalDate.parse(versionsForTheYear.startDate)
      val reportVersions = versionsForTheYear.data.map(_.reportVersion)
      val lastReturnForTaxYear = versionsForTheYear.data
        .find(
          _.reportVersion == reportVersions.max // last one only
            && !overviewSummary
              .exists(os => os.yearFrom == yearFrom) // if it is not present in overview section already
        )
      lastReturnForTaxYear match {
        case None => Nil
        case Some(last) =>
          val matchingOverviewResponse =
            overview.getOrElse(Seq()).find(overviewResponse => overviewResponse.periodStartDate == yearFrom)
          val reportType = matchingOverviewResponse.fold(PsrReportType.Standard.name)(
            or => or.psrReportType.getOrElse(PsrReportType.Standard).name
          )
          val key = Message("site.to", Message(yearFrom.show), Message(yearFrom.plusYears(1).minusDays(1).show))
          if (last.reportStatus == ReportStatus.ReportStatusCompiled) {
            List(
              OverviewSummary(
                key = key,
                firstValue = Message("overview.previous.compiled.table.status"),
                secondValue = "",
                yearFrom = yearFrom,
                actions = Some(
                  Actions(
                    items = Seq(
                      ActionItem(
                        content = messages("site.continue"),
                        href = controllers.routes.OverviewController
                          .onSelectContinue(
                            srn,
                            formatDateForApi(yearFrom),
                            "%03d".format(last.reportVersion),
                            Some(last.reportFormBundleNumber),
                            reportType,
                            None
                          )
                          .url
                      )
                    )
                  )
                )
              )
            )
          } else {
            List(
              OverviewSummary(
                key = key,
                firstValue = Message(getSubmitter(last)),
                secondValue = "",
                yearFrom = yearFrom,
                submitted = true,
                actions = Some(
                  Actions(
                    items = Seq(
                      ActionItem(
                        content = messages("site.viewOrChange"),
                        href = controllers.routes.OverviewController
                          .onSelectViewAndChange(
                            srn,
                            last.reportFormBundleNumber,
                            formatDateForApi(yearFrom),
                            reportType
                          )
                          .url
                      )
                    )
                  )
                )
              )
            )
          }
      }
    }

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).async { implicit request =>
      val toDate = formatDateForApi(allDates.head._2.from)
      val fromDate = formatDateForApi(allDates.last._2.from)
      for {
        overviewResponse <- psrOverviewService.getOverview(request.schemeDetails.pstr, fromDate, toDate, srn)
        versionsForYearsResponse <- psrVersionsService
          .getVersionsForYears(request.schemeDetails.pstr, allDates.drop(1).map(dates => dates._2.from.toString), srn)
        outstanding = outstandingData(srn, overviewResponse, versionsForYearsResponse)
        previous = previousData(srn, versionsForYearsResponse, overviewResponse, outstanding)
      } yield Ok(view(outstanding, previous, request.schemeDetails.schemeName))
        .addingToSession((Constants.SRN, srn.value))
    }

  def onSelectStart(
    srn: Srn,
    taxYear: String,
    version: String,
    reportType: String,
    lastSubmittedPsrFbInPreviousYears: Option[String]
  ): Action[AnyContent] =
    identify
      .andThen(allowAccess(srn))
      .andThen(createData)
      .andThen(
        prePopulatedData(
          Option.when(reportType == PsrReportType.Standard.name)(lastSubmittedPsrFbInPreviousYears).flatten
        )
      )
      .async { implicit request =>
        reportType match {
          case PsrReportType.Sipp.name =>
            val sippUrl = s"${config.urls.sippBaseUrl}/${srn.value}${config.urls.sippStartJourney}"
            Future.successful(
              Redirect(sippUrl)
                .addingToSession(Constants.TAX_YEAR -> taxYear)
                .addingToSession(Constants.VERSION -> version)
            )
          case _ =>
            val yearFrom = LocalDate.parse(taxYear)
            val yearTo = yearFrom.plusYears(1).minusDays(1)
            val dateRange = DateRange(yearFrom, yearTo)
            for {
              userAnswers <- Future.fromTry(request.userAnswers.set(WhichTaxYearPage(srn), dateRange))
              _ <- saveService.save(userAnswers.copy(id = UNCHANGED_SESSION_PREFIX + userAnswers.id))
              _ <- saveService.save(userAnswers)
            } yield {
              if (lastSubmittedPsrFbInPreviousYears.isDefined) {
                Redirect(controllers.routes.CheckUpdateInformationController.onPageLoad(srn))
                  .addingToSession(Constants.PREPOPULATION_FLAG -> String.valueOf(true))
              } else {
                Redirect(
                  controllers.routes.WhatYouWillNeedController.onPageLoad(srn, "", taxYear, version)
                ).addingToSession(Constants.PREPOPULATION_FLAG -> String.valueOf(false))
              }
            }
        }
      }

  def onSelectContinue(
    srn: Srn,
    taxYear: String,
    version: String,
    fbNumber: Option[String],
    reportType: String,
    lastSubmittedPsrFbInPreviousYears: Option[String]
  ): Action[AnyContent] =
    identifyAndRequireData(srn, taxYear, version).async { implicit request =>
      reportType match {
        case PsrReportType.Sipp.name =>
          version.toIntOption match {
            case None =>
              logger.error(s"Could not parse version [$version] to int")
              Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
            case Some(versionInt) =>
              val path = if (versionInt == 1) {
                config.urls.sippContinueJourney //Standard journey - undeclared changes made
              } else {
                config.urls.sippViewAndChange
              }

              val sippUrl = s"${config.urls.sippBaseUrl}/${srn.value}$path"
              Future.successful {
                val result = Redirect(sippUrl)
                  .addingToSession(Constants.TAX_YEAR -> taxYear)
                  .addingToSession(Constants.VERSION -> version)

                fbNumber
                  .map(fb => result.addingToSession(Constants.FB_NUMBER -> fb))
                  .getOrElse(result)
              }
          }

        case _ =>
          Future.successful(
            Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn))
              .addingToSession(
                Constants.PREPOPULATION_FLAG -> String.valueOf(lastSubmittedPsrFbInPreviousYears.isDefined)
              )
          )
      }
    }

  def onSelectViewAndChange(
    srn: Srn,
    fbNumber: String,
    taxYear: String,
    reportType: String
  ): Action[AnyContent] =
    identifyAndRequireData(srn, fbNumber).async { implicit request =>
      reportType match {
        case PsrReportType.Sipp.name =>
          val sippUrl = s"${config.urls.sippBaseUrl}/${srn.value}${config.urls.sippViewAndChange}"
          Future.successful(
            Redirect(sippUrl)
              .addingToSession(Constants.FB_NUMBER -> fbNumber)
              .addingToSession(Constants.TAX_YEAR -> taxYear)
          )
        case _ =>
          val byPassedJourney =
            Redirect(controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, CheckMode))
          val regularJourney = Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn))
          isJourneyBypassed(srn)
            .map(res => res.map(if (_) byPassedJourney else regularJourney).merge)
            .map(_.addingToSession(Constants.PREPOPULATION_FLAG -> String.valueOf(false)))
      }
    }
}

object OverviewController {

  def options(startingTaxYear: TaxYear): List[(String, DateRange)] = {

    val taxYears = startingTaxYear.next +: List.iterate(startingTaxYear, 7)(_.previous)

    val taxYearRanges = taxYears.map(DateRange.from).map(r => (r.toString, r))

    Enumerable(taxYearRanges: _*).toList
  }
}
