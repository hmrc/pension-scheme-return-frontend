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

import config.Refined.{Max300, Max50}
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import models.CheckOrChange
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.membercontributions.TotalMemberContributionPage
import pages.nonsipp.memberdetails.MemberDetailsPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import views.html.CheckYourAnswersView

class CYAMemberContributionsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrSubmissionService)

  private def onPageLoad(checkOrChange: CheckOrChange) =
    routes.CYAMemberContributionsController.onPageLoad(srn, index, secondaryIndex, checkOrChange)

  private def onSubmit(checkOrChange: CheckOrChange) =
    routes.CYAMemberContributionsController.onSubmit(srn, checkOrChange)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(TotalMemberContributionPage(srn, index, secondaryIndex), money)
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)

  "CYAMemberContributionsController" - {

    List(CheckOrChange.Check, CheckOrChange.Change).foreach { checkOrChange =>
      act.like(
        renderView(onPageLoad(checkOrChange), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            CYAMemberContributionsController.viewModel(
              ViewModelParameters(
                srn,
                memberDetails.fullName,
                index,
                secondaryIndex,
                money,
                checkOrChange
              )
            )
          )
        }.withName(s"render correct ${checkOrChange.name} view")
      )

      act.like(
        redirectNextPage(onSubmit(checkOrChange))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .withName(s"redirect to next page when in ${checkOrChange.name} mode")
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
      )

      act.like(
        journeyRecoveryPage(onPageLoad(checkOrChange))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in ${checkOrChange.name} mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(checkOrChange))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in ${checkOrChange.name} mode")
      )
    }
  }
}
