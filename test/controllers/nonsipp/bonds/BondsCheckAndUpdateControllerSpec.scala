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

import pages.nonsipp.bonds._
import utils.nonsipp.summary.BondsCheckAnswersUtils
import views.html.PrePopCheckYourAnswersView
import utils.IntUtils.given
import config.Constants.incomplete
import models.NormalMode
import viewmodels.models.SectionJourneyStatus
import eu.timepit.refined.api.Refined
import config.RefinedTypes.OneTo5000
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import models.SchemeHoldBond.Acquisition

class BondsCheckAndUpdateControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index: Refined[Int, OneTo5000] = 1

  private def onPageLoad = routes.BondsCheckAndUpdateController.onPageLoad(srn, index)
  private def onSubmit = routes.BondsCheckAndUpdateController.onSubmit(srn, index)

  private val prePodDataMissingUserAnswers = defaultUserAnswers
    .unsafeSet(NameOfBondsPage(srn, index), otherName)
    .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), Acquisition)
    .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index), localDate)
    .unsafeSet(CostOfBondsPage(srn, index), money)
    .unsafeSet(BondsFromConnectedPartyPage(srn, index), true)
    .unsafeSet(AreBondsUnregulatedPage(srn, index), true)
    .unsafeSet(BondsProgress(srn, index), SectionJourneyStatus.Completed)

  private val completedUserAnswers = prePodDataMissingUserAnswers
    .unsafeSet(IncomeFromBondsPage(srn, index), money)

  "BondsCheckAndUpdateController" - {
    act.like(
      renderView(onPageLoad, prePodDataMissingUserAnswers) { implicit app => implicit request =>
        injected[PrePopCheckYourAnswersView].apply(
          BondsCheckAndUpdateController.viewModel(
            srn,
            index,
            BondsCheckAnswersUtils
              .viewModel(
                srn,
                index,
                schemeName,
                otherName,
                Acquisition,
                Some(localDate),
                money,
                Some(true),
                areBondsUnregulated = true,
                Left(incomplete),
                NormalMode,
                viewOnlyUpdated = true
              )
              .page
              .sections
          )
        )
      }.withName(s"render correct view when prePopulation data missing")
    )

    act.like(
      renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
        injected[PrePopCheckYourAnswersView].apply(
          BondsCheckAndUpdateController.viewModel(
            srn,
            index,
            BondsCheckAnswersUtils
              .viewModel(
                srn,
                index,
                schemeName,
                otherName,
                Acquisition,
                Some(localDate),
                money,
                Some(true),
                areBondsUnregulated = true,
                Right(money),
                NormalMode,
                viewOnlyUpdated = true
              )
              .page
              .sections
          )
        )
      }.withName(s"render correct view when data complete")
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
