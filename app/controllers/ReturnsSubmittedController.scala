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

import services.{PsrRetrievalService, PsrVersionsService}
import utils.DateTimeUtils
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Constants
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.WhichTaxYearPage
import models._
import controllers.ReturnsSubmittedController.viewModel
import play.api.i18n.MessagesApi
import views.html.ReturnsSubmittedView
import models.SchemeId.Srn
import utils.nonsipp.SchemeDetailNavigationUtils
import models.backend.responses.ReportStatus
import viewmodels.DisplayMessage.{LinkMessage, Message}
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class ReturnsSubmittedController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  val psrVersionsService: PsrVersionsService,
  val psrRetrievalService: PsrRetrievalService,
  view: ReturnsSubmittedView
)(implicit ec: ExecutionContext)
    extends PSRController
    with SchemeDetailNavigationUtils {

  def onPageLoad(srn: Srn, page: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      request.userAnswers.get(WhichTaxYearPage(srn)) match {
        case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(dateRange: DateRange) =>
          val response =
            psrVersionsService.getVersions(request.schemeDetails.pstr, formatDateForApi(dateRange.from), srn)
          response.map { seqPsrVersionsResponse =>
            val reportVersions = seqPsrVersionsResponse.map(_.reportVersion)
            val listRetHistorySummary = seqPsrVersionsResponse
              .sortBy(_.reportVersion)
              .reverse
              .map { psrVersionsResponse =>
                val maxReportVersion = reportVersions.max
                val currentVersion = psrVersionsResponse.reportVersion
                val submitter =
                  if (
                    currentVersion == maxReportVersion && psrVersionsResponse.reportStatus == ReportStatus.ReportStatusCompiled
                  ) {
                    "returnsSubmitted.inProgress"
                  } else {
                    getSubmitter(psrVersionsResponse)
                  }
                List(
                  TableElem(Message(currentVersion.toString)),
                  TableElem(
                    Message(DateTimeUtils.formatHtml(psrVersionsResponse.compilationOrSubmissionDate.toLocalDate))
                  ),
                  TableElem(Message(submitter)),
                  if (currentVersion == maxReportVersion) {
                    if (psrVersionsResponse.reportStatus == ReportStatus.ReportStatusCompiled) {
                      TableElem(
                        LinkMessage(
                          Message("site.continue"),
                          controllers.routes.ReturnsSubmittedController
                            .onSelect(srn, psrVersionsResponse.reportFormBundleNumber)
                            .url,
                          hiddenText = Message("returnsSubmitted.continue.hiddenText")
                        )
                      )
                    } else {
                      TableElem(
                        LinkMessage(
                          Message("site.viewOrChange"),
                          controllers.routes.ReturnsSubmittedController
                            .onSelect(srn, psrVersionsResponse.reportFormBundleNumber)
                            .url
                        )
                      )
                    }
                  } else {
                    TableElem(
                      LinkMessage(
                        Message("site.view"),
                        controllers.routes.ReturnsSubmittedController
                          .onSelectToView(srn, formatDateForApi(dateRange.from), currentVersion, currentVersion - 1)
                          .url
                      )
                    )
                  }
                )
              }
              .toList
            Ok(
              view(
                viewModel(
                  srn,
                  page,
                  listRetHistorySummary,
                  DateTimeUtils.formatHtml(dateRange.from),
                  DateTimeUtils.formatHtml(dateRange.to),
                  request.schemeDetails.schemeName
                )
              )
            )
          }
      }
    }

  def onSelect(srn: Srn, fbNumber: String): Action[AnyContent] =
    identifyAndRequireData(srn, fbNumber).async {
      Future.successful(Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))
    }

  def onSelectToView(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, current, previous).async { implicit request =>
      val byPassedJourney =
        Redirect(
          controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoadViewOnly(
            srn,
            year,
            current,
            previous
          )
        )
      val regularJourney = Redirect(
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(
            srn,
            year,
            current,
            previous
          )
      )
      isJourneyBypassed(srn).map(res => res.map(if (_) byPassedJourney else regularJourney).merge)
    }
}

object ReturnsSubmittedController {

  def viewModel(
    srn: Srn,
    page: Int,
    data: List[List[TableElem]],
    fromYear: String,
    toYear: String,
    schemeName: String
  ): FormPageViewModel[ActionTableViewModel] = {

    val pagination = Pagination(
      currentPage = page,
      pageSize = Constants.returnsSubmittedPageSize,
      totalSize = data.size,
      call = controllers.routes.ReturnsSubmittedController.onPageLoad(srn, _)
    )
    FormPageViewModel(
      title = Message("returnsSubmitted.title", fromYear, toYear),
      heading = Message("returnsSubmitted.heading", fromYear, toYear),
      description = Some(Message(schemeName)),
      page = ActionTableViewModel(
        inset = "",
        head = Some(
          List(
            TableElem("returnsSubmitted.table.header1"),
            TableElem("returnsSubmitted.table.header2"),
            TableElem("returnsSubmitted.table.header3"),
            TableElem.empty
          )
        ),
        rows = data,
        paginatedViewModel = Some(
          PaginatedViewModel(
            Message(
              "returnsSubmitted.list.pagination.label",
              pagination.pageStart,
              pagination.pageEnd,
              pagination.totalSize
            ),
            pagination
          )
        )
      ),
      refresh = None,
      buttonText = Message("returnsSubmitted.link", schemeName),
      details = None,
      onSubmit = controllers.nonsipp.routes.TaskListController.onPageLoad(srn) // not used
    )
  }
}
