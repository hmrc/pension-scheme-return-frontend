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

package controllers.nonsipp.landorproperty

import play.api.mvc.Call
import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import views.html.RadioListView
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, WhyDoesSchemeHoldLandPropertyPage}
import controllers.nonsipp.landorproperty.WhyDoesSchemeHoldLandPropertyController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.{NormalMode, SchemeHoldLandProperty}

class WhyDoesSchemeHoldLandPropertyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  lazy val onPageLoad: Call = routes.WhyDoesSchemeHoldLandPropertyController.onPageLoad(srn, index, NormalMode)
  lazy val onSubmit: Call = routes.WhyDoesSchemeHoldLandPropertyController.onSubmit(srn, index, NormalMode)

  private val userAnswersWithLookUpPage =
    defaultUserAnswers.unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)

  "WhyDoesSchemeHoldLandPropertyController" - {

    act.like(renderView(onPageLoad, userAnswersWithLookUpPage) { implicit app => implicit request =>
      injected[RadioListView]
        .apply(
          form(injected[RadioListFormProvider]),
          viewModel(srn, index, schemeName, address.addressLine1, NormalMode)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        WhyDoesSchemeHoldLandPropertyPage(srn, index),
        SchemeHoldLandProperty.Acquisition,
        userAnswersWithLookUpPage
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(SchemeHoldLandProperty.Acquisition),
            viewModel(srn, index, schemeName, address.addressLine1, NormalMode)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswersWithLookUpPage, "value" -> SchemeHoldLandProperty.Acquisition.name))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
