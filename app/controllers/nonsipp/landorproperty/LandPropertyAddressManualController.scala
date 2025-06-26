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

package controllers.nonsipp.landorproperty

import services.SaveService
import controllers.nonsipp.landorproperty.LandPropertyAddressManualController._
import utils.Country
import viewmodels.implicits._
import play.api.mvc._
import forms.mappings.Mappings
import viewmodels.models.MultipleQuestionsViewModel.{QuintupleQuestion, SextupleQuestion}
import controllers.actions._
import navigation.Navigator
import forms.MultipleQuestionFormProvider
import models._
import viewmodels.models._
import forms.mappings.errors.InputFormErrors
import config.RefinedTypes._
import controllers.PSRController
import views.html.MultipleQuestionView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined5000}
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import play.api.i18n.MessagesApi
import viewmodels.InputWidth
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandPropertyAddressManualController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: MultipleQuestionView,
  saveService: SaveService
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val internationalAddressFormWithCountries = internationalAddressForm(Country.countries)

  def onPageLoad(srn: Srn, index: Int, isUkAddress: Boolean, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val previousAnswer = request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index))

      if (isUkAddress) {
        val preparedForm = previousAnswer.fold(ukAddressForm)(address =>
          if (address.isManualAddress) ukAddressForm.fill(address.asUKAddressTuple) else ukAddressForm
        )
        Ok(
          view(
            preparedForm,
            viewModel(srn, index, ukPage(preparedForm), isUkAddress, mode)
          )
        )
      } else {
        val preparedForm = previousAnswer.fold(internationalAddressFormWithCountries)(address =>
          if (address.isManualAddress) {
            internationalAddressFormWithCountries.fill(address.asInternationalAddressTuple)
          } else {
            internationalAddressFormWithCountries
          }
        )
        Ok(
          view(
            preparedForm,
            viewModel(srn, index, internationalPage(preparedForm, Country.countries), isUkAddress, mode)
          )
        )
      }
    }

  def onSubmit(srn: Srn, index: Int, isUkAddress: Boolean, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      if (isUkAddress) {
        onSubmitUKAddress(srn, index, mode)
      } else {
        onSubmitInternationalAddress(srn, index, mode)
      }
    }

  private def onSubmitUKAddress(srn: Srn, index: Max5000, mode: Mode)(implicit
    request: DataRequest[?]
  ): Future[Result] =
    ukAddressForm
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(
                formWithErrors,
                viewModel(srn, index, ukPage(formWithErrors), isUkAddress = true, mode)
              )
            )
          ),
        value =>
          for {
            updatedAnswers <- Future
              .fromTry(
                request.userAnswers
                  .set(LandOrPropertyChosenAddressPage(srn, index), Address.fromManualUKAddress(value))
              )
            nextPage = navigator.nextPage(LandOrPropertyChosenAddressPage(srn, index), mode, updatedAnswers)
            updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
            _ <- saveService.save(updatedProgressAnswers)
          } yield Redirect(nextPage)
      )

  private def onSubmitInternationalAddress(srn: Srn, index: Max5000, mode: Mode)(implicit
    request: DataRequest[?]
  ): Future[Result] =
    internationalAddressFormWithCountries
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(
                formWithErrors,
                viewModel(srn, index, internationalPage(formWithErrors, Country.countries), isUkAddress = false, mode)
              )
            )
          ),
        value =>
          for {
            updatedAnswers <- Future
              .fromTry(
                request.userAnswers
                  .set(
                    LandOrPropertyChosenAddressPage(srn, index),
                    Address.fromManualInternationalAddress(value)
                  )
              )
            nextPage = navigator.nextPage(LandOrPropertyChosenAddressPage(srn, index), mode, updatedAnswers)
            updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
            _ <- saveService.save(updatedProgressAnswers)
          } yield Redirect(nextPage)
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
      "error.textarea.invalid",
      "landPropertyAddressManual.field1.error.max"
    )

  private val field2Errors: InputFormErrors =
    InputFormErrors.input(
      "",
      "error.textarea.invalid",
      "landPropertyAddressManual.field2.error.max"
    )

  private val field3Errors: InputFormErrors =
    InputFormErrors.input(
      "",
      "error.textarea.invalid",
      "landPropertyAddressManual.field3.error.max"
    )

  private val field4Errors: InputFormErrors =
    InputFormErrors.input(
      "landPropertyAddressManual.field4.error.required",
      "error.textarea.invalid",
      "landPropertyAddressManual.field4.error.max"
    )

  private val field5Errors: InputFormErrors =
    InputFormErrors.input(
      "",
      "error.textarea.invalid",
      "landPropertyAddressManual.field5.error.max"
    )

  private val postCodeFormErrors = InputFormErrors.postcode(
    "",
    "landOrPropertyPostcodeLookup.postcode.error.invalid.characters",
    "landOrPropertyPostcodeLookup.postcode.error.invalid.format"
  )

  val ukAddressForm: Form[ManualUKAddressAnswers] =
    MultipleQuestionFormProvider(
      Mappings.input(field1Errors),
      Mappings.optionalInput(field2Errors),
      Mappings.optionalInput(field3Errors),
      Mappings.input(field4Errors),
      Mappings.optionalPostcode(postCodeFormErrors)
    )

  val internationalAddressForm: List[SelectInput] => Form[ManualAddressAnswers] =
    countryOptions =>
      MultipleQuestionFormProvider(
        Mappings.input(field1Errors),
        Mappings.optionalInput(field2Errors),
        Mappings.optionalInput(field3Errors),
        Mappings.input(field4Errors),
        Mappings.optionalInput(field5Errors),
        Mappings.select(
          countryOptions,
          "landPropertyAddressManual.field6.error.required",
          "error.textarea.invalid"
        )
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

  def internationalPage(
    form: Form[ManualAddressAnswers],
    countryOptions: Seq[SelectInput]
  ): ManualAddressQuestions =
    SextupleQuestion(
      form,
      QuestionField.input("landPropertyAddressManual.field1.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field2.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field3.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field4.label").withWidth(InputWidth.TwoThirds),
      QuestionField.input("landPropertyAddressManual.field5.label"),
      QuestionField
        .select(label = "landPropertyAddressManual.field6.label", selectSource = countryOptions)
        .withWidth(InputWidth.TwoThirds)
    )
}
