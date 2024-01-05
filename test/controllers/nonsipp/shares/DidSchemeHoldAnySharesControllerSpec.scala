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

package controllers.nonsipp.shares

import controllers.ControllerBaseSpec
import controllers.nonsipp.shares.DidSchemeHoldAnySharesController._
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.nonsipp.shares.DidSchemeHoldAnySharesPage
import views.html.YesNoPageView

class DidSchemeHoldAnySharesControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.DidSchemeHoldAnySharesController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.DidSchemeHoldAnySharesController.onSubmit(srn, NormalMode)
  private val incomeTaxAct = "https://www.legislation.gov.uk/ukpga/2007/3/section/993"

  "DidSchemeHoldAnySharesController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, schemeName, incomeTaxAct, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, DidSchemeHoldAnySharesPage(srn), true) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, schemeName, incomeTaxAct, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
