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

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.YesNoPageView
import forms.YesNoPageFormProvider
import models.NormalMode
import controllers.nonsipp.landorproperty.IsLandPropertyLeasedController._
import utils.IntUtils.given
import pages.nonsipp.landorproperty.{IsLandPropertyLeasedPage, LandOrPropertyChosenAddressPage, LandPropertyInUKPage}

class IsLandPropertyLeasedControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1

  private lazy val onPageLoad = routes.IsLandPropertyLeasedController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.IsLandPropertyLeasedController.onSubmit(srn, index, NormalMode)

  "LandPropertyInUKController" - {

    val updatedUserAnswers = defaultUserAnswers
      .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
      .unsafeSet(LandPropertyInUKPage(srn, index), true)

    act.like(renderView(onPageLoad, updatedUserAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, index, address.addressLine1, NormalMode, updatedUserAnswers)
        )
    })

    act.like(renderPrePopView(onPageLoad, IsLandPropertyLeasedPage(srn, index), true, updatedUserAnswers) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, index, address.addressLine1, NormalMode, updatedUserAnswers)
          )
    })

    act.like(redirectNextPage(onSubmit, updatedUserAnswers, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, updatedUserAnswers, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, updatedUserAnswers, "value" -> "true"))

    act.like(invalidForm(onSubmit, updatedUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
