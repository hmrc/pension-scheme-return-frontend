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

import com.google.inject.Inject
import config.Constants.maxCashInBank
import config.Refined.Max300
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.memberreceivedpcls.PensionCommencementLumpSumAmountController._
import forms.MoneyFormProvider
import forms.mappings.errors.MoneyFormErrors
import models.SchemeId.Srn
import models.{Mode, Money}
import models.PensionCommencementLumpSum._
import navigation.Navigator
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.memberpayments.PensionCommencementLumpSumAmountPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.DoubleQuestion
import viewmodels.models.{FormPageViewModel, FurtherDetailsViewModel, QuestionField}
import views.html.MoneyView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}

class PensionCommencementLumpSumAmountController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  private def form: Form[(Money, Money)] =
    PensionCommencementLumpSumAmountController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney { memberName =>
        val preparedForm =
          request.userAnswers.fillForm(PensionCommencementLumpSumAmountPage(srn, index, mode), form)

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
                    request.userAnswers.transformAndSet(PensionCommencementLumpSumAmountPage(srn, index, mode), value)
                  )
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(
                navigator.nextPage(PensionCommencementLumpSumAmountPage(srn, index, mode), mode, updatedAnswers)
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
        maxCashInBank -> "pensionCommencementLumpSumAmount.received.error.tooLarge"
      ),
      MoneyFormErrors(
        "pensionCommencementLumpSumAmount.relevant.error.required",
        "pensionCommencementLumpSumAmount.relevant.error.nonNumeric",
        maxCashInBank -> "pensionCommencementLumpSumAmount.relevant.error.tooLarge"
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
      "pensionCommencementLumpSumAmount.title",
      Message("pensionCommencementLumpSumAmount.heading", fullName),
      page = DoubleQuestion(
        form,
        QuestionField.input(Message("pensionCommencementLumpSumAmount.received", fullName)),
        QuestionField.input(Message("pensionCommencementLumpSumAmount.relevant", fullName))
      ),
      Some(
        FurtherDetailsViewModel(
          Message("pensionCommencementLumpSumAmount.details.title"),
          ParagraphMessage("pensionCommencementLumpSumAmount.details")
        )
      ),
      routes.PensionCommencementLumpSumAmountController.onSubmit(srn, index, mode)
    )
}
