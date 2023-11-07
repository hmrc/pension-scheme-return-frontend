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

package controllers.nonsipp.memberpayments

import controllers.ControllerBaseSpec
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.nonsipp.memberpayments.{UnallocatedEmployerAmountPage, UnallocatedEmployerContributionsPage}
import views.html.YesNoPageView

class RemoveUnallocatedAmountControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.RemoveUnallocatedAmountController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.RemoveUnallocatedAmountController.onSubmit(srn, NormalMode)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
    .unsafeSet(UnallocatedEmployerAmountPage(srn), money)

  "RemoveUnallocatedAmountController" - {

    act.like(renderView(onPageLoad, filledUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          RemoveUnallocatedAmountController.form(injected[YesNoPageFormProvider]),
          RemoveUnallocatedAmountController.viewModel(srn, NormalMode, money.displayAs)
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
