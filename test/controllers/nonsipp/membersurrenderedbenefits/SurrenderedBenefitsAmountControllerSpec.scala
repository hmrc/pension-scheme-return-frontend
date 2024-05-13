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

package controllers.nonsipp.membersurrenderedbenefits

import pages.nonsipp.memberdetails.MemberDetailsPage
import config.Refined.Max300
import controllers.ControllerBaseSpec
import views.html.MoneyView
import eu.timepit.refined.refineMV
import forms.MoneyFormProvider
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsAmountPage
import models.NormalMode
import controllers.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsAmountController.{form, viewModel}

class SurrenderedBenefitsAmountControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private lazy val onPageLoad = routes.SurrenderedBenefitsAmountController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.SurrenderedBenefitsAmountController.onSubmit(srn, index, NormalMode)

  private val userAnswers = defaultUserAnswers.unsafeSet(MemberDetailsPage(srn, index), memberDetails)

  "SurrenderedBenefitsAmountController" - {

    act.like(
      renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
        injected[MoneyView]
          .apply(
            form(injected[MoneyFormProvider]),
            viewModel(
              srn,
              memberDetails.fullName,
              index,
              form(injected[MoneyFormProvider]),
              NormalMode
            )
          )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, SurrenderedBenefitsAmountPage(srn, index), money, userAnswers) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            form(injected[MoneyFormProvider]).fill(money),
            viewModel(
              srn,
              memberDetails.fullName,
              index,
              form(injected[MoneyFormProvider]),
              NormalMode
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "1"))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
