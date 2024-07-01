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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.{Constants, FrontendAppConfig}
import controllers.actions._
import pages.nonsipp.WhichTaxYearPage
import models.backend.responses._
import uk.gov.hmrc.time.TaxYear
import viewmodels.DisplayMessage.Message
import views.html.OverviewView
import models.SchemeId.Srn
import cats.implicits.toShow
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import config.Constants.UNCHANGED_SESSION_PREFIX
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions}
import utils.DateTimeUtils.localDateShow
import models.{DateRange, Enumerable}
import play.api.i18n.{I18nSupport, MessagesApi}
import viewmodels.OverviewSummary

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.Inject

class OverviewController @Inject()(
  override val messagesApi: MessagesApi,
  config: FrontendAppConfig,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  createData: DataCreationAction,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrOverviewService: PsrOverviewService,
  psrVersionsService: PsrVersionsService,
  psrRetrievalService: PsrRetrievalService,
  saveService: SaveService,
  view: OverviewView,
  taxYearService: TaxYearService
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private val allDates = OverviewController.options(taxYearService.current)

  private def outstandingData(
    srn: Srn,
    overview: Option[Seq[OverviewResponse]],
    labelStart: String,
    labelContinue: String
  ): Seq[OverviewSummary] =
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
              (
                ReportStatus.ReportStatusCompiled,
                controllers.routes.OverviewController
                  .onSelectContinue(srn, formatDateForApi(yearFrom), "001", overviewResponse.psrReportType.get.name)
                  .url,
                labelContinue
              )
            } else {
              (
                ReportStatus.NotStarted,
                controllers.routes.OverviewController
                  .onSelectStart(srn, formatDateForApi(yearFrom), "001", overviewResponse.psrReportType.get.name)
                  .url,
                labelStart
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
                      content = Text(label),
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
    response: Seq[PsrVersionsForYearsResponse],
    overview: Option[Seq[OverviewResponse]],
    labelView: String,
    labelContinue: String,
    overviewSummary: Seq[OverviewSummary]
  ): Seq[OverviewSummary] =
    response.flatMap { versionsForYearsResponse =>
      val yearFrom = LocalDate.parse(versionsForYearsResponse.startDate)
      val reportVersions = versionsForYearsResponse.data.map(_.reportVersion)
      val lastReturnForTaxYear = versionsForYearsResponse.data
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
                        content = Text(labelContinue),
                        href = controllers.routes.OverviewController
                          .onSelectContinue(
                            srn,
                            formatDateForApi(yearFrom),
                            "%03d".format(last.reportVersion),
                            reportType
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
                        content = Text(labelView),
                        href = controllers.routes.OverviewController
                          .onSelectViewAndChange(srn, last.reportFormBundleNumber, reportType)
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
    identify.andThen(allowAccess(srn)).andThen(getData).async { implicit request =>
      val toDate = formatDateForApi(allDates.toList.head._2.from)
      val fromDate = formatDateForApi(allDates.toList.reverse.head._2.from)
      val labelView = messagesApi.preferred(request)("site.viewOrChange")
      val labelStart = messagesApi.preferred(request)("site.start")
      val labelContinue = messagesApi.preferred(request)("site.continue")
      for {
        overviewResponse <- psrOverviewService.getOverview(request.schemeDetails.pstr, fromDate, toDate)
        getVersionsResponse <- psrVersionsService
          .getVersionsForYears(request.schemeDetails.pstr, allDates.toList.map(dates => dates._2.from.toString))
        outstanding = outstandingData(srn, overviewResponse, labelStart, labelContinue)
        previous = previousData(srn, getVersionsResponse, overviewResponse, labelView, labelContinue, outstanding)
      } yield Ok(view(outstanding, previous, request.schemeDetails.schemeName))
        .addingToSession((Constants.SRN, srn.value))
    }

  def onSelectStart(srn: Srn, taxYear: String, version: String, reportType: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).andThen(createData).async { implicit request =>
      reportType match {
        case PsrReportType.Sipp.name =>
          val sippUrl = s"${config.urls.sippBaseUrl}/${srn.value}${config.urls.sippStartJourney}"
          Future.successful(Redirect(sippUrl))
        case _ =>
          val yearFrom = LocalDate.parse(taxYear)
          val yearTo = yearFrom.plusYears(1).minusDays(1)
          val dateRange = DateRange(yearFrom, yearTo)
          for {
            userAnswers <- Future.fromTry(request.userAnswers.set(WhichTaxYearPage(srn), dateRange))
            _ <- saveService.save(userAnswers.copy(id = UNCHANGED_SESSION_PREFIX + userAnswers.id))
            _ <- saveService.save(userAnswers)
          } yield {
            Redirect(controllers.routes.WhatYouWillNeedController.onPageLoad(srn, "", taxYear, version))
          }
      }
    }

  def onSelectContinue(srn: Srn, taxYear: String, version: String, reportType: String): Action[AnyContent] =
    identifyAndRequireData(srn, taxYear, version).async { _ =>
      reportType match {
        case PsrReportType.Sipp.name =>
          val sippUrl = s"${config.urls.sippBaseUrl}/${srn.value}${config.urls.sippContinueJourney}"
          Future.successful(Redirect(sippUrl))
        case _ =>
          Future.successful(Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))
      }
    }

  def onSelectViewAndChange(srn: Srn, fbNumber: String, reportType: String): Action[AnyContent] =
    identifyAndRequireData(srn, fbNumber).async { _ =>
      reportType match {
        case PsrReportType.Sipp.name =>
          val sippUrl = s"${config.urls.sippBaseUrl}/${srn.value}${config.urls.sippViewAndChange}?fbNumber=$fbNumber"
          Future.successful(Redirect(sippUrl))
        case _ =>
          Future.successful(Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))
      }
    }
}

object OverviewController {

  def options(startingTaxYear: TaxYear): Enumerable[DateRange] = {

    val taxYears = List.iterate(startingTaxYear, 7)(_.previous)

    val taxYearRanges = taxYears.map(DateRange.from).map(r => (r.toString, r))

    Enumerable(taxYearRanges: _*)
  }
}
