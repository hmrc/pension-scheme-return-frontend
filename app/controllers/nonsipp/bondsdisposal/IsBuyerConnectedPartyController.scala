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

package controllers.nonsipp.bondsdisposal

import services.SaveService
import controllers.nonsipp.bondsdisposal.IsBuyerConnectedPartyController._
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.data.Form
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import pages.nonsipp.bondsdisposal._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IsBuyerConnectedPartyController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = IsBuyerConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.get(IsBuyerConnectedPartyPage(srn, index, disposalIndex)).fold(form)(form.fill)
      request.userAnswers.get(BuyerNamePage(srn, index, disposalIndex)).getOrRecoverJourney { buyerName =>
        Ok(view(preparedForm, viewModel(srn, index, disposalIndex, buyerName, mode)))
      }
    }

  def onSubmit(srn: Srn, index: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers.get(BuyerNamePage(srn, index, disposalIndex)).getOrRecoverJourney { buyerName =>
              Future
                .successful(BadRequest(view(formWithErrors, viewModel(srn, index, disposalIndex, buyerName, mode))))
            },
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(IsBuyerConnectedPartyPage(srn, index, disposalIndex), value))
              nextPage = navigator.nextPage(IsBuyerConnectedPartyPage(srn, index, disposalIndex), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object IsBuyerConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "bondsDisposal.isBuyerConnectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    disposalIndex: Max50,
    buyerName: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "bondsDisposal.isBuyerConnectedParty.title",
      Message("bondsDisposal.isBuyerConnectedParty.heading", buyerName),
      controllers.nonsipp.bondsdisposal.routes.IsBuyerConnectedPartyController
        .onSubmit(srn, index, disposalIndex, mode)
    )
}
