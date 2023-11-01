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

package controllers.nonsipp.common

import controllers.ControllerBaseSpec
import controllers.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingController._
import controllers.nonsipp.loansmadeoroutstanding.routes
import forms.YesNoPageFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import views.html.YesNoPageView

class LoansMadeOrOutstandingControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.LoansMadeOrOutstandingController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.LoansMadeOrOutstandingController.onSubmit(srn, NormalMode)
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "loansMadeOrOutstandingController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView].apply(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, defaultSchemeDetails.schemeName, NormalMode)
      )
    })

    act.like(renderPrePopView(onPageLoad, LoansMadeOrOutstandingPage(srn), true) { implicit app => implicit request =>
      val preparedForm = form(injected[YesNoPageFormProvider]).fill(true)
      injected[YesNoPageView].apply(preparedForm, viewModel(srn, defaultSchemeDetails.schemeName, NormalMode))
    })

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, never).submitPsrDetails(any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .before(MockPSRSubmissionService.submitPsrDetails())
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit " + _))
  }
}
