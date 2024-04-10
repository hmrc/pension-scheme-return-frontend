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

package controllers.nonsipp.memberdetails

import pages.nonsipp.memberdetails.MemberDetailsPage
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.{NameDOB, NormalMode}
import controllers.nonsipp.memberdetails.RemoveMemberDetailsController.{form, viewModel}
import controllers.nonsipp.memberdetails.routes
import controllers.ControllerBaseSpec

class RemoveMemberDetailsControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.RemoveMemberDetailsController.onPageLoad(srn, refineMV(1), NormalMode)
  private lazy val onSubmit = routes.RemoveMemberDetailsController.onSubmit(srn, refineMV(1), NormalMode)

  override val memberDetails: NameDOB = nameDobGen.sample.value

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)

  "RemoveMemberDetailsController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]

      view(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, refineMV(1), memberDetails, NormalMode)
      )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))
    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }

}
