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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{Mode, UserAnswers}
import play.api.i18n.MessagesApi
import play.api.data.Form
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import controllers.nonsipp.landorproperty.IsLandPropertyLeasedController._
import utils.IntUtils.{toInt, toRefined5000}
import pages.nonsipp.landorproperty.{IsLandPropertyLeasedPage, LandOrPropertyChosenAddressPage, LandPropertyInUKPage}
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IsLandPropertyLeasedController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = IsLandPropertyLeasedController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm = request.userAnswers.fillForm(IsLandPropertyLeasedPage(srn, index), form)
        Ok(view(preparedForm, viewModel(srn, index, address.addressLine1, mode, request.userAnswers)))
      }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
              Future.successful(
                BadRequest(view(formWithErrors, viewModel(srn, index, address.addressLine1, mode, request.userAnswers)))
              )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(IsLandPropertyLeasedPage(srn, index), value))
              nextPage = navigator.nextPage(IsLandPropertyLeasedPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
  }
}

object IsLandPropertyLeasedController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "isLandPropertyLeased.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    addressLine1: String,
    mode: Mode,
    userAnswers: UserAnswers
  ): FormPageViewModel[YesNoPageViewModel] = {

    val isInUK = userAnswers.get(LandPropertyInUKPage(srn, index))

    val hintMessage = isInUK match {
      case Some(true) => Some(Message("IsLandPropertyLeased.hint.uk"))
      case Some(false) => Some(Message(""))
      case None => None
    }

    FormPageViewModel(
      "IsLandPropertyLeased.title",
      Message("IsLandPropertyLeased.heading", addressLine1),
      YesNoPageViewModel(hint = hintMessage),
      routes.IsLandPropertyLeasedController.onSubmit(srn, index, mode)
    )
  }

}
