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

package controllers.nonsipp.membercontributions

import services.PsrSubmissionService
import pages.nonsipp.membercontributions.TotalMemberContributionPage
import config.Refined.Max300
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CheckYourAnswersView
import eu.timepit.refined.refineMV
import models.{CheckMode, Mode, NormalMode}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito.{reset, times, verify}

class MemberContributionsCYAControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override def beforeEach(): Unit =
    reset(mockPsrSubmissionService)

  private def onPageLoad(mode: Mode) =
    routes.MemberContributionsCYAController.onPageLoad(srn, index, mode)

  private def onSubmit(mode: Mode) =
    routes.MemberContributionsCYAController.onSubmit(srn, mode)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(TotalMemberContributionPage(srn, index), money)
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)

  "CYAMemberContributionsController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            MemberContributionsCYAController.viewModel(
              ViewModelParameters(
                srn,
                memberDetails.fullName,
                index,
                money,
                mode
              )
            )
          )
        }.withName(s"render correct $mode view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .withName(s"redirect to next page when in $mode mode")
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in $mode mode")
      )
    }
  }
}
