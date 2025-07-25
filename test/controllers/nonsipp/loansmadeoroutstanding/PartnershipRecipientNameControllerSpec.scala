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

import controllers.nonsipp.loansmadeoroutstanding.PartnershipRecipientNameController._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.TextInputView
import utils.IntUtils.given
import forms.TextFormProvider
import models.NormalMode
import pages.nonsipp.loansmadeoroutstanding.PartnershipRecipientNamePage

class PartnershipRecipientNameControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1

  "PartnershipRecipientNameController" - {

    val populatedUserAnswers = defaultUserAnswers.set(PartnershipRecipientNamePage(srn, index), partnershipName).get
    lazy val onPageLoad = routes.PartnershipRecipientNameController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.PartnershipRecipientNameController.onSubmit(srn, index, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, PartnershipRecipientNamePage(srn, index), partnershipName) {
      implicit app => implicit request =>
        val preparedForm = form(injected[TextFormProvider]).fill(partnershipName)
        injected[TextInputView].apply(preparedForm, viewModel(srn, index, NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> partnershipName))
    act.like(invalidForm(onSubmit, populatedUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
