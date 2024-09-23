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
import pages.nonsipp.membercontributions.{
  MemberContributionsListPage,
  MemberContributionsPage,
  TotalMemberContributionPage
}
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import config.Refined.Max300
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.TwoColumnsTripleAction
import eu.timepit.refined.refineMV
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import forms.YesNoPageFormProvider
import models._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import org.mockito.Mockito._
import viewmodels.DisplayMessage.Message
import viewmodels.models.SectionCompleted

import scala.concurrent.Future

import java.time.LocalDate

class MemberContributionListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.MemberContributionListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.MemberContributionListController.onSubmit(srn, page = 1, NormalMode)
  private lazy val onSubmitViewOnly = routes.MemberContributionListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.MemberContributionListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.MemberContributionListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private val index = refineMV[Max300.Refined](1)
  private val page = 1

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )
  val userAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(MemberContributionsPage(srn), true)

  "MemberContributionListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val memberList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

      injected[TwoColumnsTripleAction].apply(
        MemberContributionListController.form(injected[YesNoPageFormProvider]),
        MemberContributionListController.viewModel(
          srn,
          page = 1,
          NormalMode,
          memberList,
          userAnswers,
          viewOnlyUpdated = false,
          noPageEnabled = false
        )
      )
    })

    act.like(renderPrePopView(onPageLoad, MemberContributionsListPage(srn), true, userAnswers) {
      implicit app => implicit request =>
        val memberList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

        injected[TwoColumnsTripleAction]
          .apply(
            MemberContributionListController.form(injected[YesNoPageFormProvider]).fill(true),
            MemberContributionListController.viewModel(
              srn,
              page = 1,
              NormalMode,
              memberList,
              userAnswers,
              viewOnlyUpdated = false,
              noPageEnabled = false
            )
          )
    })

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .before(
          when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(())))
        )
        .after({
          verify(mockPsrSubmissionService, times(1)).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .before(
          when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
            .thenReturn(Future.successful(Some(())))
        )
        .after({
          verify(mockPsrSubmissionService, never).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
          reset(mockPsrSubmissionService)
        })
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "MemberContributionListController in view only mode" - {

    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    "ViewModel should display 'Member Contributions' when there are 0 member contributions" in {
      val userAnswersWithNoContributions = userAnswers
        .unsafeSet(MemberContributionsPage(srn), true)
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
        .unsafeSet(TotalMemberContributionPage(srn, refineMV(1)), Money(0))

      val memberList: List[Option[NameDOB]] = userAnswersWithNoContributions.membersOptionList(srn)

      val result = MemberContributionListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithNoContributions,
        viewOnlyUpdated = false,
        optYear = Some(yearString),
        optCurrentVersion = Some(submissionNumberTwo),
        optPreviousVersion = Some(submissionNumberOne),
        compilationOrSubmissionDate = Some(submissionDateTwo),
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message("ReportContribution.MemberList.viewOnly.noContributions")
      result.page.rows.size mustBe 1
    }

    "ViewModel should display 'Member contribution' for 1 member contribution" in {
      val userAnswersWithOneContribution = userAnswers
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
        .unsafeSet(TotalMemberContributionPage(srn, refineMV(1)), Money(1))

      val memberList: List[Option[NameDOB]] = userAnswersWithOneContribution.membersOptionList(srn)

      val result = MemberContributionListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithOneContribution,
        viewOnlyUpdated = false,
        optYear = Some(yearString),
        optCurrentVersion = Some(submissionNumberTwo),
        optPreviousVersion = Some(submissionNumberOne),
        compilationOrSubmissionDate = Some(submissionDateTwo),
        noPageEnabled = false
      )
      result.optViewOnlyDetails.value.heading mustBe Message("ReportContribution.MemberList.viewOnly.singular")

    }

    "ViewModel should display '2 member contributions' for 2 member contributions" in {
      val memberDetails1 = NameDOB("testFirstName1", "testLastName1", LocalDate.of(1990, 12, 12))
      val memberDetails2 = NameDOB("testFirstName2", "testLastName2", LocalDate.of(1991, 6, 15))

      val userAnswersWithTwoContributions = userAnswers
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails1)
        .unsafeSet(TotalMemberContributionPage(srn, refineMV(1)), Money(10))
        .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails2)
        .unsafeSet(TotalMemberContributionPage(srn, refineMV(2)), Money(20))

      val memberList: List[Option[NameDOB]] = List(Some(memberDetails1), Some(memberDetails2))

      val result = MemberContributionListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithTwoContributions,
        viewOnlyUpdated = false,
        optYear = Some(yearString),
        optCurrentVersion = Some(submissionNumberTwo),
        optPreviousVersion = Some(submissionNumberOne),
        compilationOrSubmissionDate = Some(submissionDateTwo),
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message(
        "ReportContribution.MemberList.viewOnly.plural",
        Message("2")
      )

    }

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          val memberList: List[Option[NameDOB]] = currentUserAnswers.membersOptionList(srn)

          injected[TwoColumnsTripleAction].apply(
            MemberContributionListController.form(injected[YesNoPageFormProvider]),
            MemberContributionListController.viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              memberList,
              currentUserAnswers,
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
      .unsafeSet(TotalMemberContributionPage(srn, index), money)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          val memberList: List[Option[NameDOB]] = updatedUserAnswers.membersOptionList(srn)

          injected[TwoColumnsTripleAction].apply(
            MemberContributionListController.form(injected[YesNoPageFormProvider]),
            MemberContributionListController.viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              memberList,
              updatedUserAnswers,
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

    val noUserAnswers = currentUserAnswers.unsafeSet(MemberContributionsPage(srn), false)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = noUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            MemberContributionListController.form(injected[YesNoPageFormProvider]),
            MemberContributionListController.viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              List.empty,
              noUserAnswers,
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
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only tasklist")
    )

  }
}
