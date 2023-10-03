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

import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyStillHeldController._
import forms.YesNoPageFormProvider
import models.{HowDisposed, Mode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.landorproperty.LandOrPropertyAddressLookupPage
import pages.nonsipp.landorpropertydisposal.{
  HowWasPropertyDisposedOfPage,
  LandOrPropertyStillHeldPage,
  LandPropertyDisposalCYAPage
}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

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

  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers
        .get(LandOrPropertyAddressLookupPage(srn, landOrPropertyIndex))
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

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            request.userAnswers
              .get(LandOrPropertyAddressLookupPage(srn, landOrPropertyIndex))
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
              _ <- saveService.save(updatedAnswers)
            } yield {
              val x = updatedAnswers.get(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex))

              x match {
                case Some(HowDisposed.Transferred) =>
                  Redirect(
                    navigator
                      .nextPage(
                        LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex),
                        mode,
                        updatedAnswers
                      )
                  )

                case Some(HowDisposed.Sold) =>
                  Redirect(
                    navigator
                      .nextPage(
                        LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex),
                        mode,
                        updatedAnswers
                      )
                  )

                case _ =>
                  Redirect(
                    navigator
                      .nextPage(
                        LandOrPropertyStillHeldPage(srn, landOrPropertyIndex, disposalIndex),
                        mode,
                        updatedAnswers
                      )
                  )
              }

            }
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
