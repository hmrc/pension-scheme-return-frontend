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

package controllers.nonsipp.landorpropertydisposal

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.nonsipp.landorpropertydisposal.TotalProceedsSaleLandPropertyController._
import controllers.PSRController
import config.Constants
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import pages.nonsipp.landorpropertydisposal.TotalProceedsSaleLandPropertyPage
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.Refined.{Max50, Max5000}
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import views.html.MoneyView
import models.SchemeId.Srn
import viewmodels.DisplayMessage._
import viewmodels.models.{FormPageViewModel, QuestionField}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class TotalProceedsSaleLandPropertyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: MoneyFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: MoneyView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = TotalProceedsSaleLandPropertyController.form(formProvider)

  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
        address =>
          val preparedForm = request.userAnswers
            .fillForm(TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, disposalIndex), form)
          Ok(view(viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, preparedForm, mode)))
      }
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
              address =>
                Future.successful(
                  BadRequest(
                    view(viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, formWithErrors, mode))
                  )
                )
            }
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .transformAndSet(TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, disposalIndex), value)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, disposalIndex),
                mode,
                updatedAnswers
              )
            )
        )
    }
}

object TotalProceedsSaleLandPropertyController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "totalProceedsSaleLandProperty.error.required",
      "totalProceedsSaleLandProperty.error.invalid",
      (Constants.maxMoneyValue, "totalProceedsSaleLandProperty.error.tooLarge")
    )
  )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    addressLine1: String,
    form: Form[Money],
    mode: Mode
  ): FormPageViewModel[SingleQuestion[Money]] =
    FormPageViewModel(
      title = "totalProceedsSaleLandProperty.title",
      heading = Message("totalProceedsSaleLandProperty.heading", addressLine1),
      description = Some(
        ParagraphMessage("totalProceedsSaleLandProperty.paragraph") ++
          ListMessage(
            ListType.Bullet,
            "totalProceedsSaleLandProperty.list1",
            "totalProceedsSaleLandProperty.list2"
          )
      ),
      refresh = None,
      buttonText = Message("site.saveAndContinue"),
      details = None,
      page = SingleQuestion(
        form,
        QuestionField.input(Empty)
      ),
      onSubmit = routes.TotalProceedsSaleLandPropertyController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
