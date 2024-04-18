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

package controllers.nonsipp.declaration

import services.PsrSubmissionService
import controllers.nonsipp.declaration.PspDeclarationController._
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.PsaIdInputView
import forms.TextFormProvider
import pages.nonsipp.declaration.PspDeclarationPage
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, times, verify}

class PspDeclarationControllerSpec extends ControllerBaseSpec {
  private val populatedUserAnswers = {
    defaultUserAnswers.unsafeSet(PspDeclarationPage(srn), psaId.value)
  }
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "PspDeclarationController" - {

    lazy val viewModel = PspDeclarationController.viewModel(srn)

    lazy val onPageLoad = routes.PspDeclarationController.onPageLoad(srn)
    lazy val onSubmit = routes.PspDeclarationController.onSubmit(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[PsaIdInputView]
        .apply(form(injected[TextFormProvider], Some(psaId.value)), viewModel)
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      agreeAndContinue(onSubmit, populatedUserAnswers, "value" -> psaId.value)
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }
}
