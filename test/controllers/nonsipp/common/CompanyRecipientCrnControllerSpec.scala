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
    IdentitySubject.values.foreach {
      case identitySubject =>
        lazy val onPageLoad =
          controllers.nonsipp.common.routes.CompanyRecipientCrnController
            .onPageLoad(srn, index, NormalMode, identitySubject)
        lazy val onSubmit =
          controllers.nonsipp.common.routes.CompanyRecipientCrnController
            .onSubmit(srn, index, NormalMode, identitySubject)
        val userAnswersWithData = defaultUserAnswers
          .unsafeSet(CompanyRecipientNamePage(srn, index), companyName)
          .unsafeSet(CompanySellerNamePage(srn, index), companyName)

        testCrnController(identitySubject, onPageLoad, onSubmit, userAnswersWithData)
    }

    "Unknown" - {
      lazy val onPageLoad =
        controllers.nonsipp.common.routes.CompanyRecipientCrnController
          .onPageLoad(srn, index, NormalMode, IdentitySubject.Unknown)

      act.like(
        unauthorisedPage(onPageLoad, Some(defaultUserAnswers))
          .updateName("onPageLoad " + _)
      )
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
    }.withName(s"should render testCrnController for $identitySubject"))

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
      }.withName(s"should render pre-populated testCrnController for $identitySubject")
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "true", "value.yes" -> crn.value)
        .withName(s"should redirect to next page value yes, for $identitySubject")
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false", "value.no" -> "reason")
        .withName(s"should redirect to next page value no, for $identitySubject")
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .updateName("onPageLoad" + _)
        .withName(s"should redirect to Journey Recovery if no existing data is found, for $identitySubject")
    )

    act.like(
      saveAndContinue(onSubmit, "value" -> "true", "value.yes" -> crn.value)
        .withName(s"should save data and continue to next page with for $identitySubject")
    )

    act.like(
      invalidForm(onSubmit, userAnswersWithCompanyName)
        .withName(s"should return BAD_REQUEST for a POST with invalid form for $identitySubject")
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .updateName("onSubmit" + _)
        .withName(
          s"should redirect to Journey Recovery if no existing data is found when submitting, for $identitySubject"
        )
    )
  }
}
