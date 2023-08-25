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

import config.Refined.Max5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.loansmadeoroutstanding.LoansListController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{IdentitySubject, IdentityType, Money, NormalMode}
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.loansmadeoroutstanding._
import views.html.ListView

class LoansListControllerSpec extends ControllerBaseSpec {
  private val subject = IdentitySubject.LoanRecipient

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(IdentityTypePage(srn, refineMV(1), subject, NormalMode), IdentityType.UKCompany)
    .unsafeSet(CompanyRecipientNamePage(srn, refineMV(1), NormalMode), "recipientName1")
    .unsafeSet(AmountOfTheLoanPage(srn, refineMV(1), NormalMode), (money, money, money))
    .unsafeSet(IdentityTypePage(srn, refineMV(2), subject, NormalMode), IdentityType.UKPartnership)
    .unsafeSet(PartnershipRecipientNamePage(srn, refineMV(2), NormalMode), "recipientName2")
    .unsafeSet(AmountOfTheLoanPage(srn, refineMV(2), NormalMode), (money, money, money))
    .unsafeSet(IdentityTypePage(srn, refineMV(3), subject, NormalMode), IdentityType.Individual)
    .unsafeSet(IndividualRecipientNamePage(srn, refineMV(3), NormalMode), "recipientName3")
    .unsafeSet(AmountOfTheLoanPage(srn, refineMV(3), NormalMode), (money, money, money))

  private lazy val onPageLoad = routes.LoansListController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.LoansListController.onSubmit(srn, NormalMode)
  private lazy val onLoansMadePageLoad = routes.LoansMadeOrOutstandingController.onPageLoad(srn, NormalMode)

  private val recipients: List[(Max5000, String, Money)] = List(
    (refineMV(1), "recipientName1", money),
    (refineMV(2), "recipientName2", money),
    (refineMV(3), "recipientName3", money)
  )

  "LoansListController" - {

    act.like(renderView(onPageLoad, filledUserAnswers) { implicit app => implicit request =>
      injected[ListView].apply(
        form(new YesNoPageFormProvider()),
        viewModel(
          srn,
          NormalMode,
          recipients
        )
      )
    })

    act.like(
      redirectToPage(
        onPageLoad,
        onLoansMadePageLoad,
        defaultUserAnswers
      )
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
