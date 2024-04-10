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

package controllers.nonsipp.employercontributions

import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.TwoColumnsTripleAction
import eu.timepit.refined.refineMV
import models.NormalMode
import viewmodels.models.SectionJourneyStatus
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.employercontributions.{EmployerContributionsMemberListPage, EmployerContributionsProgress}
import services.PsrSubmissionService
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import forms.YesNoPageFormProvider
import controllers.nonsipp.employercontributions.EmployerContributionsMemberListController.{EmployerContributions, _}

import scala.concurrent.Future

class EmployerContributionsMemberListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.EmployerContributionsMemberListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.EmployerContributionsMemberListController.onSubmit(srn, page = 1, NormalMode)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(
      EmployerContributionsProgress(srn, refineMV(1), refineMV(1)),
      SectionJourneyStatus.Completed
    )

  val employerContributions: List[EmployerContributions] = List(
    EmployerContributions(
      memberIndex = refineMV(1),
      employerFullName = memberDetails.fullName,
      contributions = List(
        Contributions(
          contributionIndex = refineMV(1),
          status = SectionJourneyStatus.Completed
        )
      )
    )
  )

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  override def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetails(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "EmployerContributionsMemberListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[TwoColumnsTripleAction].apply(
        form(injected[YesNoPageFormProvider]),
        viewModel(srn, page = 1, NormalMode, employerContributions)
      )
    })

    act.like(renderPrePopView(onPageLoad, EmployerContributionsMemberListPage(srn), true, userAnswers) {
      implicit app => implicit request =>
        injected[TwoColumnsTripleAction]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, page = 1, NormalMode, employerContributions)
          )
    })

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
