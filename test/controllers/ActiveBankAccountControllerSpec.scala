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

package controllers

import controllers.ActiveBankAccountController._
import forms.YesNoPageFormProvider
import models.NormalMode
import views.html.YesNoPageView

class ActiveBankAccountControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ActiveBankAccountController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.ActiveBankAccountController.onSubmit(srn, NormalMode)

  "ActiveBankAccountController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val preparedForm = form(injected[YesNoPageFormProvider], defaultSchemeDetails.schemeName)
      injected[YesNoPageView]
        .apply(preparedForm, viewModel(srn, defaultSchemeDetails.schemeName, NormalMode))

    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))
    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
