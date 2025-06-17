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

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.otherassetsheld.OtherAssetsCheckAndUpdateController._
import pages.nonsipp.otherassetsheld.{CostOfOtherAssetPage, WhatIsOtherAssetPage, WhyDoesSchemeHoldAssetsPage}
import com.google.inject.Inject
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.ContentTablePageView
import models.SchemeId.Srn
import models.{Money, NormalMode, SchemeHoldAsset}
import models.SchemeHoldAsset.{Acquisition, Contribution, Transfer}
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{ListMessage, Message, ParagraphMessage}
import viewmodels.models.{ContentTablePageViewModel, FormPageViewModel}

class OtherAssetsCheckAndUpdateController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentTablePageView
) extends PSRController {

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    (
      for {
        descriptionOfAsset <- request.userAnswers.get(WhatIsOtherAssetPage(srn, index)).getOrRecoverJourney
        whyAssetIsHeld <- request.userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, index)).getOrRecoverJourney
        totalCostOfAsset <- request.userAnswers.get(CostOfOtherAssetPage(srn, index)).getOrRecoverJourney
      } yield Ok(
        view(
          viewModel(
            srn,
            index,
            descriptionOfAsset,
            whyAssetIsHeld,
            totalCostOfAsset
          )
        )
      )
    ).merge
  }

  def onSubmit(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { _ =>
    Redirect(routes.IsAssetTangibleMoveablePropertyController.onPageLoad(srn, index, NormalMode))
  }
}

object OtherAssetsCheckAndUpdateController {

  def viewModel(
    srn: Srn,
    index: Max5000,
    descriptionOfAsset: String,
    whyAssetIsHeld: SchemeHoldAsset,
    totalCostOfAsset: Money
  ): FormPageViewModel[ContentTablePageViewModel] = {

    val whyAssetIsHeldString = whyAssetIsHeld match {
      case Acquisition => "otherAssetsCheckAndUpdate.Acquisition"
      case Contribution => "otherAssetsCheckAndUpdate.Contribution"
      case Transfer => "otherAssetsCheckAndUpdate.Transfer"
    }

    val rows: List[(DisplayMessage, DisplayMessage)] = List(
      Message("otherAssetsCheckAndUpdate.table.one") -> Message(descriptionOfAsset),
      Message("otherAssetsCheckAndUpdate.table.two") -> Message(whyAssetIsHeldString),
      Message("otherAssetsCheckAndUpdate.table.three") -> Message(totalCostOfAsset.displayAs)
    )

    FormPageViewModel(
      mode = NormalMode,
      title = Message("otherAssetsCheckAndUpdate.title"),
      heading = Message("otherAssetsCheckAndUpdate.heading"),
      description = None,
      page = ContentTablePageViewModel(
        inset = None,
        beforeTable = Some(ParagraphMessage("otherAssetsCheckAndUpdate.paragraph")),
        afterTable = Some(
          ParagraphMessage("otherAssetsCheckAndUpdate.bullet.paragraph") ++ ListMessage
            .Bullet(
              Message("otherAssetsCheckAndUpdate.bullet.one"),
              Message("otherAssetsCheckAndUpdate.bullet.two")
            )
        ),
        rows = rows
      ),
      refresh = None,
      buttonText = Message("otherAssetsCheckAndUpdate.button"),
      details = None,
      onSubmit = routes.OtherAssetsCheckAndUpdateController.onSubmit(srn, index),
      optViewOnlyDetails = None
    )
  }
}
