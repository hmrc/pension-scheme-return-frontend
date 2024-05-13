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

package controllers.nonsipp.landorpropertydisposal

import controllers.nonsipp.landorpropertydisposal.TotalProceedsSaleLandPropertyController._
import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import views.html.MoneyView
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import pages.nonsipp.landorpropertydisposal.TotalProceedsSaleLandPropertyPage
import eu.timepit.refined.refineMV
import forms.MoneyFormProvider
import models.NormalMode

class TotalProceedsSaleLandPropertyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.TotalProceedsSaleLandPropertyController.onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit =
    routes.TotalProceedsSaleLandPropertyController.onSubmit(srn, index, disposalIndex, NormalMode)

  "TotalProceedsSaleLandPropertyController" - {

    val updatedUserAnswers = defaultUserAnswers.unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)

    act.like(renderView(onPageLoad, updatedUserAnswers) { implicit app => implicit request =>
      injected[MoneyView].apply(
        form(injected[MoneyFormProvider]),
        viewModel(srn, index, disposalIndex, address.addressLine1, form(injected[MoneyFormProvider]), NormalMode)
      )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        TotalProceedsSaleLandPropertyPage(srn, index, disposalIndex),
        money,
        updatedUserAnswers
      ) { implicit app => implicit request =>
        injected[MoneyView].apply(
          form(injected[MoneyFormProvider]).fill(money),
          viewModel(
            srn,
            index,
            disposalIndex,
            address.addressLine1,
            form(injected[MoneyFormProvider]),
            NormalMode
          )
        )
      }
    )

    act.like(redirectNextPage(onSubmit, updatedUserAnswers, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, updatedUserAnswers, "value" -> "1"))

    act.like(invalidForm(onSubmit, updatedUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
