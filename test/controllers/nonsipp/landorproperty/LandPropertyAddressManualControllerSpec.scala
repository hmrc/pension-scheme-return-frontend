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

import controllers.nonsipp.landorproperty.LandPropertyAddressManualController._
import utils.Country
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.MultipleQuestionView
import models.NormalMode
import utils.IntUtils.given
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage

class LandPropertyAddressManualControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private lazy val onPageLoad =
    routes.LandPropertyAddressManualController.onPageLoad(srn, index, isUkAddress = true, NormalMode)
  private lazy val onPageLoadInternational =
    routes.LandPropertyAddressManualController.onPageLoad(srn, index, isUkAddress = false, NormalMode)

  private lazy val onSubmit =
    routes.LandPropertyAddressManualController.onSubmit(srn, index, isUkAddress = true, NormalMode)
  private lazy val onSubmitInternational =
    routes.LandPropertyAddressManualController.onSubmit(srn, index, isUkAddress = false, NormalMode)

  "LandPropertyAddressManualController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MultipleQuestionView].apply(
        ukAddressForm,
        viewModel(srn, index, ukPage(ukAddressForm), isUkAddress = true, NormalMode)
      )
    })

    act.like(renderView(onPageLoadInternational) { implicit app => implicit request =>
      injected[MultipleQuestionView].apply(
        internationalAddressForm(Country.countries),
        viewModel(
          srn,
          index,
          internationalPage(internationalAddressForm(Country.countries), Country.countries),
          isUkAddress = false,
          NormalMode
        )
      )
    }.updateName(_ + " - International"))

    act.like(renderPrePopView(onPageLoad, LandOrPropertyChosenAddressPage(srn, index), address) {
      implicit app => implicit request =>
        injected[MultipleQuestionView].apply(
          ukAddressForm.fill(address.asUKAddressTuple),
          viewModel(srn, index, ukPage(ukAddressForm), isUkAddress = true, NormalMode)
        )
    })

    act.like(
      redirectNextPage(
        onSubmit,
        "value.1" -> "test address line 1",
        "value.2" -> "test address line 2",
        "value.3" -> "test address line 3",
        "value.4" -> "test town",
        "value.5" -> "ZZ1 1ZZ"
      )
    )

    act.like(
      redirectNextPage(
        onSubmitInternational,
        "value.1" -> "test address line 1",
        "value.2" -> "test address line 2",
        "value.3" -> "test address line 3",
        "value.4" -> "test town",
        "value.5" -> "test postal code",
        "value.6" -> "GB"
      ).updateName(_ + " - International")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      saveAndContinue(
        onSubmit,
        "value.1" -> "test address line 1",
        "value.2" -> "test address line 2",
        "value.3" -> "test address line 3",
        "value.4" -> "test town",
        "value.5" -> "ZZ1 1ZZ"
      )
    )

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
