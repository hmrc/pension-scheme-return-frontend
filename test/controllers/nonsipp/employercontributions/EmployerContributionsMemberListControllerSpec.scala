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

import play.api.test.FakeRequest
import pages.nonsipp.employercontributions._
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import config.Refined.Max300
import controllers.ControllerBaseSpec
import views.html.TwoColumnsTripleAction
import eu.timepit.refined.refineMV
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import controllers.nonsipp.employercontributions.EmployerContributionsMemberListController._
import models.{NormalMode, ViewOnlyMode}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import viewmodels.DisplayMessage.Message
import viewmodels.models._

class EmployerContributionsMemberListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.EmployerContributionsMemberListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.EmployerContributionsMemberListController.onSubmit(srn, page = 1, NormalMode)
  private lazy val onSubmitViewOnly = routes.EmployerContributionsMemberListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.EmployerContributionsMemberListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.EmployerContributionsMemberListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private val index = refineMV[Max300.Refined](1)
  private val page = 1

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(
      EmployerContributionsProgress(srn, refineMV(1), refineMV(1)),
      SectionJourneyStatus.Completed
    )
    .unsafeSet(EmployerContributionsPage(srn), true)

  val employerContributions: List[MemberWithEmployerContributions] = List(
    MemberWithEmployerContributions(
      memberIndex = refineMV(1),
      employerFullName = memberDetails.fullName,
      contributions = List(
        EmployerContributions(
          contributionIndex = refineMV(1),
          status = SectionJourneyStatus.Completed
        )
      )
    )
  )

  "EmployerContributionsMemberListController" - {

    "viewModel should show Employer Contributions when there are 0 contributions" in {
      val employerContributionsEmpty: List[MemberWithEmployerContributions] = List.empty
      val result: FormPageViewModel[ActionTableViewModel] = viewModel(
        srn,
        page,
        mode = ViewOnlyMode,
        employerContributionsEmpty,
        viewOnlyUpdated = false,
        noPageEnabled = false
      )
      result.optViewOnlyDetails.value.heading shouldBe Message(
        "employerContributions.MemberList.viewOnly.noContributions"
      )
    }

    "viewModel should show 1 Employer Contribution when there is 1 contribution" in {
      val employerContributionsSingle: List[MemberWithEmployerContributions] = List(
        MemberWithEmployerContributions(
          memberIndex = refineMV(1),
          employerFullName = "Test Member",
          contributions = List(
            EmployerContributions(
              contributionIndex = refineMV(1),
              status = SectionJourneyStatus.Completed
            )
          )
        )
      )
      val result = viewModel(
        srn,
        page,
        mode = ViewOnlyMode,
        employerContributionsSingle,
        viewOnlyUpdated = false,
        noPageEnabled = false
      )
      result.optViewOnlyDetails.value.heading shouldBe Message("employerContributions.MemberList.viewOnly.singular")
    }

    "viewModel should show 2 Employer Contributions when there are 2 contributions" in {
      val employerContributionsMultiple: List[MemberWithEmployerContributions] = List.fill(2)(
        MemberWithEmployerContributions(
          memberIndex = refineMV(1),
          employerFullName = "Test Member",
          contributions = List(
            EmployerContributions(
              contributionIndex = refineMV(1),
              status = SectionJourneyStatus.Completed
            )
          )
        )
      )

      val result = viewModel(
        srn,
        page,
        mode = ViewOnlyMode,
        employerContributionsMultiple,
        viewOnlyUpdated = false,
        noPageEnabled = false
      )
      result.optViewOnlyDetails.value.heading shouldBe Message(
        "employerContributions.MemberList.viewOnly.plural",
        Message("2")
      )
    }

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[TwoColumnsTripleAction].apply(
        viewModel(srn, page = 1, NormalMode, employerContributions, viewOnlyUpdated = false, noPageEnabled = false)
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "EmployerContributionsMemberListController in view only mode" - {
    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    "must return OK and render the correct view without back link" in {

      val currentUserAnswers = userAnswers
        .unsafeSet(FbVersionPage(srn), "002")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

      val previousUserAnswers = userAnswers
        .unsafeSet(FbVersionPage(srn), "001")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

      val application =
        applicationBuilder(userAnswers = Some(currentUserAnswers), previousUserAnswers = Some(previousUserAnswers))
          .build()

      running(application) {

        val request = FakeRequest(GET, onPreviousViewOnly.url)

        val result = route(application, request).value

        status(result) mustEqual OK

        contentAsString(result) must include("Submitted on")

        (contentAsString(result) must not).include("govuk-back-link")
      }
    }

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              employerContributions,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              noPageEnabled = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(TotalEmployerContributionPage(srn, index, refineMV(1)), money)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              employerContributions,
              viewOnlyUpdated = true,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              noPageEnabled = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    val userAnswersNoEmployerContributions = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
      .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
      .unsafeSet(EmployerContributionsPage(srn), false)
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswersNoEmployerContributions = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    act.like(
      renderView(
        onPageLoadViewOnly,
        userAnswers = userAnswersNoEmployerContributions,
        optPreviousAnswers = Some(previousUserAnswersNoEmployerContributions)
      ) { implicit app => implicit request =>
        injected[TwoColumnsTripleAction].apply(
          viewModel(
            srn,
            page,
            mode = ViewOnlyMode,
            employerContributions = List(
              MemberWithEmployerContributions(
                memberIndex = refineMV(1),
                employerFullName = memberDetails.fullName,
                contributions = Nil
              )
            ),
            viewOnlyUpdated = false,
            optYear = Some(yearString),
            optCurrentVersion = Some(submissionNumberTwo),
            optPreviousVersion = Some(submissionNumberOne),
            compilationOrSubmissionDate = Some(submissionDateTwo),
            noPageEnabled = true
          )
        )
      }.withName("OnPageLoadViewOnly renders ok NO records")
    )

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).withName("Submit redirects to view only tasklist")
    )
  }
}
