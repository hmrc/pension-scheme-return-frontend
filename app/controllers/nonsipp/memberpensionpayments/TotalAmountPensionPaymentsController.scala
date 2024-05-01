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

package controllers.nonsipp.memberpensionpayments

import controllers.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsController._
import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MembersDetailsPages
import viewmodels.implicits._
import play.api.mvc._
import controllers.PSRController
import config.Constants
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money, NameDOB}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.Refined.Max300
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import views.html.MoneyView
import models.SchemeId.Srn
import pages.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsPage
import controllers.actions._
import viewmodels.DisplayMessage.{Empty, Message}
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TotalAmountPensionPaymentsController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TotalAmountPensionPaymentsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val memberMap = request.userAnswers.map(MembersDetailsPages(srn))
      val maxIndex: Either[Result, Int] = memberMap.keys
        .map(_.toInt)
        .maxOption
        .map(Right(_))
        .getOrElse(Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

      val optionList: List[Option[NameDOB]] = maxIndex match {
        case Right(index) =>
          (0 to index).toList.map { index =>
            val memberOption = memberMap.get(index.toString)
            memberOption match {
              case Some(member) => Some(member)
              case None => None
            }
          }
        case Left(_) => List.empty
      }

      val preparedForm =
        request.userAnswers
          .get(TotalAmountPensionPaymentsPage(srn, index))
          .fold(form)(value => if (value.isZero) form else form.fill(value))

      optionList(index.value - 1)
        .map(_.fullName)
        .getOrRecoverJourney
        .map(memberName => Ok(view(viewModel(srn, index, memberName, preparedForm, mode))))
        .merge
    }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val memberMap = request.userAnswers.map(MembersDetailsPages(srn))
      val maxIndex: Either[Result, Int] = memberMap.keys
        .map(_.toInt)
        .maxOption
        .map(Right(_))
        .getOrElse(Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

      val optionList: List[Option[NameDOB]] = maxIndex match {
        case Right(index) =>
          (0 to index).toList.map { index =>
            val memberOption = memberMap.get(index.toString)
            memberOption match {
              case Some(member) => Some(member)
              case None => None
            }
          }
        case Left(_) => List.empty
      }

      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            Future.successful(
              optionList(index.value - 1)
                .map(_.fullName)
                .getOrRecoverJourney
                .map(
                  memberName =>
                    BadRequest(
                      view(viewModel(srn, index, memberName, formWithErrors, mode))
                    )
                )
                .merge
            )
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.transformAndSet(TotalAmountPensionPaymentsPage(srn, index), value)
                )
              _ <- saveService.save(updatedAnswers)
              submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(srn, updatedAnswers)
            } yield submissionResult.getOrRecoverJourney(
              _ =>
                Redirect(
                  navigator.nextPage(TotalAmountPensionPaymentsPage(srn, index), mode, updatedAnswers)
                )
            )
        )
    }
}

object TotalAmountPensionPaymentsController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "totalAmountPensionPayments.error.required",
      "totalAmountPensionPayments.error.invalid",
      (Constants.maxMoneyValue, "totalAmountPensionPayments.error.tooLarge"),
      (Constants.minPosMoneyValue, "totalAmountPensionPayments.error.tooSmall")
    )
  )

  def viewModel(
    srn: Srn,
    index: Max300,
    memberName: String,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      "totalAmountPensionPayments.title",
      Message("totalAmountPensionPayments.heading", memberName),
      SingleQuestion(
        form,
        QuestionField.input(Empty)
      ),
      controllers.nonsipp.memberpensionpayments.routes.TotalAmountPensionPaymentsController
        .onSubmit(srn, index, mode)
    )
}
