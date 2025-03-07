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

package controllers.nonsipp.membertransferout

import views.html.RadioListView
import eu.timepit.refined.refineMV
import forms.RadioListFormProvider
import models.NormalMode
import pages.nonsipp.membertransferout.ReceivingSchemeNamePage
import config.RefinedTypes.{Max300, Max5}
import controllers.ControllerBaseSpec

class ReceivingSchemeTypeControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondarylIndex = refineMV[Max5.Refined](1)

  private lazy val onPageLoad =
    controllers.nonsipp.membertransferout.routes.ReceivingSchemeTypeController
      .onPageLoad(srn, index, secondarylIndex, NormalMode)
  private lazy val onSubmit = controllers.nonsipp.membertransferout.routes.ReceivingSchemeTypeController
    .onSubmit(srn, index, secondarylIndex, NormalMode)
  private val userAnswers = defaultUserAnswers
    .unsafeSet(ReceivingSchemeNamePage(srn, index, secondarylIndex), individualName)

  "ReceivingSchemeTypeController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[RadioListView]
        .apply(
          ReceivingSchemeTypeController.form(injected[RadioListFormProvider]),
          ReceivingSchemeTypeController.viewModel(srn, index, secondarylIndex, individualName, NormalMode)
        )
    })

    act.like(saveAndContinue(onSubmit, "value" -> "registeredPS", "registeredPS-conditional" -> " 872 19363 yN"))

    act.like(
      saveAndContinue(
        onSubmit,
        "value" -> "qualifyingRecognisedOverseasPS",
        "qualifyingRecognisedOverseasPS-conditional" -> " q 19 7 363 "
      )
    )

    act.like(redirectNextPage(onSubmit, "value" -> "other", "other-conditional" -> "details"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "registeredPS", "registeredPS-conditional" -> " 872 19363 YN"))

    act.like(
      invalidForm(
        onSubmit,
        "value" -> "other",
        "other-conditional" -> "This text has 166 characters. It includes 28 spaces which should be counted with the overall length. If this test passes then it means that it is counting whitespace."
      )
    )

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
