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
import controllers.nonsipp.common.PartnershipRecipientUtrController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, IdentitySubject, NormalMode, Utr}
import pages.nonsipp.common.PartnershipRecipientUtrPage
import pages.nonsipp.loansmadeoroutstanding.PartnershipRecipientNamePage
import views.html.ConditionalYesNoPageView

class PartnershipRecipientUtrControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad =
    controllers.nonsipp.common.routes.PartnershipRecipientUtrController
      .onPageLoad(srn, index, NormalMode, IdentitySubject.LoanRecipient)
  private lazy val onSubmit =
    controllers.nonsipp.common.routes.PartnershipRecipientUtrController
      .onSubmit(srn, index, NormalMode, IdentitySubject.LoanRecipient)

  val userAnswersWithPartnershipRecipientName =
    defaultUserAnswers.unsafeSet(PartnershipRecipientNamePage(srn, index), partnershipName)

  val conditionalNo: ConditionalYesNo[String, Utr] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Utr] = ConditionalYesNo.yes(utr)

  "PartnershipRecipientUtrController" - {

    act.like(renderView(onPageLoad, userAnswersWithPartnershipRecipientName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider], IdentitySubject.LoanRecipient),
          viewModel(srn, index, NormalMode, IdentitySubject.LoanRecipient, userAnswersWithPartnershipRecipientName)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        PartnershipRecipientUtrPage(srn, index, IdentitySubject.LoanRecipient),
        conditionalNo,
        userAnswersWithPartnershipRecipientName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider], IdentitySubject.LoanRecipient).fill(conditionalNo.value),
            viewModel(srn, index, NormalMode, IdentitySubject.LoanRecipient, userAnswersWithPartnershipRecipientName)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> utr.value).withName("redirect on yes"))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason").withName("redirect on no"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> utr.value).withName("save and continue"))

    act.like(invalidForm(onSubmit, userAnswersWithPartnershipRecipientName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
