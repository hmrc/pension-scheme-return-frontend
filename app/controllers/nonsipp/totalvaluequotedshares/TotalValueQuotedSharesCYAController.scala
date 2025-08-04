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

package controllers.nonsipp.totalvaluequotedshares

import services.{PsrSubmissionService, SchemeDateService}
import pages.nonsipp.totalvaluequotedshares._
import play.api.mvc._
import utils.nonsipp.summary.TotalValueQuotedSharesCheckAnswersUtils
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import controllers.actions._
import models._
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n._
import models.requests.DataRequest
import views.html.CYAWithRemove
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TotalValueQuotedSharesCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: CYAWithRemove
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn, NormalMode, showBackLink = true)
    }
  def onPageLoadViewOnly(
    srn: Srn,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, current, previous) { implicit request =>
      val showBackLink = true
      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = request.previousUserAnswers match {
          case Some(previousUserAnswers) =>
            val updated = getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              previousUserAnswers,
              Paths.quotedShares
            ) == Updated
            updated
          case _ => false
        },
        year = year,
        currentVersion = current,
        previousVersion = previous,
        compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
      )
      onPageLoadCommon(srn, ViewOnlyMode, Some(viewOnlyViewModel), showBackLink)
    }

  def onPreviousViewOnly(
    srn: Srn,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] = identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)) {
    implicit request =>
      val showBackLink = false
      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = request.previousUserAnswers match {
          case Some(previousUserAnswers) =>
            val updated = getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              previousUserAnswers,
              Paths.quotedShares
            ) == Updated
            updated
          case _ => false
        },
        year = year,
        currentVersion = (current - 1).max(0),
        previousVersion = (previous - 1).max(0),
        compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
      )
      onPageLoadCommon(srn, ViewOnlyMode, Some(viewOnlyViewModel), showBackLink)
  }

  private def onPageLoadCommon(
    srn: Srn,
    mode: Mode,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  )(implicit
    request: DataRequest[?]
  ) =
    schemeDateService.taxYearOrAccountingPeriods(srn) match {
      case Some(periods) =>
        val totalCost = request.userAnswers.get(TotalValueQuotedSharesPage(srn))
        Ok(
          view(
            TotalValueQuotedSharesCheckAnswersUtils.viewModel(
              srn,
              totalCost,
              periods,
              request.schemeDetails,
              mode,
              viewOnlyViewModel,
              showBackLink = showBackLink
            )
          )
        )
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  def onSubmit(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    psrSubmissionService
      .submitPsrDetails(
        srn,
        fallbackCall =
          controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController.onPageLoad(srn)
      )
      .map {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          Redirect(navigator.nextPage(TotalValueQuotedSharesCYAPage(srn), NormalMode, request.userAnswers))
      }
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, current, previous).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
    }
}
