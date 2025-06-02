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

import controllers.nonsipp.landorpropertydisposal.LandOrPropertyStillHeldController._
import services.SaveService
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.landorpropertydisposal.LandOrPropertyStillHeldPage
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandOrPropertyStillHeldController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandOrPropertyStillHeldController.form(formProvider)

  def onPageLoad(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers
        .get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex))
        .getOrRecoverJourney { address =>
          val preparedForm =
            request.userAnswers.fillForm(LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex), form)
          Ok(
            view(
              preparedForm,
              viewModel(
                srn,
                landOrPropertyIndex,
                disposalIndex,
                address.addressLine1,
                request.schemeDetails.schemeName,
                mode
              )
            )
          )
        }
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers
              .get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex))
              .getOrRecoverJourney { address =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      viewModel(
                        srn,
                        landOrPropertyIndex,
                        disposalIndex,
                        address.addressLine1,
                        request.schemeDetails.schemeName,
                        mode
                      )
                    )
                  )
                )
              },
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex), value)
              )
              nextPage = navigator
                .nextPage(
                  LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex),
                  mode,
                  updatedAnswers
                )
              updatedProgressAnswers <- saveProgress(srn, landOrPropertyIndex, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
        )
    }
}

object LandOrPropertyStillHeldController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "landOrPropertyStillHeld.error.required"
  )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    addressLine1: String,
    schemeName: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "landOrPropertyStillHeld.title",
      Message("landOrPropertyStillHeld.heading", addressLine1, schemeName),
      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyStillHeldController
        .onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
