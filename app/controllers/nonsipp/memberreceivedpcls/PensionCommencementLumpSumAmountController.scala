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

package controllers.nonsipp.memberreceivedpcls

import services.SaveService
import pages.nonsipp.memberdetails.MemberDetailsPage
import com.google.inject.Inject
import controllers.PSRController
import config.Constants.{maxPCLSAmount, minPosMoneyValue}
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.i18n.{I18nSupport, MessagesApi}
import forms.mappings.errors.MoneyFormErrors
import viewmodels.implicits._
import pages.nonsipp.memberreceivedpcls.PensionCommencementLumpSumAmountPage
import controllers.nonsipp.memberreceivedpcls.PensionCommencementLumpSumAmountController._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.Max300
import viewmodels.models.MultipleQuestionsViewModel.DoubleQuestion
import views.html.MultipleQuestionView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, FurtherDetailsViewModel, QuestionField}
import models.PensionCommencementLumpSum._
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Named

class PensionCommencementLumpSumAmountController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private def form: Form[(Money, Money)] =
    PensionCommencementLumpSumAmountController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
        val preparedForm =
          request.userAnswers
            .get(PensionCommencementLumpSumAmountPage(srn, index))
            .fold(form)(value => if (value.isZero) form else form.fill(value.tuple))

        Ok(view(viewModel(srn, index, memberName.fullName, mode, preparedForm)))
      }
    }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(BadRequest(view(viewModel(srn, index, memberName.fullName, mode, formWithErrors)))),
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(
                    request.userAnswers.transformAndSet(PensionCommencementLumpSumAmountPage(srn, index), value)
                  )
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(PensionCommencementLumpSumAmountPage(srn, index), mode, updatedAnswers)
              )
          )
      }
  }
}

object PensionCommencementLumpSumAmountController {
  def form(formProvider: MoneyFormProvider): Form[(Money, Money)] =
    formProvider(
      MoneyFormErrors(
        "pensionCommencementLumpSumAmount.received.error.required",
        "pensionCommencementLumpSumAmount.received.error.nonNumeric",
        maxPCLSAmount -> "pensionCommencementLumpSumAmount.received.error.tooLarge",
        minPosMoneyValue -> "pensionCommencementLumpSumAmount.received.error.tooSmall"
      ),
      MoneyFormErrors(
        "pensionCommencementLumpSumAmount.relevant.error.required",
        "pensionCommencementLumpSumAmount.relevant.error.nonNumeric",
        maxPCLSAmount -> "pensionCommencementLumpSumAmount.relevant.error.tooLarge",
        minPosMoneyValue -> "pensionCommencementLumpSumAmount.relevant.error.tooSmall"
      )
    )

  def viewModel(
    srn: Srn,
    index: Max300,
    fullName: String,
    mode: Mode,
    form: Form[(Money, Money)]
  ): FormPageViewModel[DoubleQuestion[Money]] =
    FormPageViewModel(
      title = "pensionCommencementLumpSumAmount.title",
      heading = Message("pensionCommencementLumpSumAmount.heading", fullName),
      page = DoubleQuestion(
        form,
        QuestionField.currency(Message("pensionCommencementLumpSumAmount.received", fullName)),
        QuestionField.currency(Message("pensionCommencementLumpSumAmount.relevant", fullName))
      ),
      Option(
        FurtherDetailsViewModel(
          Message("pensionCommencementLumpSumAmount.details.title"),
          ParagraphMessage("pensionCommencementLumpSumAmount.details")
        )
      ),
      routes.PensionCommencementLumpSumAmountController.onSubmit(srn, index, mode)
    )
}
