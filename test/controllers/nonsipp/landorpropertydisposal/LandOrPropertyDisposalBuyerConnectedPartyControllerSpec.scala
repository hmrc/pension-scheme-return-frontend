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

package controllers.nonsipp.landorpropertydisposal

import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalBuyerConnectedPartyController._
import views.html.YesNoPageView
import pages.nonsipp.landorpropertydisposal._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{IdentityType, NormalMode}
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec

class LandOrPropertyDisposalBuyerConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.LandOrPropertyDisposalBuyerConnectedPartyController.onPageLoad(
      srn,
      index.value,
      disposalIndex.value,
      NormalMode
    )
  private lazy val onSubmit =
    routes.LandOrPropertyDisposalBuyerConnectedPartyController.onSubmit(
      srn,
      index.value,
      disposalIndex.value,
      NormalMode
    )

  "LandOrPropertyDisposalSellerConnectedPartyController" - {

    val individualUserAnswers = defaultUserAnswers
      .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndex), IdentityType.Individual)
      .unsafeSet(LandOrPropertyIndividualBuyerNamePage(srn, index, disposalIndex), buyerName)

    val companyUserAnswers = defaultUserAnswers
      .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndex), IdentityType.UKCompany)
      .unsafeSet(CompanyBuyerNamePage(srn, index, disposalIndex), buyerName)

    val partnershipUserAnswers = defaultUserAnswers
      .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndex), IdentityType.UKPartnership)
      .unsafeSet(PartnershipBuyerNamePage(srn, index, disposalIndex), buyerName)

    List(
      ("individual", individualUserAnswers),
      ("company", companyUserAnswers),
      ("partnership", partnershipUserAnswers)
    ).foreach {
      case (testScenario, userAnswers) =>
        act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
          injected[YesNoPageView]
            .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, buyerName, index, disposalIndex, NormalMode))
        }.updateName(_ + s" as $testScenario"))

        act.like(
          renderPrePopView(
            onPageLoad,
            LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndex),
            true,
            userAnswers
          ) { implicit app => implicit request =>
            injected[YesNoPageView]
              .apply(
                form(injected[YesNoPageFormProvider]).fill(true),
                viewModel(srn, buyerName, index, disposalIndex, NormalMode)
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
