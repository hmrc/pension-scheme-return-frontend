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

package controllers.nonsipp.landorpropertydisposal

import controllers.ControllerBaseSpec
import views.html.ListRadiosView
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalAddressListController._
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage

class LandOrPropertyDisposalAddressListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.LandOrPropertyDisposalAddressListController.onPageLoad(srn, page = 1)
  private lazy val onSubmit = routes.LandOrPropertyDisposalAddressListController.onSubmit(srn, page = 1)

  private val address1 = addressGen.sample.value
  private val address2 = addressGen.sample.value

  private val addresses = Map(0 -> address1, 1 -> address2)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, refineMV(1)), address1)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, refineMV(2)), address2)

  "LandOrPropertyDisposalAddressListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[ListRadiosView]
        .apply(form(injected[RadioListFormProvider]), viewModel(srn, page = 1, addresses, userAnswers))
    })

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
