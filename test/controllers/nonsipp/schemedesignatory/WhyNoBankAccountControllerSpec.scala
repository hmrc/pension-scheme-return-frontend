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

package controllers.nonsipp.schemedesignatory

import controllers.ControllerBaseSpec
import controllers.nonsipp.schemedesignatory.WhyNoBankAccountController.{form, viewModel}
import forms.TextFormProvider
import models.NormalMode
import pages.nonsipp.schemedesignatory.WhyNoBankAccountPage
import views.html.TextAreaView

class WhyNoBankAccountControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.WhyNoBankAccountController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.WhyNoBankAccountController.onSubmit(srn, NormalMode)

  "WhyNoBankAccountController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextAreaView].apply(form(injected[TextFormProvider]), viewModel(srn, schemeName, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, WhyNoBankAccountPage(srn), "test text") { implicit app => implicit request =>
      injected[TextAreaView]
        .apply(form(injected[TextFormProvider]).fill("test text"), viewModel(srn, schemeName, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "test text"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "reasonNoOpenAccount", "value" -> "test text"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(invalidForm(onSubmit))
  }
}
