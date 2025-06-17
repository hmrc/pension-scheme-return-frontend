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
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.nonsipp.landorpropertydisposal.OtherBuyerDetailsController._
import pages.nonsipp.landorpropertydisposal.OtherBuyerDetailsPage
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.RecipientDetailsFormProvider
import models.{Mode, RecipientDetails}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.data.Form
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.RecipientDetailsView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RecipientDetailsViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class OtherBuyerDetailsController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: RecipientDetailsFormProvider,
  view: RecipientDetailsView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(
    srn: Srn,
    landOrPropertyIndex: Int,
    disposalIndex: Int,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val address: String =
        request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).get.addressLine1
      val form = OtherBuyerDetailsController.form(formProvider)
      Ok(
        view(
          form.fromUserAnswers(OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex)),
          viewModel(srn, landOrPropertyIndex, disposalIndex, mode, address)
        )
      )
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val address: String =
        request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).get.addressLine1
      val form = OtherBuyerDetailsController.form(formProvider)
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, viewModel(srn, landOrPropertyIndex, disposalIndex, mode, address)))
            ),
          answer =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers.set(OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex), answer)
                )
              nextPage = navigator
                .nextPage(OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, landOrPropertyIndex, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object OtherBuyerDetailsController {
  def form(formProvider: RecipientDetailsFormProvider): Form[RecipientDetails] = formProvider(
    "otherBuyerDetails.name.error.required",
    "error.textarea.invalid",
    "otherBuyerDetails.name.error.length",
    "otherBuyerDetails.description.error.required",
    "error.textarea.invalid",
    "otherBuyerDetails.description.error.length"
  )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode,
    address: String
  ): FormPageViewModel[RecipientDetailsViewModel] =
    FormPageViewModel(
      Message("otherBuyerDetails.title"),
      Message("otherBuyerDetails.heading", address),
      RecipientDetailsViewModel(
        Message("otherBuyerDetails.name"),
        Message("otherBuyerDetails.description")
      ),
      routes.OtherBuyerDetailsController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
