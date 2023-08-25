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
import controllers.nonsipp.loansmadeoroutstanding.PartnershipRecipientUtrController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, NormalMode, Utr}
import pages.nonsipp.loansmadeoroutstanding.{PartnershipRecipientNamePage, PartnershipRecipientUtrPage}
import views.html.ConditionalYesNoPageView

class PartnershipRecipientUtrControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad = routes.PartnershipRecipientUtrController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.PartnershipRecipientUtrController.onSubmit(srn, index, NormalMode)

  val userAnswersWithPartnershipRecipientName =
    defaultUserAnswers.unsafeSet(PartnershipRecipientNamePage(srn, index, NormalMode), partnershipName)

  val conditionalNo: ConditionalYesNo[String, Utr] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Utr] = ConditionalYesNo.yes(utr)

  "PartnershipRecipientUtrController" - {

    act.like(renderView(onPageLoad, userAnswersWithPartnershipRecipientName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, partnershipName, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        PartnershipRecipientUtrPage(srn, index, NormalMode),
        conditionalNo,
        userAnswersWithPartnershipRecipientName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, index, partnershipName, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> utr.value))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> utr.value))

    act.like(invalidForm(onSubmit, userAnswersWithPartnershipRecipientName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
