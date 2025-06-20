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

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.RecipientDetailsView
import pages.nonsipp.landorpropertydisposal.OtherBuyerDetailsPage
import forms.RecipientDetailsFormProvider
import models.{NormalMode, RecipientDetails, UserAnswers}
import utils.IntUtils.given
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage

class OtherBuyerDetailsControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private val disposalIndex = 1

  private lazy val onPageLoad =
    controllers.nonsipp.landorpropertydisposal.routes.OtherBuyerDetailsController
      .onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit =
    controllers.nonsipp.landorpropertydisposal.routes.OtherBuyerDetailsController
      .onSubmit(srn, index, disposalIndex, NormalMode)

  val updatedUserAnswers: UserAnswers =
    defaultUserAnswers.unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)

  private val validForm = List(
    "name" -> "name",
    "description" -> "description"
  )

  private val invalidForm = List(
    "name" -> "",
    "description" -> ""
  )

  private val recipientDetails = RecipientDetails(
    "testName",
    "testDescription"
  )

  "OtherBuyerDetailsController" - {

    act.like(renderView(onPageLoad, updatedUserAnswers) { implicit app => implicit request =>
      injected[RecipientDetailsView].apply(
        OtherBuyerDetailsController.form(injected[RecipientDetailsFormProvider]),
        OtherBuyerDetailsController
          .viewModel(srn, index, disposalIndex, NormalMode, address.addressLine1)
      )
    })

    act.like(
      renderPrePopView(
        onPageLoad,
        OtherBuyerDetailsPage(srn, index, disposalIndex),
        recipientDetails,
        updatedUserAnswers
      ) { implicit app => implicit request =>
        val preparedForm =
          OtherBuyerDetailsController
            .form(injected[RecipientDetailsFormProvider])
            .fill(recipientDetails)
        injected[RecipientDetailsView]
          .apply(
            preparedForm,
            OtherBuyerDetailsController
              .viewModel(srn, index, disposalIndex, NormalMode, address.addressLine1)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, updatedUserAnswers, validForm*))
    act.like(invalidForm(onSubmit, updatedUserAnswers, invalidForm*))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
