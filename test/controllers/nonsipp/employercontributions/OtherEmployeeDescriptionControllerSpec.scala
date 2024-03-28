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

import config.Refined._
import controllers.ControllerBaseSpec
import views.html.TextAreaView
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode
import controllers.nonsipp.employercontributions.OtherEmployeeDescriptionController._
import pages.nonsipp.employercontributions.{EmployerNamePage, OtherEmployeeDescriptionPage}

class OtherEmployeeDescriptionControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)
  private lazy val onPageLoad =
    routes.OtherEmployeeDescriptionController.onPageLoad(srn, index, secondaryIndex, NormalMode)
  private lazy val onSubmit = routes.OtherEmployeeDescriptionController.onSubmit(srn, index, secondaryIndex, NormalMode)

  private val userAnswers = defaultUserAnswers.unsafeSet(EmployerNamePage(srn, index, secondaryIndex), employerName)

  "OtherEmployeeDescriptionController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[TextAreaView]
        .apply(form(injected[TextFormProvider]), viewModel(srn, employerName, index, secondaryIndex, NormalMode))
    })

    act.like(
      renderPrePopView(onPageLoad, OtherEmployeeDescriptionPage(srn, index, secondaryIndex), "test text", userAnswers) {
        implicit app => implicit request =>
          injected[TextAreaView]
            .apply(
              form(injected[TextFormProvider]).fill("test text"),
              viewModel(srn, employerName, index, secondaryIndex, NormalMode)
            )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "test text"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "test text"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(invalidForm(onSubmit, userAnswers))
  }
}
