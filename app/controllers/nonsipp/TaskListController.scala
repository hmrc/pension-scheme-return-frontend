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

package controllers.nonsipp

import services.{PsrVersionsService, SchemeDateService}
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import controllers.PSRController
import cats.implicits.toShow
import controllers.actions._
import utils.nonsipp.TaskListUtils._
import models.backend.responses.ReportStatus
import viewmodels.models.TaskListStatus._
import play.api.i18n.MessagesApi
import views.html.TaskListView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate

class TaskListController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  schemeDateService: SchemeDateService,
  psrVersionsService: PsrVersionsService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    schemeDateService.schemeDate(srn) match {
      case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(dates) =>
        for {
          response <- psrVersionsService.getVersions(request.schemeDetails.pstr, formatDateForApi(dates.from))
          hasHistory = response
            .filter(
              psrVersionsService =>
                psrVersionsService.reportStatus == ReportStatus.SubmittedAndInProgress
                  || psrVersionsService.reportStatus == ReportStatus.SubmittedAndSuccessfullyProcessed
            )
            .toList
            .nonEmpty
          viewModel = TaskListController.viewModel(
            srn,
            request.schemeDetails.schemeName,
            dates.from,
            dates.to,
            request.userAnswers,
            request.pensionSchemeId,
            hasHistory
          )
        } yield Ok(view(viewModel))
    }
  }
}

object TaskListController {

  def messageKey(prefix: String, suffix: String, status: TaskListStatus): String =
    status match {
      case UnableToStart | NotStarted => s"$prefix.add.$suffix"
      case _ => s"$prefix.change.$suffix"
    }

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate,
    userAnswers: UserAnswers,
    pensionSchemeId: PensionSchemeId,
    hasHistory: Boolean = false
  ): PageViewModel[TaskListViewModel] = {

    val sectionList = getSectionList(srn, schemeName, userAnswers, pensionSchemeId)

    val historyLink = if (hasHistory) {
      Some(
        LinkMessage(
          Message("nonsipp.tasklist.history"),
          controllers.routes.ReturnsSubmittedController.onPageLoad(srn, 1).url
        )
      )
    } else {
      None
    }

    val viewModel = TaskListViewModel(
      hasHistory,
      historyLink,
      sectionList.head,
      sectionList.tail: _*
    )

    val (numberOfCompleted, numberOfTotal) = evaluateCompletedTotalTuple(viewModel.sections.toList)

    PageViewModel(
      Message("nonsipp.tasklist.title", startDate.show, endDate.show),
      Message("nonsipp.tasklist.heading", startDate.show, endDate.show),
      viewModel
    ).withDescription(
      Heading2.small("nonsipp.tasklist.subheading.incomplete") ++
        ParagraphMessage(Message("nonsipp.tasklist.description", numberOfCompleted, numberOfTotal))
    )
  }

}
