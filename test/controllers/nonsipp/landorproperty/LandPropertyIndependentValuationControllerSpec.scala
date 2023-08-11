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

package controllers.nonsipp.landorproperty

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.landorproperty.LandPropertyIndependentValuationController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{Address, NormalMode}
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPage, LandPropertyIndependentValuationPage}
import views.html.YesNoPageView

class LandPropertyIndependentValuationControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad = routes.LandPropertyIndependentValuationController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.LandPropertyIndependentValuationController.onSubmit(srn, index, NormalMode)

  val address = Address("addressLine1", "addressLine2", None, None, None, "UK")
  private val userAnswersWithLookUpPage =
    defaultUserAnswers.unsafeSet(LandOrPropertyAddressLookupPage(srn, index), address)

  "LandPropertyIndependentValuationController" - {

    act.like(renderView(onPageLoad, userAnswersWithLookUpPage) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, "addressLine1", NormalMode))
    })

    act.like(
      renderPrePopView(onPageLoad, LandPropertyIndependentValuationPage(srn, index), true, userAnswersWithLookUpPage) {
        implicit app => implicit request =>
          injected[YesNoPageView]
            .apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, index, "addressLine1", NormalMode))
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswersWithLookUpPage, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, userAnswersWithLookUpPage, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswersWithLookUpPage))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))
  }
}
