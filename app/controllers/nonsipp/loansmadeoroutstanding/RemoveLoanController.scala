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

import cats.implicits._
import config.Refined.{Max9999999, OneTo9999999}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.RemoveLoanController._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.{refineMV, refineV}
import forms.YesNoPageFormProvider
import models.{Mode, Money, ReceivedLoanType}
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{
  AmountOfTheLoanPage,
  CompanyRecipientNamePage,
  IndividualRecipientNamePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientNamePage,
  RemoveLoanPage,
  WhoReceivedLoanPage,
  WhoReceivedLoans
}
import pages.nonsipp.memberdetails.MemberDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemoveLoanController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = RemoveLoanController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      val preparedForm = request.userAnswers.fillForm(RemoveLoanPage(srn, index), form)
      val whoReceivedLoanPage = request.userAnswers
        .get(WhoReceivedLoanPage(srn, index))
      whoReceivedLoanPage match {
        case Some(who) => {
//          TODO remove
//          val indexFail: Max9999999 = refineMV(13)
          val recipientName =
            who match {
              case ReceivedLoanType.Individual =>
                request.userAnswers.get(IndividualRecipientNamePage(srn, index)).getOrRecoverJourney
              case ReceivedLoanType.UKCompany =>
                request.userAnswers.get(CompanyRecipientNamePage(srn, index)).getOrRecoverJourney
              case ReceivedLoanType.UKPartnership =>
                request.userAnswers.get(PartnershipRecipientNamePage(srn, index)).getOrRecoverJourney
              case ReceivedLoanType.Other =>
                request.userAnswers.get(OtherRecipientDetailsPage(srn, index)).map(_.name).getOrRecoverJourney
            }
          recipientName.fold(
            l => l,
            name => {
              val loanAmount =
                request.userAnswers.get(AmountOfTheLoanPage(srn, index)).map(_._1).getOrRecoverJourney
              loanAmount.fold(
                l => l,
                amount => Ok(view(preparedForm, viewModel(srn, index, mode, amount.displayAs, name)))
              )
            }
          )
        }
        case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  def onSubmit(srn: Srn, index: Max9999999, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          // TODO get loan amount and recipientName
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode, "", "")))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.remove(RemoveLoanPage(srn, index))) // TODO remove all data
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(RemoveLoanPage(srn, index), mode, updatedAnswers))
        )
  }

  private def loanRecipientDetails(srn: Srn, index: Max9999999)(
    implicit request: DataRequest[_]
  ): (Either[Result, String], Either[Result, Money]) = {

    val whoReceivedLoanPage = request.userAnswers
      .get(WhoReceivedLoanPage(srn, index))
      .get // TODO else

    val recipientName = whoReceivedLoanPage match {
      case ReceivedLoanType.Individual =>
        request.userAnswers.get(IndividualRecipientNamePage(srn, index)).getOrRecoverJourney
      case ReceivedLoanType.UKCompany =>
        request.userAnswers.get(CompanyRecipientNamePage(srn, index)).getOrRecoverJourney
      case ReceivedLoanType.UKPartnership =>
        request.userAnswers.get(PartnershipRecipientNamePage(srn, index)).getOrRecoverJourney
      case ReceivedLoanType.Other =>
        request.userAnswers.get(OtherRecipientDetailsPage(srn, index)).map(_.name).getOrRecoverJourney
    }
    val loanAmount = request.userAnswers.get(AmountOfTheLoanPage(srn, index)).map(_._1).getOrRecoverJourney

    val recipientDetails = (recipientName, loanAmount)
    recipientDetails
//        case recipientName =>
//          request.userAnswers
//            .get(AmountOfTheLoanPage(srn, index))
//            .map(_._1)
//            .getOrRecoverJourney
//            .map((index, recipientName, _))
//      }
  }
}

object RemoveLoanController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeLoan.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max9999999,
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
