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

import controllers.ControllerBaseSpec
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, NormalMode}
import pages.nonsipp.loansmadeoroutstanding.{CompanyRecipientCrnPage, CompanyRecipientNamePage}
import uk.gov.hmrc.domain.Nino
import views.html.ConditionalYesNoPageView
import CompanyRecipientCrnController._

class CompanyRecipientCrnControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.CompanyRecipientCrnController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.CompanyRecipientCrnController.onSubmit(srn, NormalMode)

  val userAnswersWithCompanyName = defaultUserAnswers.unsafeSet(CompanyRecipientNamePage(srn), companyName)

  val conditionalNo: ConditionalYesNo[Nino] = ConditionalYesNo[Nino](Left("reason"))
  val conditionalYes: ConditionalYesNo[Nino] = ConditionalYesNo[Nino](Right(nino)) //TODO

  "IndividualRecipientNinoController" - {

    act.like(renderView(onPageLoad, userAnswersWithCompanyName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, companyName, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        CompanyRecipientCrnPage(srn),
        conditionalNo,
        userAnswersWithCompanyName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(conditionalNo.value),
            viewModel(srn, companyName, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> nino.value))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> nino.value))

    act.like(invalidForm(onSubmit, userAnswersWithCompanyName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
