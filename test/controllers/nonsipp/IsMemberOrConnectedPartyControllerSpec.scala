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

package controllers.nonsipp

import controllers.ControllerBaseSpec
import forms.RadioListFormProvider
import models.{MemberOrConnectedParty, NormalMode, UserAnswers}
import play.api.mvc.Call
import views.html.RadioListView
import IsMemberOrConnectedPartyController._
import pages.nonsipp.IsMemberOrConnectedPartyPage
import pages.nonsipp.loansmadeoroutstanding.IndividualRecipientNamePage

class IsMemberOrConnectedPartyControllerSpec extends ControllerBaseSpec {

  lazy val onPageLoad = routes.IsMemberOrConnectedPartyController.onPageLoad(srn, NormalMode)
  lazy val onSubmit = routes.IsMemberOrConnectedPartyController.onSubmit(srn, NormalMode)

  val userServicesWithIndividualName: UserAnswers =
    defaultUserAnswers.unsafeSet(IndividualRecipientNamePage(srn), individualName)

  "IsMemberOrConnectedParty Controller" - {

    act.like(renderView(onPageLoad, userServicesWithIndividualName) { implicit app => implicit request =>
      injected[RadioListView].apply(form(injected[RadioListFormProvider]), viewModel(srn, individualName, NormalMode))
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        IsMemberOrConnectedPartyPage(srn),
        MemberOrConnectedParty.Member,
        userServicesWithIndividualName
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]).fill(MemberOrConnectedParty.Member),
            viewModel(srn, individualName, NormalMode)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> MemberOrConnectedParty.Member.name))

    act.like(invalidForm(onSubmit, userServicesWithIndividualName))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
