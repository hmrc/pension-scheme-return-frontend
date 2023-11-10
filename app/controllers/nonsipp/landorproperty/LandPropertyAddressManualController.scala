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

package controllers.nonsipp.landorproperty

import config.Refined._
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.landorproperty.LandPropertyAddressManualController._
import forms.MultipleQuestionFormProvider
import forms.mappings.Mappings
import forms.mappings.errors.InputFormErrors
import models.SchemeId.Srn
import models._
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SaveService
import viewmodels.InputWidth
import viewmodels.implicits._
import viewmodels.models.MultipleQuestionsViewModel.{QuintupleQuestion, SextupleQuestion}
import viewmodels.models._
import views.html.MultipleQuestionView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class LandPropertyAddressManualController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Max5000, isUkAddress: Boolean, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val previousAnswer = request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index))

      if (isUkAddress) {
        val preparedForm = previousAnswer.fold(ukAddressForm)(
          address => if (address.isManualAddress) ukAddressForm.fill(address.asUKAddressTuple) else ukAddressForm
        )
        Ok(view(viewModel(srn, index, ukPage(preparedForm), isUkAddress, mode)))
      } else {
        val preparedForm = previousAnswer.fold(internationalAddressForm)(
          address =>
            if (address.isManualAddress) internationalAddressForm.fill(address.asInternationalAddressTuple)
            else internationalAddressForm
        )
        Ok(view(viewModel(srn, index, internationalPage(preparedForm), isUkAddress, mode)))
      }
    }

  def onSubmit(srn: Srn, index: Max5000, isUkAddress: Boolean, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      if (isUkAddress) {
        onSubmitUKAddress(srn, index, mode)
      } else {
        onSubmitInternationalAddress(srn, index, mode)
      }
    }

  private def onSubmitUKAddress(srn: Srn, index: Max5000, mode: Mode)(
    implicit request: DataRequest[_]
  ): Future[Result] =
    ukAddressForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Future.successful(BadRequest(view(viewModel(srn, index, ukPage(formWithErrors), isUkAddress = true, mode))))
        },
        value =>
          for {
            updatedAnswers <- Future
              .fromTry(
                request.userAnswers
                  .set(LandOrPropertyChosenAddressPage(srn, index), Address.fromManualUKAddress(value))
              )
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(LandOrPropertyChosenAddressPage(srn, index), mode, updatedAnswers))
      )

  private def onSubmitInternationalAddress(srn: Srn, index: Max5000, mode: Mode)(
    implicit request: DataRequest[_]
  ): Future[Result] =
    internationalAddressForm
      .bindFromRequest()
      .fold(
        formWithErrors => {
          Future.successful(
            BadRequest(view(viewModel(srn, index, internationalPage(formWithErrors), isUkAddress = false, mode)))
          )
        },
        value =>
          for {
            updatedAnswers <- Future
              .fromTry(
                request.userAnswers
                  .set(LandOrPropertyChosenAddressPage(srn, index), Address.fromManualInternationalAddress(value))
              )
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(LandOrPropertyChosenAddressPage(srn, index), mode, updatedAnswers))
      )
}

object LandPropertyAddressManualController {

  // addressLine1, addressLine2, addressLine3, town, postcode, if not uk (county)
  private type ManualUKAddressAnswers = (String, Option[String], Option[String], String, Option[String])
  private type ManualAddressAnswers = (String, Option[String], Option[String], String, Option[String], String)
  private type ManualUKAddressQuestions =
    QuintupleQuestion[String, Option[String], Option[String], String, Option[String]]
  private type ManualAddressQuestions =
    SextupleQuestion[String, Option[String], Option[String], String, Option[String], String]

  private val field1Errors: InputFormErrors =
    InputFormErrors.input(
      "landPropertyAddressManual.field1.error.required",
      "landPropertyAddressManual.field1.error.invalid",
      "landPropertyAddressManual.field1.error.max"
    )

  private val field2Errors: InputFormErrors =
    InputFormErrors.input(
      "landPropertyAddressManual.field2.error.required",
      "landPropertyAddressManual.field2.error.invalid",
      "landPropertyAddressManual.field2.error.max"
    )

  private val field3Errors: InputFormErrors =
    InputFormErrors.input(
      "landPropertyAddressManual.field3.error.required",
      "landPropertyAddressManual.field3.error.invalid",
      "landPropertyAddressManual.field3.error.max"
    )

  private val field4Errors: InputFormErrors =
    InputFormErrors.input(
      "landPropertyAddressManual.field4.error.required",
      "landPropertyAddressManual.field4.error.invalid",
      "landPropertyAddressManual.field4.error.max"
    )

  private val field5Errors: InputFormErrors =
    InputFormErrors.input(
      "landPropertyAddressManual.field5.error.required",
      "landPropertyAddressManual.field5.error.invalid",
      "landPropertyAddressManual.field5.error.max"
    )

  private val field6Errors: InputFormErrors =
    InputFormErrors.input(
      "landPropertyAddressManual.field6.error.required",
      "landPropertyAddressManual.field6.error.invalid",
      "landPropertyAddressManual.field6.error.max"
    )

  val ukAddressForm: Form[ManualUKAddressAnswers] =
    MultipleQuestionFormProvider(
      Mappings.input(field1Errors),
      Mappings.optionalInput(field2Errors),
      Mappings.optionalInput(field3Errors),
      Mappings.input(field4Errors),
      Mappings.optionalInput(field5Errors)
    )

  private val internationalAddressForm: Form[ManualAddressAnswers] =
    MultipleQuestionFormProvider(
      Mappings.input(field1Errors),
      Mappings.optionalInput(field2Errors),
      Mappings.optionalInput(field3Errors),
      Mappings.input(field4Errors),
      Mappings.optionalInput(field5Errors),
      Mappings.input(field6Errors)
    )

  def viewModel[A](
    srn: Srn,
    index: Max5000,
    page: A,
    isUkAddress: Boolean,
    mode: Mode
  ): FormPageViewModel[A] = FormPageViewModel[A](
    title = "landPropertyAddressManual.title",
    heading = "landPropertyAddressManual.heading",
    description = None,
    page = page,
    refresh = None,
    buttonText = "site.saveAndContinue",
    details = None,
    onSubmit = routes.LandPropertyAddressManualController.onSubmit(srn, index, isUkAddress, mode)
  )

  def ukPage(form: Form[ManualUKAddressAnswers]): ManualUKAddressQuestions =
    QuintupleQuestion(
      form,
      QuestionField.input("landPropertyAddressManual.field1.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field2.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field3.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field4.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field5.label.uk")
    )

  def internationalPage(form: Form[ManualAddressAnswers]): ManualAddressQuestions =
    SextupleQuestion(
      form,
      QuestionField.input("landPropertyAddressManual.field1.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field2.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field3.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field4.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field5.label"),
      QuestionField.input("landPropertyAddressManual.field6.label").withWidth(InputWidth.TwoThirds)
    )

}
