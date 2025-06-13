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

import pages.nonsipp.memberdetails.MemberDetailsPage
import controllers.nonsipp.membertransferout.ReportAnotherTransferOutController._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.YesNoPageView
import utils.IntUtils.given
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.nonsipp.membertransferout.ReportAnotherTransferOutPage

class ReportAnotherTransferOutControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private val secondaryIndex = 1

  private lazy val onPageLoad =
    routes.ReportAnotherTransferOutController.onPageLoad(srn, index, secondaryIndex, NormalMode)
  private lazy val onSubmit =
    routes.ReportAnotherTransferOutController.onSubmit(srn, index, secondaryIndex, NormalMode)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)

  "ReportAnotherTransferOutController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(
            srn,
            index,
            secondaryIndex,
            NormalMode,
            memberDetails.fullName
          )
        )
    })

    act.like(
      renderPrePopView(onPageLoad, ReportAnotherTransferOutPage(srn, index, secondaryIndex), true, userAnswers) {
        implicit app => implicit request =>
          injected[YesNoPageView].apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(
              srn,
              index,
              secondaryIndex,
              NormalMode,
              memberDetails.fullName
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
