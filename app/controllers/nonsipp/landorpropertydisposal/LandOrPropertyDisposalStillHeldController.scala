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

import controllers.actions._
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import controllers.PSRController
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YesNoPageView
import services.SaveService
import LandOrPropertyDisposalStillHeldController._
import viewmodels.implicits._
import pages.nonsipp.landorpropertydisposal.LandOrPropertyDisposalStillHeldPage

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import config.Refined.{Max50, Max5000}
import pages.nonsipp.landorproperty.LandOrPropertyAddressLookupPage
import viewmodels.DisplayMessage.Message

class LandOrPropertyDisposalStillHeldController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandOrPropertyDisposalStillHeldController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm =
          request.userAnswers.fillForm(LandOrPropertyDisposalStillHeldPage(srn, index, disposalIndex), form)
        Ok(
          view(
            preparedForm,
            viewModel(srn, address.addressLine1, request.schemeDetails.schemeName, index, disposalIndex, mode)
          )
        )
      }
    }

  def onSubmit(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney { address =>
              Future.successful(
                BadRequest(
                  view(
                    formWithErrors,
                    viewModel(srn, address.addressLine1, request.schemeDetails.schemeName, index, disposalIndex, mode)
                  )
                )
              )
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(LandOrPropertyDisposalStillHeldPage(srn, index, disposalIndex), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(LandOrPropertyDisposalStillHeldPage(srn, index, disposalIndex), mode, updatedAnswers)
            )
        )
    }
}

object LandOrPropertyDisposalStillHeldController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "landOrPropertyDisposalStillHeld.error.required"
  )

  def viewModel(
    srn: Srn,
    addressLine1: String,
    schemeName: String,
    index: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "landOrPropertyDisposalStillHeld.title",
      Message("landOrPropertyDisposalStillHeld.heading", addressLine1, schemeName),
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalStillHeldController
        .onSubmit(srn, index, disposalIndex, mode)
    )
}
