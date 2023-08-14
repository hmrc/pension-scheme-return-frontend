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

package controllers.nonsipp.loansmadeoroutstanding

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.loansmadeoroutstanding.IsMemberOrConnectedPartyController._
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.{MemberOrConnectedParty, NormalMode, UserAnswers}
import pages.nonsipp.loansmadeoroutstanding.{IndividualRecipientNamePage, IsMemberOrConnectedPartyPage}
import views.html.RadioListView

class IsMemberOrConnectedPartyControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  lazy val onPageLoad = routes.IsMemberOrConnectedPartyController.onPageLoad(srn, index, NormalMode)
  lazy val onSubmit = routes.IsMemberOrConnectedPartyController.onSubmit(srn, index, NormalMode)

  val userServicesWithIndividualName: UserAnswers =
    defaultUserAnswers.unsafeSet(IndividualRecipientNamePage(srn, index), individualName)

  "IsMemberOrConnectedParty Controller" - {

    act.like(renderView(onPageLoad, userServicesWithIndividualName) { implicit app => implicit request =>
      injected[RadioListView]
        .apply(form(injected[RadioListFormProvider]), viewModel(srn, index, individualName, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        IsMemberOrConnectedPartyPage(srn, index),
        MemberOrConnectedParty.Member,
        userServicesWithIndividualName
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(MemberOrConnectedParty.Member),
            viewModel(srn, index, individualName, NormalMode)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> MemberOrConnectedParty.Member.name))

    act.like(invalidForm(onSubmit, userServicesWithIndividualName))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
