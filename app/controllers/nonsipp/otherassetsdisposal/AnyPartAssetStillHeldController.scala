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

package controllers.nonsipp.otherassetsdisposal

import services.SaveService
import pages.nonsipp.otherassetsdisposal.{AnyPartAssetStillHeldPage, HowWasAssetDisposedOfPage}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{HowDisposed, Mode}
import play.api.i18n.MessagesApi
import play.api.data.Form
import viewmodels.implicits._
import controllers.nonsipp.otherassetsdisposal.AnyPartAssetStillHeldController._
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class AnyPartAssetStillHeldController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = AnyPartAssetStillHeldController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex), form)
      Ok(
        view(
          preparedForm,
          viewModel(
            srn,
            assetIndex,
            disposalIndex,
            request.schemeDetails.schemeName,
            mode
          )
        )
      )
    }

  def onSubmit(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  viewModel(
                    srn,
                    assetIndex,
                    disposalIndex,
                    request.schemeDetails.schemeName,
                    mode
                  )
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex), value)
              )
              _ <- saveService.save(updatedAnswers)
            } yield {
              updatedAnswers.get(HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex)) match {
                case Some(HowDisposed.Transferred) =>
                  Redirect(
                    navigator
                      .nextPage(
                        AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex),
                        mode,
                        updatedAnswers
                      )
                  )

                case Some(HowDisposed.Sold) =>
                  Redirect(
                    navigator
                      .nextPage(
                        AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex),
                        mode,
                        updatedAnswers
                      )
                  )

                case _ =>
                  Redirect(
                    navigator
                      .nextPage(
                        AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex),
                        mode,
                        updatedAnswers
                      )
                  )
              }

            }
        )
    }
}

object AnyPartAssetStillHeldController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "anyPartAssetStillHeld.error.required"
  )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    schemeName: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "anyPartAssetStillHeld.title",
      Message("anyPartAssetStillHeld.heading", schemeName),
      controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
        .onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
