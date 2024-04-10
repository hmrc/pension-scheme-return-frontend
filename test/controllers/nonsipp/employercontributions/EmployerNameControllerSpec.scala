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

package controllers.nonsipp.employercontributions

import pages.nonsipp.employercontributions.EmployerNamePage
import controllers.nonsipp.employercontributions.EmployerNameController._
import config.Refined._
import controllers.ControllerBaseSpec
import views.html.TextInputView
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode

class EmployerNameControllerSpec extends ControllerBaseSpec {

  private val memberIndex = refineMV[Max300.Refined](1)
  private val index = refineMV[Max50.Refined](1)
  private lazy val onPageLoad = routes.EmployerNameController.onPageLoad(srn, memberIndex, index, NormalMode)
  private lazy val onSubmit = routes.EmployerNameController.onSubmit(srn, memberIndex, index, NormalMode)

  "EmployerNameController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, memberIndex, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, EmployerNamePage(srn, memberIndex, index), "test") {
      implicit app => implicit request =>
        injected[TextInputView]
          .apply(form(injected[TextFormProvider]).fill("test"), viewModel(srn, memberIndex, index, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "test"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "test"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
