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

import pages.nonsipp.otherassetsdisposal._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.YesNoPageView
import utils.IntUtils.given
import controllers.nonsipp.otherassetsdisposal.IsBuyerConnectedPartyController._
import forms.YesNoPageFormProvider
import models.{IdentityType, NormalMode}

class IsBuyerConnectedPartyControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val assetIndex = 1
  private val disposalIndex = 1

  private lazy val onPageLoad =
    routes.IsBuyerConnectedPartyController.onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.IsBuyerConnectedPartyController.onSubmit(srn, assetIndex, disposalIndex, NormalMode)

  "IsBuyerConnectedPartyController" - {

    val individualUserAnswers = defaultUserAnswers
      .unsafeSet(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex), IdentityType.Individual)
      .unsafeSet(IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex), buyerName)

    val companyUserAnswers = defaultUserAnswers
      .unsafeSet(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex), IdentityType.UKCompany)
      .unsafeSet(CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex), buyerName)

    val partnershipUserAnswers = defaultUserAnswers
      .unsafeSet(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex), IdentityType.UKPartnership)
      .unsafeSet(PartnershipBuyerNamePage(srn, assetIndex, disposalIndex), buyerName)

    List(
      ("individual", individualUserAnswers),
      ("company", companyUserAnswers),
      ("partnership", partnershipUserAnswers)
    ).foreach { case (testScenario, userAnswers) =>
      act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]),
            viewModel(srn, assetIndex, disposalIndex, buyerName, NormalMode)
          )
      }.updateName(_ + s" as $testScenario"))

      act.like(
        renderPrePopView(
          onPageLoad,
          IsBuyerConnectedPartyPage(srn, assetIndex, disposalIndex),
          true,
          userAnswers
        ) { implicit app => implicit request =>
          injected[YesNoPageView]
            .apply(
              form(injected[YesNoPageFormProvider]).fill(true),
              viewModel(srn, assetIndex, disposalIndex, buyerName, NormalMode)
            )
        }.updateName(_ + s" as $testScenario")
      )

      act.like(invalidForm(onSubmit, userAnswers).updateName(_ + s" as $testScenario"))
    }

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
