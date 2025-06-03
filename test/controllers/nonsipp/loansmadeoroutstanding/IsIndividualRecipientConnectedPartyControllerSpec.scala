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

package controllers.nonsipp.loansmadeoroutstanding

import play.api.mvc.Call
import controllers.nonsipp.loansmadeoroutstanding.IsIndividualRecipientConnectedPartyController._
import views.html.YesNoPageView
import utils.IntUtils.toInt
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{NormalMode, UserAnswers}
import pages.nonsipp.loansmadeoroutstanding.{IndividualRecipientNamePage, IsIndividualRecipientConnectedPartyPage}
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec

class IsIndividualRecipientConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  lazy val onPageLoad: Call = routes.IsIndividualRecipientConnectedPartyController.onPageLoad(srn, index, NormalMode)
  lazy val onSubmit: Call = routes.IsIndividualRecipientConnectedPartyController.onSubmit(srn, index, NormalMode)
  private val incomeTaxAct = "https://www.legislation.gov.uk/ukpga/2007/3/section/993"

  val userServicesWithIndividualName: UserAnswers =
    defaultUserAnswers.unsafeSet(IndividualRecipientNamePage(srn, index), individualName)

  "IsIndividualRecipientConnectedPartyController" - {

    act.like(renderView(onPageLoad, userServicesWithIndividualName) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, index, individualName, incomeTaxAct, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        IsIndividualRecipientConnectedPartyPage(srn, index),
        true,
        userServicesWithIndividualName
      ) { implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, index, individualName, incomeTaxAct, NormalMode)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, userServicesWithIndividualName))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
