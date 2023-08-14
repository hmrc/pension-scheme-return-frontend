/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.nonsipp.loansmadeoroutstanding

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.loansmadeoroutstanding.RemoveLoanController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{IdentitySubject, IdentityType, NormalMode}
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.loansmadeoroutstanding.{
  AmountOfTheLoanPage,
  CompanyRecipientNamePage,
  IndividualRecipientNamePage,
  PartnershipRecipientNamePage
}
import views.html.YesNoPageView

class RemoveLoanControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad = routes.RemoveLoanController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.RemoveLoanController.onSubmit(srn, index, NormalMode)
  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(IdentityTypePage(srn, refineMV(1), IdentitySubject.LoanRecipient), IdentityType.UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, refineMV(1)), "recipientName1")
    .unsafeSet(AmountOfTheLoanPage(srn, refineMV(1)), (money, money, money))
    .unsafeSet(IdentityTypePage(srn, refineMV(2), IdentitySubject.LoanRecipient), IdentityType.UKPartnership)
    .unsafeSet(PartnershipRecipientNamePage(srn, refineMV(2)), "recipientName2")
    .unsafeSet(AmountOfTheLoanPage(srn, refineMV(2)), (money, money, money))
    .unsafeSet(IdentityTypePage(srn, refineMV(3), IdentitySubject.LoanRecipient), IdentityType.Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, refineMV(3)), "recipientName3")
    .unsafeSet(AmountOfTheLoanPage(srn, refineMV(3)), (money, money, money))

  "RemoveLoanController" - {

    act.like(renderView(onPageLoad, filledUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, index, NormalMode, money.displayAs, "recipientName1")
        )
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, filledUserAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
