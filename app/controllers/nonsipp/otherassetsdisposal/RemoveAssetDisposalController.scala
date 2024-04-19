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

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.otherassetsdisposal._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld.WhatIsOtherAssetPage
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.NormalMode
import play.api.i18n.MessagesApi
import play.api.data.Form
import viewmodels.implicits._
import controllers.nonsipp.otherassetsdisposal.RemoveAssetDisposalController._
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveAssetDisposalController @Inject()(
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

  private val form = RemoveAssetDisposalController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Max5000, disposalIndex: Max50): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(WhatIsOtherAssetPage(srn, assetIndex)).getOrRecoverJourney { otherAsset =>
        Ok(view(form, viewModel(srn, assetIndex, disposalIndex, otherAsset)))
      }
    }

  def onSubmit(srn: Srn, assetIndex: Max5000, disposalIndex: Max50): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.userAnswers.get(WhatIsOtherAssetPage(srn, assetIndex)).getOrRecoverJourney { otherAsset =>
              Future.successful(BadRequest(view(errors, viewModel(srn, assetIndex, disposalIndex, otherAsset))))
            },
          removeDisposal =>
            if (removeDisposal) {
              for {
                removedUserAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .remove(HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex))
                      .remove(OtherAssetsDisposalCompleted(srn))
                      .remove(OtherAssetsDisposalProgress(srn, assetIndex, disposalIndex))
                  )
                _ <- saveService.save(removedUserAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(srn, removedUserAnswers)
              } yield submissionResult.getOrRecoverJourney(
                _ => Redirect(navigator.nextPage(RemoveAssetDisposalPage(srn), NormalMode, removedUserAnswers))
              )
            } else {
              Future.successful(
                Redirect(navigator.nextPage(RemoveAssetDisposalPage(srn), NormalMode, request.userAnswers))
              )
            }
        )
    }

}

object RemoveAssetDisposalController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeAssetDisposal.error.required"
  )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    otherAsset: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "removeAssetDisposal.title",
      Message("removeAssetDisposal.heading", otherAsset),
      routes.RemoveAssetDisposalController.onSubmit(srn, assetIndex, disposalIndex)
    )
}
