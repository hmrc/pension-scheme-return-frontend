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

package controllers.nonsipp.loansmadeoroutstanding

import services.{PsrSubmissionService, SaveService, SchemeDateService}
import play.api.mvc._
import utils.nonsipp.summary.LoansCheckAnswersUtils
import utils.IntUtils.toRefined5000
import controllers.actions._
import navigation.Navigator
import models._
import pages.nonsipp.loansmadeoroutstanding._
import play.api.i18n._
import viewmodels.models._
import models.requests.DataRequest
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LoansCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  saveService: SaveService,
  schemeDateService: SchemeDateService,
  psrSubmissionService: PsrSubmissionService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  given SchemeDateService = schemeDateService

  def onPageLoad(
    srn: Srn,
    index: Int,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Int,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)
    }

  def onPageLoadCommon(srn: Srn, index: Max5000, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    if (
      !request.userAnswers
        .get(LoansProgress(srn, index))
        .exists(_.completed)
    ) {
      Redirect(routes.LoansListController.onPageLoad(srn, 1, mode))
    } else {
      LoansCheckAnswersUtils(schemeDateService)
        .summaryData(srn, index, mode)
        .map { data =>
          Ok(
            view(
              LoansCheckAnswersUtils(schemeDateService).viewModel(
                data.srn,
                data.index,
                data.schemeName,
                data.receivedLoanType,
                data.recipientName,
                data.recipientDetails,
                data.recipientReasonNoDetails,
                data.connectedParty,
                data.datePeriodLoan,
                data.amountOfTheLoan,
                data.returnEndDate,
                data.repaymentInstalments,
                data.interestOnLoan,
                data.arrearsPrevYears,
                data.outstandingArrearsOnLoan,
                data.securityOnLoan,
                data.mode,
                data.viewOnlyUpdated,
                data.optYear,
                data.optCurrentVersion,
                data.optPreviousVersion
              )
            )
          )
        }
        .merge
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val prePopulated = request.userAnswers.get(LoanPrePopulated(srn, index))

      for {
        updatedAnswers <- Future.fromTry(
          request.userAnswers
            .set(LoansMadeOrOutstandingPage(srn), true)
            .set(LoanCompleted(srn, index), SectionCompleted)
            .setWhen(prePopulated.isDefined)(LoanPrePopulated(srn, index), true)
        )
        _ <- saveService.save(updatedAnswers)
        redirectTo <- psrSubmissionService
          .submitPsrDetailsWithUA(
            srn,
            updatedAnswers,
            fallbackCall =
              controllers.nonsipp.loansmadeoroutstanding.routes.LoansCYAController.onPageLoad(srn, index, mode)
          )
          .map {
            case None => controllers.routes.JourneyRecoveryController.onPageLoad()
            case Some(_) => navigator.nextPage(LoansCYAPage(srn), NormalMode, request.userAnswers)
          }
      } yield Redirect(redirectTo)
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.loansmadeoroutstanding.routes.LoansListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}
