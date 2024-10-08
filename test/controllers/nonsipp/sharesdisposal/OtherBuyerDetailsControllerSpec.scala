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

package controllers.nonsipp.sharesdisposal

import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import views.html.RecipientDetailsView
import eu.timepit.refined.refineMV
import pages.nonsipp.sharesdisposal.OtherBuyerDetailsPage
import forms.RecipientDetailsFormProvider
import models.{NormalMode, RecipientDetails, UserAnswers}
import config.RefinedTypes.{Max50, Max5000}
import controllers.ControllerBaseSpec

class OtherBuyerDetailsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  private lazy val onPageLoad =
    controllers.nonsipp.sharesdisposal.routes.OtherBuyerDetailsController
      .onPageLoad(srn, index, disposalIndex, NormalMode)
  private lazy val onSubmit =
    controllers.nonsipp.sharesdisposal.routes.OtherBuyerDetailsController
      .onSubmit(srn, index, disposalIndex, NormalMode)

  val updatedUserAnswers: UserAnswers =
    defaultUserAnswers.unsafeSet(CompanyNameRelatedSharesPage(srn, index), companyName)

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
          .viewModel(srn, index, disposalIndex, companyName, NormalMode)
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
              .viewModel(srn, index, disposalIndex, companyName, NormalMode)
          )
      }
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, updatedUserAnswers, validForm: _*))
    act.like(invalidForm(onSubmit, updatedUserAnswers, invalidForm: _*))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
