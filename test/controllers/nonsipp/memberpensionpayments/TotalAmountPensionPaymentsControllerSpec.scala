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

package controllers.nonsipp.memberpensionpayments

import config.Refined._
import controllers.ControllerBaseSpec
import controllers.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsController._
import eu.timepit.refined.refineMV
import forms.MoneyFormProvider
import models.NormalMode
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsPage
import views.html.MoneyView

class TotalAmountPensionPaymentsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val userAnswers = defaultUserAnswers.unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)

  private lazy val onPageLoad =
    routes.TotalAmountPensionPaymentsController.onPageLoad(srn, index, NormalMode)

  private lazy val onSubmit =
    routes.TotalAmountPensionPaymentsController.onSubmit(srn, index, NormalMode)

  "TotalAmountPensionPaymentsController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[MoneyView]
        .apply(
          viewModel(
            srn,
            index,
            memberDetails.fullName,
            form(injected[MoneyFormProvider]),
            NormalMode
          )
        )
    })

    act.like(
      renderPrePopView(onPageLoad, TotalAmountPensionPaymentsPage(srn, index), money, userAnswers) {
        implicit app => implicit request =>
          injected[MoneyView].apply(
            viewModel(
              srn,
              index,
              memberDetails.fullName,
              form(injected[MoneyFormProvider]).fill(money),
              NormalMode
            )
          )
      }
    )

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "1"))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
