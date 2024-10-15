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

import views.html.RadioListView
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode
import controllers.nonsipp.landorproperty.LandPropertyAddressResultsController._
import config.RefinedTypes._
import controllers.ControllerBaseSpec

class LandPropertyAddressResultsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private lazy val onPageLoad = routes.LandPropertyAddressResultsController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.LandPropertyAddressResultsController.onSubmit(srn, index, NormalMode)

  private val userAnswers = defaultUserAnswers.unsafeSet(AddressLookupResultsPage(srn, index), List(address))

  "LandPropertyAddressResultsController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[RadioListView].apply(form(injected[TextFormProvider]), viewModel(srn, index, List(address), NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, LandOrPropertyChosenAddressPage(srn, index), address, userAnswers) {
      implicit app => implicit request =>
        injected[RadioListView]
          .apply(form(injected[TextFormProvider]).fill(address.id), viewModel(srn, index, List(address), NormalMode))
    })
    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> address.id))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> address.id))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
