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

import views.html.ContentTablePageView
import models.NormalMode
import eu.timepit.refined.api.Refined
import config.RefinedTypes.OneTo5000
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import utils.IntUtils.given
import pages.nonsipp.landorproperty.{
  LandOrPropertyChosenAddressPage,
  LandOrPropertyTotalCostPage,
  WhyDoesSchemeHoldLandPropertyPage
}

class LandOrPropertyCheckAndUpdateControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index: Refined[Int, OneTo5000] = 1

  private def onPageLoad = routes.LandOrPropertyCheckAndUpdateController.onPageLoad(srn, index)
  private def onSubmit = routes.LandOrPropertyCheckAndUpdateController.onSubmit(srn, index)

  private val schemeHoldLandProperty = schemeHoldLandPropertyGen.sample.value

  private val completedUserAnswers = defaultUserAnswers
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), schemeHoldLandProperty)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index), money)

  "LandOrPropertyCheckAndUpdateController" - {

    act.like(
      renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
        injected[ContentTablePageView].apply(
          LandOrPropertyCheckAndUpdateController.viewModel(
            srn = srn,
            index = index,
            address = address,
            whyLandIsHeld = schemeHoldLandProperty,
            totalCost = money
          )
        )
      }.withName(s"render correct view")
    )

    act.like(
      redirectToPage(onSubmit, routes.IsLandOrPropertyResidentialController.onPageLoad(srn, index, NormalMode))
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
