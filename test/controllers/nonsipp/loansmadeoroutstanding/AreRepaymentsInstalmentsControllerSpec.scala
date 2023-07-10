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
import controllers.ControllerBaseSpec
import controllers.nonsipp.loansmadeoroutstanding.AreRepaymentsInstalmentsController.viewModel
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.nonsipp.loansmadeoroutstanding.AreRepaymentsInstalmentsPage
import views.html.YesNoPageView

class AreRepaymentsInstalmentsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo9999999](1)

  private lazy val onPageLoad = routes.AreRepaymentsInstalmentsController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.AreRepaymentsInstalmentsController.onSubmit(srn, index, NormalMode)

  val form = AreRepaymentsInstalmentsController.form(new YesNoPageFormProvider())

  "AreRepaymentsInstalmentsController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(
        form,
        viewModel(srn, index, NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, AreRepaymentsInstalmentsPage(srn, index), true) {
      implicit app => implicit request =>
        val preparedForm = form.fill(true)
        injected[YesNoPageView].apply(preparedForm, viewModel(srn, index, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))
  }
}
