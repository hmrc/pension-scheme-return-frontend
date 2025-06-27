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

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import utils.IntUtils.given
import pages.nonsipp.FbVersionPage
import models._
import viewmodels.models.SectionJourneyStatus
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.employercontributions._
import services.PsrSubmissionService
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito.{reset, when, _}
import controllers.nonsipp.employercontributions.EmployerContributionsCYAController._
import views.html.CheckYourAnswersView

import scala.concurrent.Future

class EmployerContributionsCYAControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1
  private val secondaryIndex = 1
  private val page = 1

  private val employerCYAs = List(
    EmployerCYA(secondaryIndex, employerName, IdentityType.UKCompany, Right(crn.value), money)
  )

  private def onPageLoad(mode: Mode) =
    routes.EmployerContributionsCYAController.onPageLoad(srn, index, page, mode)
  private def onSubmit(mode: Mode) = routes.EmployerContributionsCYAController.onSubmit(srn, index, page, mode)

  private lazy val onPageLoadViewOnly = routes.EmployerContributionsCYAController.onPageLoadViewOnly(
    srn,
    index,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onSubmitViewOnly = routes.EmployerContributionsCYAController.onSubmitViewOnly(
    srn,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(EmployerNamePage(srn, index, secondaryIndex), employerName)
    .unsafeSet(EmployerTypeOfBusinessPage(srn, index, secondaryIndex), IdentityType.UKCompany)
    .unsafeSet(TotalEmployerContributionPage(srn, index, secondaryIndex), money)
    .unsafeSet(EmployerCompanyCrnPage(srn, index, secondaryIndex), ConditionalYesNo.yes[String, Crn](crn))
    .unsafeSet(EmployerContributionsProgress(srn, index, secondaryIndex), SectionJourneyStatus.Completed)

  private val userAnswersInProgress = userAnswers
    .unsafeSet(
      EmployerContributionsProgress(srn, index, index1of50),
      SectionJourneyStatus.InProgress(anyUrl)
    )

  override def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "EmployerContributionsCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(renderView(onPageLoad(mode), userAnswers) { implicit app => implicit request =>
        injected[CheckYourAnswersView].apply(
          viewModel(srn, memberDetails.fullName, index, page, employerCYAs, mode, viewOnlyUpdated = true)
        )
      }.withName(s"render correct ${mode.toString} view"))

      act.like(
        redirectToPage(
          call = onPageLoad(mode),
          page = routes.EmployerContributionsMemberListController.onPageLoad(srn, 1, mode),
          userAnswers = userAnswersInProgress
        ).withName(s"Redirect to list page if the only entry is incomplete in mode $mode")
      )

      act.like(redirectNextPage(onSubmit(mode)).updateName(s"${mode.toString} onSubmit" + _))

      act.like(journeyRecoveryPage(onPageLoad(mode)).updateName(s"${mode.toString} onPageLoad" + _))

      act.like(journeyRecoveryPage(onSubmit(mode)).updateName(s"${mode.toString} onSubmit" + _))
    }
  }

  "EmployerContributionsCYAController in view only mode" - {

    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")

    val previousUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "001")

    act.like(
      renderView(
        onPageLoadViewOnly,
        userAnswers = currentUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[CheckYourAnswersView].apply(
          EmployerContributionsCYAController.viewModel(
            srn,
            memberDetails.fullName,
            index,
            page,
            employerCYAs,
            ViewOnlyMode,
            viewOnlyUpdated = false,
            optYear = Some(yearString),
            optCurrentVersion = Some(submissionNumberTwo),
            optPreviousVersion = Some(submissionNumberOne)
          )
        )
      }.withName("Render view only mode correctly")
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.employercontributions.routes.EmployerContributionsMemberListController
          .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
        verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(using any(), any(), any())
      ).withName("Submit redirects to view only tasklist")
    )
  }
}
