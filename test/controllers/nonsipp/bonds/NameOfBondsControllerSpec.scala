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

package controllers.nonsipp.bonds

import controllers.nonsipp.bonds.NameOfBondsController._
import pages.nonsipp.bonds.NameOfBondsPage
import controllers.ControllerBaseSpec
import views.html.TextAreaView
import forms.TextFormProvider
import models.NormalMode

class NameOfBondsControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.NameOfBondsController.onPageLoad(srn, 1, NormalMode)
  private lazy val onSubmit = routes.NameOfBondsController.onSubmit(srn, 1, NormalMode)

  "NameOfBondsController" - {

    act.like(renderView(onPageLoad, defaultUserAnswers) { implicit app => implicit request =>
      injected[TextAreaView].apply(
        form(injected[TextFormProvider]),
        viewModel(srn, 1, NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, NameOfBondsPage(srn, index1of5000), "test text", defaultUserAnswers) {
      implicit app => implicit request =>
        injected[TextAreaView].apply(
          form(injected[TextFormProvider]).fill("test text"),
          viewModel(srn, 1, NormalMode)
        )
    })

    act.like(redirectNextPage(onSubmit, defaultUserAnswers, "value" -> "test text"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, defaultUserAnswers, "value" -> "test text"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

    act.like(invalidForm(onSubmit, defaultUserAnswers))
  }
}
