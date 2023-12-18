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

package controllers.nonsipp.common

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.common.CompanyRecipientCrnController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{ConditionalYesNo, Crn, IdentitySubject, NormalMode, UserAnswers}
import pages.nonsipp.common.CompanyRecipientCrnPage
import pages.nonsipp.landorproperty.CompanySellerNamePage
import pages.nonsipp.loansmadeoroutstanding.CompanyRecipientNamePage
import play.api.mvc.Call
import views.html.ConditionalYesNoPageView

class CompanyRecipientCrnControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  val conditionalNo: ConditionalYesNo[String, Crn] = ConditionalYesNo.no("reason")
  val conditionalYes: ConditionalYesNo[String, Crn] = ConditionalYesNo.yes(crn)

  "CompanyRecipientCrnController" - {
    "loans journey" - {
      val identitySubject: IdentitySubject = IdentitySubject.LoanRecipient
      lazy val onPageLoad =
        controllers.nonsipp.common.routes.CompanyRecipientCrnController
          .onPageLoad(srn, index, NormalMode, identitySubject)
      lazy val onSubmit =
        controllers.nonsipp.common.routes.CompanyRecipientCrnController
          .onSubmit(srn, index, NormalMode, identitySubject)
      val userAnswersWithCompanyName = defaultUserAnswers.unsafeSet(CompanyRecipientNamePage(srn, index), companyName)

      testCrnController(identitySubject, onPageLoad, onSubmit, userAnswersWithCompanyName)
    }
    "land or property journey" - {
      val identitySubject: IdentitySubject = IdentitySubject.LandOrPropertySeller
      lazy val onPageLoad =
        controllers.nonsipp.common.routes.CompanyRecipientCrnController
          .onPageLoad(srn, index, NormalMode, identitySubject)
      lazy val onSubmit =
        controllers.nonsipp.common.routes.CompanyRecipientCrnController
          .onSubmit(srn, index, NormalMode, identitySubject)
      val userAnswersWithCompanySellerName =
        defaultUserAnswers.unsafeSet(CompanySellerNamePage(srn, index), companyName)

      testCrnController(identitySubject, onPageLoad, onSubmit, userAnswersWithCompanySellerName)
    }
  }

  private def testCrnController(
    identitySubject: IdentitySubject,
    onPageLoad: => Call,
    onSubmit: => Call,
    userAnswersWithCompanyName: UserAnswers
  ): Unit = {
    act.like(renderView(onPageLoad, userAnswersWithCompanyName) { implicit app => implicit request =>
      injected[ConditionalYesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider], identitySubject),
          viewModel(srn, index, NormalMode, identitySubject, userAnswersWithCompanyName)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        CompanyRecipientCrnPage(srn, index, identitySubject),
        conditionalNo,
        userAnswersWithCompanyName
      ) { implicit app => implicit request =>
        injected[ConditionalYesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider], identitySubject).fill(conditionalNo.value),
            viewModel(srn, index, NormalMode, identitySubject, userAnswersWithCompanyName)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> crn.value).withName("redirect on yes"))
    act.like(redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason").withName("redirect on no"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> crn.value).withName("save and continue"))

    act.like(invalidForm(onSubmit, userAnswersWithCompanyName))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
