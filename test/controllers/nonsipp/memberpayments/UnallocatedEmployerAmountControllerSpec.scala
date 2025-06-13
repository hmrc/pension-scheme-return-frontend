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

package controllers.nonsipp.memberpayments

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.MoneyView
import forms.MoneyFormProvider
import controllers.nonsipp.memberpayments.UnallocatedEmployerAmountController._
import models.NormalMode
import pages.nonsipp.memberpayments.UnallocatedEmployerAmountPage

class UnallocatedEmployerAmountControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.UnallocatedEmployerAmountController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.UnallocatedEmployerAmountController.onSubmit(srn, NormalMode)

  "ValueOfSchemeAssetsWhenMoneyBorrowedController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MoneyView].apply(
        form(injected[MoneyFormProvider]),
        viewModel(
          srn,
          schemeName,
          form(injected[MoneyFormProvider]),
          NormalMode
        )
      )
    })

    act.like(
      renderPrePopView(onPageLoad, UnallocatedEmployerAmountPage(srn), money) { implicit app => implicit request =>
        injected[MoneyView].apply(
          form(injected[MoneyFormProvider]).fill(money),
          viewModel(
            srn,
            schemeName,
            form(injected[MoneyFormProvider]),
            NormalMode
          )
        )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "1"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
