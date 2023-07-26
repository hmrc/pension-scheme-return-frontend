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

import config.Refined.OneTo9999999
import controllers.nonsipp.loansmadeoroutstanding.RemoveLoanController._
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.nonsipp.loansmadeoroutstanding.RemoveLoanPage
import views.html.YesNoPageView

class RemoveLoanControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo9999999](1)

  private lazy val onPageLoad = routes.RemoveLoanController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.RemoveLoanController.onSubmit(srn, index, NormalMode)

  "RemoveLoanController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, RemoveLoanPage(srn), true) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, index, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
