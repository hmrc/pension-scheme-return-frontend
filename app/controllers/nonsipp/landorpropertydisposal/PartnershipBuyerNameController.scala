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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import pages.nonsipp.landorpropertydisposal.PartnershipBuyerNamePage
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.TextInputView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._
import controllers.nonsipp.landorpropertydisposal.PartnershipBuyerNameController._
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PartnershipBuyerNameController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = PartnershipBuyerNameController.form(formProvider)

  def onPageLoad(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex), form)
      Ok(view(preparedForm, viewModel(srn, landOrPropertyIndex, disposalIndex, mode)))
    }
  def onSubmit(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(BadRequest(view(formWithErrors, viewModel(srn, landOrPropertyIndex, disposalIndex, mode)))),
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex), value)
              )
              nextPage = navigator
                .nextPage(PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, landOrPropertyIndex, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object PartnershipBuyerNameController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "partnershipBuyerName.error.required",
    "partnershipBuyerName.error.tooLong",
    "error.textarea.invalid"
  )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[TextInputViewModel] = FormPageViewModel(
    Message("partnershipBuyerName.title"),
    Message("partnershipBuyerName.heading"),
    TextInputViewModel(isFixedLength = true),
    routes.PartnershipBuyerNameController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
  )
}
