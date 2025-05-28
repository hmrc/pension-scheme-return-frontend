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

package controllers.nonsipp.common

import views.html.RecipientDetailsView
import eu.timepit.refined.refineMV
import forms.RecipientDetailsFormProvider
import models.{IdentitySubject, NormalMode, RecipientDetails}
import pages.nonsipp.common.OtherRecipientDetailsPage
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec

class OtherRecipientDetailsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

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

  "OtherRecipientDetailsController" - {
    IdentitySubject.values.foreach { identitySubject =>
      s"for $identitySubject" - {
        lazy val onPageLoad =
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onPageLoad(srn, index.value, NormalMode, identitySubject)
        lazy val onSubmit =
          controllers.nonsipp.common.routes.OtherRecipientDetailsController
            .onSubmit(srn, index.value, NormalMode, identitySubject)

        act.like(renderView(onPageLoad) { implicit app => implicit request =>
          injected[RecipientDetailsView].apply(
            OtherRecipientDetailsController.form(injected[RecipientDetailsFormProvider], identitySubject),
            OtherRecipientDetailsController
              .viewModel(srn, index, NormalMode, identitySubject, defaultUserAnswers)
          )
        })

        act.like(
          renderPrePopView(
            onPageLoad,
            OtherRecipientDetailsPage(srn, index, identitySubject),
            recipientDetails
          ) { implicit app => implicit request =>
            val preparedForm =
              OtherRecipientDetailsController
                .form(injected[RecipientDetailsFormProvider], identitySubject)
                .fill(recipientDetails)
            injected[RecipientDetailsView]
              .apply(
                preparedForm,
                OtherRecipientDetailsController
                  .viewModel(srn, index, NormalMode, identitySubject, defaultUserAnswers)
              )
          }
        )

        act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

        act.like(saveAndContinue(onSubmit, validForm: _*))
        act.like(invalidForm(onSubmit, defaultUserAnswers, invalidForm: _*))
        act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
      }
    }

    "Unknown" - {
      lazy val onPageLoad =
        controllers.nonsipp.common.routes.OtherRecipientDetailsController
          .onPageLoad(srn, index.value, NormalMode, IdentitySubject.Unknown)

      act.like(
        unauthorisedPage(onPageLoad, Some(defaultUserAnswers))
          .updateName("onPageLoad " + _)
      )
    }
  }
}
