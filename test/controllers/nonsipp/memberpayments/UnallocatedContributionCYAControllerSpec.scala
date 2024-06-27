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

package controllers.nonsipp.memberpayments

import services.PsrSubmissionService
import play.api.mvc.Call
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CYAWithRemove
import models.{CheckMode, Mode, NormalMode}
import pages.nonsipp.memberpayments.UnallocatedEmployerAmountPage
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, times, verify}

class UnallocatedContributionCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrSubmissionService)

  private def onPageLoad(mode: Mode): Call =
    routes.UnallocatedContributionCYAController.onPageLoad(srn, mode)

  private def onSubmit(mode: Mode): Call =
    routes.UnallocatedContributionCYAController.onSubmit(srn, mode)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(UnallocatedEmployerAmountPage(srn), money)

  "UnallocatedContributionCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CYAWithRemove].apply(
            UnallocatedContributionCYAController.viewModel(
              ViewModelParameters(
                srn,
                schemeName,
                money,
                mode
              )
            )
          )
        }.withName(s"render correct ${mode.toString} view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .withName(s"redirect to next page when in ${mode.toString} mode")
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${mode.toString} mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${mode.toString} mode")
      )
    }
  }
}
