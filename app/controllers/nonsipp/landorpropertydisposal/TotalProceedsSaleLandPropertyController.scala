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

package controllers.nonsipp.landorpropertydisposal

import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.nonsipp.landorpropertydisposal.TotalProceedsSaleLandPropertyController._
import viewmodels.models.MultipleQuestionsViewModel.SingleQuestion
import config.Constants
import pages.nonsipp.landorpropertydisposal.TotalProceedsSaleLandPropertyPage
import controllers.actions._
import navigation.Navigator
import forms.MoneyFormProvider
import models.{Mode, Money}
import play.api.i18n.MessagesApi
import play.api.data.Form
import forms.mappings.errors.MoneyFormErrors
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.MoneyView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
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

  def onPageLoad(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
        address =>
          val preparedForm = request.userAnswers
            .fillForm(TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, disposalIndex), form)
          Ok(view(preparedForm, viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, form, mode)))
      }
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
              address =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, form, mode)
                    )
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
              nextPage = navigator.nextPage(
                TotalProceedsSaleLandPropertyPage(srn, landOrPropertyIndex, disposalIndex),
                mode,
                updatedAnswers
              )
              updatedProgressAnswers <- saveProgress(srn, landOrPropertyIndex, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object TotalProceedsSaleLandPropertyController {
  def form(formProvider: MoneyFormProvider): Form[Money] = formProvider(
    MoneyFormErrors(
      "totalProceedsSaleLandProperty.error.required",
      "totalProceedsSaleLandProperty.error.invalid",
      (Constants.maxMoneyValue, "totalProceedsSaleLandProperty.error.tooLarge"),
      (Constants.minPosMoneyValue, "totalProceedsSaleLandProperty.error.tooSmall")
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
      description = None,
      refresh = None,
      buttonText = Message("site.saveAndContinue"),
      details = None,
      page = SingleQuestion(
        form,
        QuestionField.currency(
          Empty,
          Some(
            ParagraphMessage("totalProceedsSaleLandProperty.paragraph") ++
              ListMessage(
                ListType.Bullet,
                "totalProceedsSaleLandProperty.list1",
                "totalProceedsSaleLandProperty.list2"
              )
          )
        )
      ),
      onSubmit = routes.TotalProceedsSaleLandPropertyController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
