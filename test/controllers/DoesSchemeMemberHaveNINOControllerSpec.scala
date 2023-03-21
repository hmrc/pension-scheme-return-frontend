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

package controllers

import controllers.DoesSchemeMemberHaveNINOController._
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models.NormalMode
import pages.{MemberDetailsPage, NationalInsuranceNumberPage}
import views.html.YesNoPageView

class DoesSchemeMemberHaveNINOControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, refineMV(1), NormalMode)
  private lazy val onSubmit = routes.DoesSchemeMemberHaveNINOController.onSubmit(srn, refineMV(1), NormalMode)

  private val userAnswersWithMemberDetails =
    defaultUserAnswers.set(MemberDetailsPage(srn, refineMV(1)), memberDetails).success.value

  "NationalInsuranceNumberController" should {

    act.like(renderView(onPageLoad, userAnswersWithMemberDetails) { implicit app => implicit request =>
      val preparedForm = form(injected[YesNoPageFormProvider], memberDetails.fullName)
      injected[YesNoPageView].apply(preparedForm, viewModel(refineMV(1), memberDetails.fullName, srn, NormalMode))
    })

    act.like(
      renderPrePopView(onPageLoad, NationalInsuranceNumberPage(srn, refineMV(1)), true, userAnswersWithMemberDetails) {
        implicit app => implicit request =>
          val preparedForm = form(injected[YesNoPageFormProvider], memberDetails.fullName).fill(true)
          injected[YesNoPageView].apply(preparedForm, viewModel(refineMV(1), memberDetails.fullName, srn, NormalMode))
      }
    )

    act.like(redirectWhenCacheEmpty(onPageLoad, controllers.routes.JourneyRecoveryController.onPageLoad()))
    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(redirectNextPage(onSubmit, userAnswersWithMemberDetails, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, userAnswersWithMemberDetails, "value" -> "false"))
    act.like(saveAndContinue(onSubmit, userAnswersWithMemberDetails, "value" -> "true"))
    act.like(invalidForm(onSubmit, userAnswersWithMemberDetails))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
