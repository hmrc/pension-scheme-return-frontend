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

package controllers.nonsipp.bondsdisposal

import pages.nonsipp.bonds._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.ListRadiosView
import utils.IntUtils.given
import controllers.nonsipp.bondsdisposal.BondsDisposalListController._
import forms.RadioListFormProvider
import models.{HowDisposed, NormalMode, SchemeHoldBond}
import pages.nonsipp.bondsdisposal.{BondsDisposalProgress, HowWereBondsDisposedOfPage}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}

class BondsDisposalListControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val page = 1
  private val indexOne = 1
  private val indexTwo = 2
  private val indexThree = 3

  private lazy val onPageLoad = routes.BondsDisposalListController.onPageLoad(srn, page, NormalMode)
  private lazy val onSubmit = routes.BondsDisposalListController.onSubmit(srn, page, NormalMode)

  private val userAnswers =
    defaultUserAnswers
      .unsafeSet(IncomeFromBondsPage(srn, indexOne), money)
      .unsafeSet(NameOfBondsPage(srn, indexOne), "bonds one")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, indexOne), SchemeHoldBond.Transfer)
      .unsafeSet(CostOfBondsPage(srn, indexOne), money)
      .unsafeSet(BondsCompleted(srn, indexOne), SectionCompleted)
      .unsafeSet(IncomeFromBondsPage(srn, indexTwo), money)
      .unsafeSet(NameOfBondsPage(srn, indexTwo), "bonds two")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, indexTwo), SchemeHoldBond.Acquisition)
      .unsafeSet(CostOfBondsPage(srn, indexTwo), money)
      .unsafeSet(BondsCompleted(srn, indexTwo), SectionCompleted)
      .unsafeSet(IncomeFromBondsPage(srn, indexThree), money)
      .unsafeSet(NameOfBondsPage(srn, indexThree), "bonds three")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, indexThree), SchemeHoldBond.Contribution)
      .unsafeSet(CostOfBondsPage(srn, indexThree), money)
      .unsafeSet(BondsCompleted(srn, indexThree), SectionCompleted)

  private val incompleteUserAnswers = userAnswers
    .unsafeSet(HowWereBondsDisposedOfPage(srn, indexOne, index1of50), HowDisposed.Sold)
    .unsafeSet(
      BondsDisposalProgress(srn, indexOne, index1of50),
      SectionJourneyStatus.InProgress(
        routes.WhenWereBondsSoldController.onPageLoad(srn, indexOne, index1of50, NormalMode).url
      )
    )

  private val bondsData = List(
    BondsData(
      indexOne,
      nameOfBonds = "bonds one",
      heldBondsType = SchemeHoldBond.Transfer,
      bondsValue = money
    ),
    BondsData(
      indexTwo,
      nameOfBonds = "bonds two",
      heldBondsType = SchemeHoldBond.Acquisition,
      bondsValue = money
    ),
    BondsData(
      indexThree,
      nameOfBonds = "bonds three",
      heldBondsType = SchemeHoldBond.Contribution,
      bondsValue = money
    )
  )

  "BondsDisposalListControllerSpec" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListRadiosView]
        .apply(form(injected[RadioListFormProvider]), viewModel(srn, page = 1, bondsData, NormalMode, userAnswers))
    })

    act.like(
      redirectToPage(
        call = onSubmit,
        page = routes.WhenWereBondsSoldController.onPageLoad(srn, indexOne, index1of50, NormalMode),
        userAnswers = incompleteUserAnswers,
        previousUserAnswers = emptyUserAnswers,
        form = "value" -> "1"
      ).withName("Redirect to incomplete record")
    )

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
