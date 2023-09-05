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
import controllers.nonsipp.common.OtherRecipientDetailsController
import eu.timepit.refined.refineMV
import forms.RecipientDetailsFormProvider
import models.{NormalMode, RecipientDetails}
import pages.nonsipp.loansmadeoroutstanding.OtherRecipientDetailsPage
import views.html.RecipientDetailsView

class OtherRecipientDetailsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private lazy val onPageLoad =
    controllers.nonsipp.common.routes.OtherRecipientDetailsController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit =
    controllers.nonsipp.common.routes.OtherRecipientDetailsController.onSubmit(srn, index, NormalMode)

  private val validForm = List(
    "name" -> "name",
    "description" -> "description"
  )

  private val recipientDetails = RecipientDetails(
    "testName",
    "testDescription"
  )

  "OtherRecipientDetailsController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[RecipientDetailsView].apply(
        OtherRecipientDetailsController.form(injected[RecipientDetailsFormProvider]),
        OtherRecipientDetailsController.viewModel(srn, index, NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, OtherRecipientDetailsPage(srn, index), recipientDetails) {
      implicit app => implicit request =>
        val preparedForm =
          OtherRecipientDetailsController.form(injected[RecipientDetailsFormProvider]).fill(recipientDetails)
        injected[RecipientDetailsView]
          .apply(preparedForm, OtherRecipientDetailsController.viewModel(srn, index, NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, validForm: _*))
    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
