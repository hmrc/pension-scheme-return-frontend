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

package controllers.nonsipp.sharesdisposal

import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import pages.nonsipp.sharesdisposal._
import forms.YesNoPageFormProvider
import models.{IdentityType, NormalMode}
import controllers.nonsipp.sharesdisposal.IsBuyerConnectedPartyController._
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec

class IsBuyerConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val shareIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.IsBuyerConnectedPartyController.onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.IsBuyerConnectedPartyController.onSubmit(srn, shareIndex, disposalIndex, NormalMode)

  "IsBuyerConnectedPartyController" - {

    val individualUserAnswers = defaultUserAnswers
      .unsafeSet(WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex), IdentityType.Individual)
      .unsafeSet(SharesIndividualBuyerNamePage(srn, shareIndex, disposalIndex), buyerName)

    val companyUserAnswers = defaultUserAnswers
      .unsafeSet(WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex), IdentityType.UKCompany)
      .unsafeSet(CompanyBuyerNamePage(srn, shareIndex, disposalIndex), buyerName)

    val partnershipUserAnswers = defaultUserAnswers
      .unsafeSet(WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex), IdentityType.UKPartnership)
      .unsafeSet(PartnershipBuyerNamePage(srn, shareIndex, disposalIndex), buyerName)

    List(
      ("individual", individualUserAnswers),
      ("company", companyUserAnswers),
      ("partnership", partnershipUserAnswers)
    ).foreach {
      case (testScenario, userAnswers) =>
        act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
          injected[YesNoPageView]
            .apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(srn, shareIndex, disposalIndex, buyerName, NormalMode)
            )
        }.updateName(_ + s" as $testScenario"))

        act.like(
          renderPrePopView(
            onPageLoad,
            IsBuyerConnectedPartyPage(srn, shareIndex, disposalIndex),
            true,
            userAnswers
          ) { implicit app => implicit request =>
            injected[YesNoPageView]
              .apply(
                form(injected[YesNoPageFormProvider]).fill(true),
                viewModel(srn, shareIndex, disposalIndex, buyerName, NormalMode)
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
