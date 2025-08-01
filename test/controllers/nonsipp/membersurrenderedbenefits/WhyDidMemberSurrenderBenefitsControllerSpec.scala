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

package controllers.nonsipp.membersurrenderedbenefits

import pages.nonsipp.memberdetails.MemberDetailsPage
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.TextAreaView
import utils.IntUtils.given
import controllers.nonsipp.membersurrenderedbenefits.WhyDidMemberSurrenderBenefitsController._
import forms.TextFormProvider
import pages.nonsipp.membersurrenderedbenefits.{SurrenderedBenefitsAmountPage, WhyDidMemberSurrenderBenefitsPage}
import models.{NormalMode, UserAnswers}

class WhyDidMemberSurrenderBenefitsControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1

  private lazy val onPageLoad = routes.WhyDidMemberSurrenderBenefitsController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.WhyDidMemberSurrenderBenefitsController.onSubmit(srn, index, NormalMode)

  val populatedUserAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(SurrenderedBenefitsAmountPage(srn, index), surrenderedBenefitsAmount)

  "WhyDidMemberSurrenderBenefitsController" - {

    act.like(renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
      injected[TextAreaView].apply(
        form(injected[TextFormProvider]),
        viewModel(srn, index, NormalMode, schemeName, surrenderedBenefitsAmount.displayAs, memberDetails.fullName)
      )
    })

    act.like(
      renderPrePopView(onPageLoad, WhyDidMemberSurrenderBenefitsPage(srn, index), "test text", populatedUserAnswers) {
        implicit app => implicit request =>
          injected[TextAreaView].apply(
            form(injected[TextFormProvider]).fill("test text"),
            viewModel(srn, index, NormalMode, schemeName, surrenderedBenefitsAmount.displayAs, memberDetails.fullName)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, populatedUserAnswers, "value" -> "test text"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, populatedUserAnswers, "value" -> "test text"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

    act.like(invalidForm(onSubmit, populatedUserAnswers))
  }
}
