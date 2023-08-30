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
import controllers.nonsipp.landorproperty.PropertyAcquiredFromController.{form, viewModel}
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.{IdentityType, NormalMode}
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPage, PropertyAcquiredFromPage}
import views.html.RadioListView

class PropertyAcquiredFromControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  lazy val onPageLoad = routes.PropertyAcquiredFromController.onPageLoad(srn, index, NormalMode)
  lazy val onSubmit = routes.PropertyAcquiredFromController.onSubmit(srn, index, NormalMode)

  private val userAnswersWithLookUpPage =
    defaultUserAnswers.unsafeSet(LandOrPropertyAddressLookupPage(srn, index), address)

  "PropertyAcquiredFromController" - {

    act.like(renderView(onPageLoad, userAnswersWithLookUpPage) { implicit app => implicit request =>
      injected[RadioListView]
        .apply(
          form(injected[RadioListFormProvider]),
          viewModel(srn, index, address.addressLine1, NormalMode)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        PropertyAcquiredFromPage(srn, index),
        IdentityType.Individual,
        userAnswersWithLookUpPage
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(IdentityType.Individual),
            viewModel(srn, index, address.addressLine1, NormalMode)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswersWithLookUpPage, "value" -> IdentityType.Individual.name))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
