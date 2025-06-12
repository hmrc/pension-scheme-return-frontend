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

package controllers.nonsipp.bonds

import pages.nonsipp.bonds.{CostOfBondsPage, NameOfBondsPage, WhyDoesSchemeHoldBondsPage}
import views.html.ContentTablePageView
import utils.IntUtils.given
import models.NormalMode
import controllers.nonsipp.bonds.BondsCheckAndUpdateController._
import eu.timepit.refined.api.Refined
import config.RefinedTypes.OneTo5000
import controllers.{ControllerBaseSpec, ControllerBehaviours}

class BondsCheckAndUpdateControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index: Refined[Int, OneTo5000] = 1

  private def onPageLoad = routes.BondsCheckAndUpdateController.onPageLoad(srn, index)
  private def onSubmit = routes.BondsCheckAndUpdateController.onSubmit(srn, index)

  private val completedUserAnswers = defaultUserAnswers
    .unsafeSet(NameOfBondsPage(srn, index), nameOfBonds)
    .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), schemeHoldBonds)
    .unsafeSet(CostOfBondsPage(srn, index), money)

  "BondsCheckAndUpdateController" - {

    act.like(
      renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
        injected[ContentTablePageView].apply(
          viewModel(
            srn = srn,
            index = index,
            nameOfBonds = nameOfBonds,
            acquisitionType = schemeHoldBonds,
            costOfBonds = money
          )
        )
      }.withName(s"render correct view")
    )

    act.like(
      redirectToPage(onSubmit, routes.IncomeFromBondsController.onPageLoad(srn, index, NormalMode))
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
