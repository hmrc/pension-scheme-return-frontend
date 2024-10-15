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

package controllers.nonsipp.moneyborrowed

import play.api.mvc.Call
import controllers.nonsipp.moneyborrowed.IsLenderConnectedPartyController._
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{NormalMode, UserAnswers}
import pages.nonsipp.moneyborrowed.{IsLenderConnectedPartyPage, LenderNamePage}
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec

class IsLenderConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)
  private val incomeTaxAct = "https://www.legislation.gov.uk/ukpga/2007/3/section/993"

  lazy val onPageLoad: Call = routes.IsLenderConnectedPartyController.onPageLoad(srn, index, NormalMode)
  lazy val onSubmit: Call = routes.IsLenderConnectedPartyController.onSubmit(srn, index, NormalMode)

  val userServicesWithLenderName: UserAnswers =
    defaultUserAnswers.unsafeSet(LenderNamePage(srn, index), lenderName)

  "IsLenderConnectedPartyController" - {

    act.like(renderView(onPageLoad, userServicesWithLenderName) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, lenderName, incomeTaxAct, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        IsLenderConnectedPartyPage(srn, index),
        true,
        userServicesWithLenderName
      ) { implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, index, lenderName, incomeTaxAct, NormalMode)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, userServicesWithLenderName))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
