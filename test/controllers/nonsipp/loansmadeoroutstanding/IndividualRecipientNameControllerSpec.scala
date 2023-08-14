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
import controllers.nonsipp.loansmadeoroutstanding.IndividualRecipientNameController._
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode
import pages.nonsipp.loansmadeoroutstanding.IndividualRecipientNamePage
import views.html.TextInputView

class IndividualRecipientNameControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  "IndividualRecipientNameController" - {

    val populatedUserAnswers = defaultUserAnswers.set(IndividualRecipientNamePage(srn, index), recipientName).get
    lazy val onPageLoad = routes.IndividualRecipientNameController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.IndividualRecipientNameController.onSubmit(srn, index, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[TextInputView].apply(form(injected[TextFormProvider]), viewModel(srn, index, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, IndividualRecipientNamePage(srn, index), recipientName) {
      implicit app => implicit request =>
        val preparedForm = form(injected[TextFormProvider]).fill(recipientName)
        injected[TextInputView].apply(preparedForm, viewModel(srn, index, NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> recipientName))
    act.like(invalidForm(onSubmit, populatedUserAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
