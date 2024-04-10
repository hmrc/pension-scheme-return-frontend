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
import viewmodels.implicits._
import play.api.mvc._
import forms.mappings.Mappings
import config.Refined.Max5000
import controllers.PSRController
import controllers.nonsipp.landorproperty.LandRegistryTitleNumberController._
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, LandRegistryTitleNumberPage}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models._
import play.api.i18n._
import play.api.data.Form
import forms.mappings.errors._
import views.html.ConditionalYesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandRegistryTitleNumberController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ConditionalYesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandRegistryTitleNumberController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm = request.userAnswers.fillForm(LandRegistryTitleNumberPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, address.addressLine1, mode)))
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => {
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, address.addressLine1, mode))))
            }
          },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(LandRegistryTitleNumberPage(srn, index), ConditionalYesNo(value)))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(LandRegistryTitleNumberPage(srn, index), mode, updatedAnswers))
        )
  }
}

object LandRegistryTitleNumberController {

  private val noFormErrors = InputFormErrors.textArea(
    "landRegistryTitleNumber.no.conditional.error.required",
    "landRegistryTitleNumber.no.conditional.error.invalid",
    "landRegistryTitleNumber.no.conditional.error.length"
  )

  private val yesFormErrors = InputFormErrors.input(
    "landRegistryTitleNumber.yes.conditional.error.required",
    "landRegistryTitleNumber.yes.conditional.error.invalid",
    "landRegistryTitleNumber.yes.conditional.error.length"
  )

  def form(formProvider: YesNoPageFormProvider): Form[Either[String, String]] = formProvider.conditional(
    "landRegistryTitleNumber.error.required",
    mappingNo = Mappings.input(noFormErrors),
    mappingYes = Mappings.input(yesFormErrors)
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    addressLine1: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      title = "landRegistryTitleNumber.title",
      heading = Message("landRegistryTitleNumber.heading", addressLine1),
      ConditionalYesNoPageViewModel(
        hint = Some("landRegistryTitleNumber.hint"),
        yes = YesNoViewModel
          .Conditional("landRegistryTitleNumber.yes.conditional", FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("landRegistryTitleNumber.no.conditional", addressLine1), FieldType.Textarea)
      ),
      controllers.nonsipp.landorproperty.routes.LandRegistryTitleNumberController.onSubmit(srn, index, mode)
    )
}
