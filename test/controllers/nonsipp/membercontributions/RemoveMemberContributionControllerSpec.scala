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

import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NameDOB
import pages.nonsipp.membercontributions.TotalMemberContributionPage
import pages.nonsipp.memberdetails.MemberDetailsPage
import views.html.YesNoPageView

class RemoveMemberContributionControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.RemoveMemberContributionController.onPageLoad(srn, refineMV(1), refineMV(1))
  private lazy val onSubmit = routes.RemoveMemberContributionController.onSubmit(srn, refineMV(1), refineMV(1))

  override val memberDetails: NameDOB = nameDobGen.sample.value

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)
    .unsafeSet(TotalMemberContributionPage(srn, refineMV(1), refineMV(1)), money)

  "RemoveMemberContributionController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        RemoveMemberContributionController.form(injected[YesNoPageFormProvider]),
        RemoveMemberContributionController.viewModel(srn, refineMV(1), refineMV(1), money, memberDetails.fullName)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))
    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }

}