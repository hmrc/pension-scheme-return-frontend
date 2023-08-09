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

import config.Refined.OneTo9999999
import controllers.ControllerBaseSpec
import controllers.nonsipp.landorproperty.WhyDoesSchemeHoldLandPropertyController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.{NormalMode, SchemeHoldLandProperty}
import pages.nonsipp.landorproperty.WhyDoesSchemeHoldLandPropertyPage
import views.html.RadioListView

class WhyDoesSchemeHoldLandPropertyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo9999999](1)

  lazy val onPageLoad = routes.WhyDoesSchemeHoldLandPropertyController.onPageLoad(srn, index, NormalMode)
  lazy val onSubmit = routes.WhyDoesSchemeHoldLandPropertyController.onSubmit(srn, index, NormalMode)

  "WhyDoesSchemeHoldLandPropertyController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[RadioListView]
        .apply(form(injected[RadioListFormProvider]), viewModel(srn, index, schemeName, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        WhyDoesSchemeHoldLandPropertyPage(srn, index),
        SchemeHoldLandProperty.Acquisition
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(SchemeHoldLandProperty.Acquisition),
            viewModel(srn, index, schemeName, NormalMode)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> SchemeHoldLandProperty.Acquisition.name))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
