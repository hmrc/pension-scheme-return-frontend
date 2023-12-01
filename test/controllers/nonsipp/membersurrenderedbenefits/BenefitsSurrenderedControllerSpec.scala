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

package controllers.nonsipp.membersurrenderedbenefits

import controllers.ControllerBaseSpec
import controllers.nonsipp.membersurrenderedbenefits.BenefitsSurrenderedController._
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.nonsipp.memberpayments.BenefitsSurrenderedPage
import play.api.libs.json.JsPath
import views.html.YesNoPageView

class BenefitsSurrenderedControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.BenefitsSurrenderedController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.BenefitsSurrenderedController.onSubmit(srn, NormalMode)

  "BenefitsSurrenderedController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(form(injected[YesNoPageFormProvider]), viewModel(srn, schemeName, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, BenefitsSurrenderedPage(srn), true) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, schemeName, NormalMode))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(
        onSubmit,
        defaultUserAnswers,
        Some(JsPath \ "membersPayments" \ "surrenderMade"),
        "value" -> "true"
      )
    )

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
