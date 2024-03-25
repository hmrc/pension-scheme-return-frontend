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

package controllers.nonsipp.bonds

import controllers.ControllerBaseSpec
import controllers.nonsipp.bonds.UnregulatedOrConnectedBondsHeldController._
import forms.YesNoPageFormProvider
import models.NormalMode
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.bonds.UnregulatedOrConnectedBondsHeldPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import views.html.YesNoPageView

class UnregulatedOrConnectedBondsHeldControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.UnregulatedOrConnectedBondsHeldController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.UnregulatedOrConnectedBondsHeldController.onSubmit(srn, NormalMode)
  private lazy val incomeTaxAct = "https://www.legislation.gov.uk/ukpga/2007/3/section/993"
  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "UnregulatedOrConnectedBondsHeldController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, schemeName, incomeTaxAct, NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, UnregulatedOrConnectedBondsHeldPage(srn), true) {
      implicit app => implicit request =>
        injected[YesNoPageView]
          .apply(form(injected[YesNoPageFormProvider]).fill(true), viewModel(srn, schemeName, incomeTaxAct, NormalMode))
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

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}