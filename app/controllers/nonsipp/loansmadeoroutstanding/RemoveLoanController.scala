/*
 * Copyright 2023 HM Revenue & Customs
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

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import models.{IdentitySubject, IdentityType, Mode}
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}
import pages.nonsipp.loansmadeoroutstanding._
import play.api.i18n.MessagesApi
import views.html.YesNoPageView
import models.SchemeId.Srn
import forms.YesNoPageFormProvider
import controllers.nonsipp.loansmadeoroutstanding.RemoveLoanController._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveLoanController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = RemoveLoanController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      getResult(srn, index, mode, request.userAnswers.fillForm(RemoveLoanPage(srn, index), form))
  }

  private def getResult(srn: Srn, index: Max5000, mode: Mode, form: Form[Boolean], error: Boolean = false)(
    implicit request: DataRequest[_]
  ) = {
    val whoReceivedLoanPage = request.userAnswers
      .get(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient))
    whoReceivedLoanPage match {
      case Some(who) => {
        val recipientName =
          who match {
            case IdentityType.Individual =>
              request.userAnswers.get(IndividualRecipientNamePage(srn, index)).getOrRecoverJourney
            case IdentityType.UKCompany =>
              request.userAnswers.get(CompanyRecipientNamePage(srn, index)).getOrRecoverJourney
            case IdentityType.UKPartnership =>
              request.userAnswers.get(PartnershipRecipientNamePage(srn, index)).getOrRecoverJourney
            case IdentityType.Other =>
              request.userAnswers
                .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LoanRecipient))
                .map(_.name)
                .getOrRecoverJourney
          }
        recipientName.fold(
          l => l,
          name => {
            val loanAmount =
              request.userAnswers.get(AmountOfTheLoanPage(srn, index)).map(_._1).getOrRecoverJourney
            loanAmount.fold(
              l => l,
              amount =>
                if (error) {
                  BadRequest(view(form, viewModel(srn, index, mode, amount.displayAs, name)))
                } else {
                  Ok(view(form, viewModel(srn, index, mode, amount.displayAs, name)))
                }
            )
          }
        )
      }
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(getResult(srn, index, mode, formWithErrors, true)),
          value =>
            if (value) {
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.remove(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient)))
                _ <- saveService.save(updatedAnswers)
                redirectTo <- psrSubmissionService
                  .submitPsrDetails(srn)(implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                  .map {
                    case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    case Some(_) => Redirect(navigator.nextPage(RemoveLoanPage(srn, index), mode, updatedAnswers))
                  }
              } yield redirectTo
            } else {
              Future
                .successful(Redirect(navigator.nextPage(RemoveLoanPage(srn, index), mode, request.userAnswers)))
            }
        )
  }
}

object RemoveLoanController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeLoan.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    loanAmount: String,
    recipientName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    (
      YesNoPageViewModel(
        "removeLoan.title",
        Message("removeLoan.heading", loanAmount, recipientName),
        routes.RemoveLoanController.onSubmit(srn, index, mode)
      )
    )
}
