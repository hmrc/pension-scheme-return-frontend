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

package controllers.nonsipp.otherrecipientdetails

import controllers.ControllerBaseSpec
import forms.RecipientDetailsFormProvider
import models.{NormalMode, RecipientDetails}
import pages.nonsipp.otherrecipientdetails.OtherRecipientDetailsPage
import views.html.RecipientDetailsView

class OtherRecipientDetailsControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.OtherRecipientDetailsController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.OtherRecipientDetailsController.onSubmit(srn, NormalMode)

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
        OtherRecipientDetailsController.viewModel(srn, NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, OtherRecipientDetailsPage(srn), recipientDetails) {
      implicit app => implicit request =>
        val preparedForm =
          OtherRecipientDetailsController.form(injected[RecipientDetailsFormProvider]).fill(recipientDetails)
        injected[RecipientDetailsView].apply(preparedForm, OtherRecipientDetailsController.viewModel(srn, NormalMode))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, validForm: _*))
    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
