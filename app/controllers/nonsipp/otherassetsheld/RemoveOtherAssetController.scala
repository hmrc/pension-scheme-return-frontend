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

package controllers.nonsipp.otherassetsheld

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld.{OtherAssetsPrePopulated, RemoveOtherAssetPage, WhatIsOtherAssetPage}
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveOtherAssetController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = RemoveOtherAssetController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      if (request.userAnswers.get(OtherAssetsPrePopulated(srn, index)).isDefined) {
        Redirect(controllers.routes.UnauthorisedController.onPageLoad())
      } else {
        (
          for {
            whatIsOtherAsset <- request.userAnswers.get(WhatIsOtherAssetPage(srn, index)).getOrRedirectToTaskList(srn)
          } yield {
            val preparedForm =
              request.userAnswers.fillForm(RemoveOtherAssetPage(srn, index), form)
            Ok(
              view(
                preparedForm,
                RemoveOtherAssetController
                  .viewModel(srn, index, whatIsOtherAsset, mode)
              )
            )
          }
        ).merge
      }
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.userAnswers.get(WhatIsOtherAssetPage(srn, index)).getOrRecoverJourney { whatIsOtherAsset =>
              Future.successful(
                BadRequest(
                  view(
                    errors,
                    RemoveOtherAssetController
                      .viewModel(srn, index, whatIsOtherAsset, mode)
                  )
                )
              )
            },
          value =>
            if (value) {
              for {
                removedUserAnswers <- Future.fromTry(request.userAnswers.remove(WhatIsOtherAssetPage(srn, index)))
                _ <- saveService.save(removedUserAnswers)
                redirectTo <- psrSubmissionService
                  .submitPsrDetailsWithUA(
                    srn,
                    removedUserAnswers,
                    fallbackCall =
                      controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onPageLoad(srn, 1, mode)
                  )(using
                    implicitly,
                    implicitly,
                    request = DataRequest(request.request, removedUserAnswers)
                  )
                  .map {
                    case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    case Some(_) =>
                      Redirect(
                        navigator.nextPage(
                          RemoveOtherAssetPage(srn, index),
                          mode,
                          removedUserAnswers
                        )
                      )
                  }
              } yield redirectTo
            } else {
              Future
                .successful(
                  Redirect(
                    navigator.nextPage(
                      RemoveOtherAssetPage(srn, index),
                      mode,
                      request.userAnswers
                    )
                  )
                )
            }
        )
    }

}

object RemoveOtherAssetController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "otherAssets.removeOtherAsset.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    whatIsOtherAsset: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "otherAssets.removeOtherAsset.title",
      Message("otherAssets.removeOtherAsset.heading", whatIsOtherAsset),
      routes.RemoveOtherAssetController.onSubmit(srn, index, mode)
    )
}
