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

package controllers.nonsipp.receivetransfer

import views.html.RadioListView
import utils.IntUtils.toInt
import pages.nonsipp.receivetransfer.TransferringSchemeNamePage
import eu.timepit.refined.refineMV
import controllers.nonsipp.receivetransfer.TransferringSchemeTypeController._
import forms.RadioListFormProvider
import models.NormalMode
import config.RefinedTypes.{Max300, Max5}
import controllers.ControllerBaseSpec

class TransferringSchemeTypeControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondarylIndex = refineMV[Max5.Refined](1)

  private lazy val onPageLoad =
    controllers.nonsipp.receivetransfer.routes.TransferringSchemeTypeController
      .onPageLoad(srn, index, secondarylIndex, NormalMode)
  private lazy val onSubmit = controllers.nonsipp.receivetransfer.routes.TransferringSchemeTypeController
    .onSubmit(srn, index, secondarylIndex, NormalMode)
  private val userAnswers = defaultUserAnswers

  "TransferringSchemeTypeController" - {

    act.like(
      renderView(
        onPageLoad,
        userAnswers
          .unsafeSet(TransferringSchemeNamePage(srn, index, secondarylIndex), transferringSchemeName)
      ) { implicit app => implicit request =>
        injected[RadioListView]
          .apply(
            form(injected[RadioListFormProvider]),
            viewModel(srn, index, secondarylIndex, transferringSchemeName, NormalMode)
          )
      }.withName("onPageLoad renders page ok")
    )

    act.like(
      journeyRecoveryPage(onPageLoad, Some(emptyUserAnswers))
        .withName("onPageLoad redirect to journey recovery page when transferring scheme name is missing")
    )

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

    act.like(saveAndContinue(onSubmit, "value" -> "other", "other-conditional" -> "details"))

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
