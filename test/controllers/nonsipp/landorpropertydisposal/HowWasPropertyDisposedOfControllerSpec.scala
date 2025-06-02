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

import controllers.nonsipp.landorpropertydisposal.HowWasPropertyDisposedOfController._
import views.html.RadioListView
import utils.IntUtils.toInt
import pages.nonsipp.landorpropertydisposal.HowWasPropertyDisposedOfPage
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.{HowDisposed, NormalMode}
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec

class HowWasPropertyDisposedOfControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    routes.HowWasPropertyDisposedOfController.onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit = routes.HowWasPropertyDisposedOfController.onSubmit(srn, index, disposalIndex, NormalMode)

  private val userAnswers = userAnswersWithAddress(srn, index)

  "HowWasPropertyDisposedOfController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[RadioListView]
        .apply(
          form(injected[RadioListFormProvider]),
          viewModel(srn, index, disposalIndex, address.addressLine1, NormalMode)
        )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        HowWasPropertyDisposedOfPage(srn, index, disposalIndex),
        HowDisposed.Sold,
        userAnswers
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(HowDisposed.Sold),
            viewModel(srn, index, disposalIndex, address.addressLine1, NormalMode)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "Sold"))
    act.like(redirectNextPage(onSubmit, "value" -> "Transferred"))
    act.like(redirectNextPage(onSubmit, "value" -> "Other", "conditional" -> "details"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "Sold"))
    act.like(saveAndContinue(onSubmit, "value" -> "Transferred"))
    act.like(saveAndContinue(onSubmit, "value" -> "Other", "conditional" -> "details"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
