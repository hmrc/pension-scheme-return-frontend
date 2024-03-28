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

package controllers.nonsipp.membercontributions

import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.membercontributions.TotalMemberContributionPage
import controllers.nonsipp.membercontributions.TotalMemberContributionController._
import config.Refined._
import controllers.ControllerBaseSpec
import views.html.MoneyView
import eu.timepit.refined.refineMV
import forms.MoneyFormProvider
import models.NormalMode

class TotalMemberContributionControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val userAnswers = defaultUserAnswers.unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)

  private lazy val onPageLoad =
    routes.TotalMemberContributionController.onPageLoad(srn, index, NormalMode)

  private lazy val onSubmit =
    routes.TotalMemberContributionController.onSubmit(srn, index, NormalMode)

  "TotalMemberContributionController" - {

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
      renderPrePopView(onPageLoad, TotalMemberContributionPage(srn, index), money, userAnswers) {
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
