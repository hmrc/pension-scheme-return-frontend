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

import config.Refined.{Max50, Max5000}
import controllers.ControllerBaseSpec
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyStillHeldController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.nonsipp.landorpropertydisposal.LandOrPropertyStillHeldPage
import views.html.YesNoPageView

class LandOrPropertyStillHeldControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)
  private lazy val onPageLoad =
    routes.LandOrPropertyStillHeldController.onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit = routes.LandOrPropertyStillHeldController.onSubmit(srn, index, disposalIndex, NormalMode)

  private val userAnswers = userAnswersWithAddress(srn, index)

  "LandOrPropertyStillHeldController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, index, disposalIndex, address.addressLine1, schemeName, NormalMode)
        )
    })

    act.like(renderPrePopView(onPageLoad, LandOrPropertyStillHeldPage(srn, index, disposalIndex), true, userAnswers) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, index, disposalIndex, address.addressLine1, schemeName, NormalMode)
          )
    })
    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
