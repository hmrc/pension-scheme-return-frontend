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

import play.api.mvc.Call
import pages.nonsipp.otherassetsheld._
import views.html.ContentTablePageView
import utils.IntUtils.toInt
import eu.timepit.refined.refineMV
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec
import models.NormalMode
import models.SchemeHoldAsset.Transfer
import controllers.nonsipp.otherassetsheld.OtherAssetsCheckAndUpdateController.viewModel
import eu.timepit.refined.api.Refined

class OtherAssetsCheckAndUpdateControllerSpec extends ControllerBaseSpec {

  private val index: Refined[Int, OneTo5000] = refineMV[OneTo5000](1)

  private def onPageLoad: Call = routes.OtherAssetsCheckAndUpdateController.onPageLoad(srn, index)
  private def onSubmit: Call = routes.OtherAssetsCheckAndUpdateController.onSubmit(srn, index)

  private val completedUserAnswers = defaultUserAnswers
    .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Transfer)
    .unsafeSet(CostOfOtherAssetPage(srn, index), money)

  "OtherAssetsCheckAndUpdateController" - {

    act.like(
      renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
        injected[ContentTablePageView].apply(
          viewModel(
            srn = srn,
            index = index,
            descriptionOfAsset = otherAssetDescription,
            whyAssetIsHeld = Transfer,
            totalCostOfAsset = money
          )
        )
      }.withName(s"render correct view")
    )

    act.like(
      redirectToPage(onSubmit, routes.IsAssetTangibleMoveablePropertyController.onPageLoad(srn, index, NormalMode))
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .updateName("onPageLoad" + _)
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .updateName("onSubmit" + _)
    )
  }
}
