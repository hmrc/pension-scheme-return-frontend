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

package controllers.nonsipp.employercontributions

import EmployerContributionsController.viewModel
import controllers.ControllerBaseSpec
import controllers.nonsipp.employercontributions.routes
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.EmployerContributionsPage
import views.html.YesNoPageView

class EmployerContributionsControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.EmployerContributionsController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.EmployerContributionsController.onSubmit(srn, NormalMode)

  val form = EmployerContributionsController.form(new YesNoPageFormProvider())

  "EmployerContributionsController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(
        form,
        viewModel(srn, NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, EmployerContributionsPage(srn), true) { implicit app => implicit request =>
      val preparedForm = form.fill(true)
      injected[YesNoPageView].apply(preparedForm, viewModel(srn, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))
  }
}
