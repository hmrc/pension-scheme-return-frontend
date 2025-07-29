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

package controllers.nonsipp.schemedesignatory

import services.{PsrSubmissionService, SchemeDateService}
import pages.nonsipp.schemedesignatory._
import play.api.mvc._
import utils.nonsipp.summary.FinancialDetailsCheckAnswersUtils
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getFinancialDetailsCompletedOrUpdated
import controllers.nonsipp.routes
import controllers.actions._
import models._
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n._
import models.requests.DataRequest
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class FinancialDetailsCheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    onPageLoadCommon(srn, mode)
  }

  def onPageLoadViewOnly(
    srn: Srn,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      val showBackLink = true
      onPageLoadCommon(srn, mode, showBackLink)
    }

  def onPageLoadCommon(srn: Srn, mode: Mode, showBackLink: Boolean = true)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    schemeDateService.schemeDate(srn) match {
      case Some(periods) =>
        val howMuchCashPage = request.userAnswers.get(HowMuchCashPage(srn, mode))
        val valueOfAssetsPage = request.userAnswers.get(ValueOfAssetsPage(srn, mode))
        val feesCommissionsWagesSalariesPage = request.userAnswers.get(FeesCommissionsWagesSalariesPage(srn, mode))
        Ok(
          view(
            FinancialDetailsCheckAnswersUtils.viewModel(
              srn,
              mode,
              howMuchCashPage,
              valueOfAssetsPage,
              feesCommissionsWagesSalariesPage,
              periods,
              request.schemeDetails,
              viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                getFinancialDetailsCompletedOrUpdated(request.userAnswers, request.previousUserAnswers.get) == Updated
              } else {
                false
              },
              optYear = request.year,
              optCurrentVersion = request.currentVersion,
              optPreviousVersion = request.previousVersion,
              compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn)),
              showBackLink = showBackLink
            )
          )
        )
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    psrSubmissionService
      .submitPsrDetails(
        srn,
        fallbackCall = controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
          .onPageLoad(srn, NormalMode)
      )
      .map {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          Redirect(navigator.nextPage(FinancialDetailsCheckYourAnswersPage(srn), mode, request.userAnswers))
      }
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, current, previous).async {
      Future.successful(Redirect(routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous)))
    }

  def onPreviousViewOnly(
    srn: Srn,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)) { implicit request =>
      val showBackLink = false
      onPageLoadCommon(srn, ViewOnlyMode, showBackLink)
    }

}
