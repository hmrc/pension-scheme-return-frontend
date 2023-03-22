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

import controllers.NoNINOController._
import eu.timepit.refined.refineMV
import forms.TextFormProvider
import models.NormalMode
import pages.{MemberDetailsPage, NoNINOPage}
import utils.UserAnswersUtils._
import views.html.TextAreaView

class NoNINOControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.NoNINOController.onPageLoad(srn, refineMV(1), NormalMode)
  private lazy val onSubmit = routes.NoNINOController.onSubmit(srn, refineMV(1), NormalMode)

  private val userAnswersWithMembersDetails = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)

  "NoNINOController" should {

    act.like(renderView(onPageLoad, userAnswersWithMembersDetails) { implicit app => implicit request =>
      injected[TextAreaView].apply(
        form(injected[TextFormProvider], memberDetails.fullName),
        viewModel(srn, memberDetails.fullName, refineMV(1), NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, NoNINOPage(srn, refineMV(1)), "test text", userAnswersWithMembersDetails) {
      implicit app => implicit request =>
        injected[TextAreaView].apply(
          form(injected[TextFormProvider], memberDetails.fullName).fill("test text"),
          viewModel(srn, memberDetails.fullName, refineMV(1), NormalMode)
        )
    })

    act.like(redirectNextPage(onSubmit, userAnswersWithMembersDetails, "value" -> "test text"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, userAnswersWithMembersDetails, "value" -> "test text"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))

    act.like(invalidForm(onSubmit, userAnswersWithMembersDetails))
  }
}
