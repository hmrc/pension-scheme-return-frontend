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

package controllers.nonsipp.memberpensionpayments

import services.PsrSubmissionService
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import config.Refined.Max300
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.TwoColumnsTripleAction
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import forms.YesNoPageFormProvider
import models._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import org.mockito.Mockito._
import pages.nonsipp.memberpensionpayments.{
  MemberPensionPaymentsListPage,
  PensionPaymentsReceivedPage,
  TotalAmountPensionPaymentsPage
}
import eu.timepit.refined.refineMV
import viewmodels.DisplayMessage.Message
import viewmodels.models.SectionCompleted

import java.time.LocalDate

class MemberPensionPaymentsListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.MemberPensionPaymentsListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.MemberPensionPaymentsListController.onSubmit(srn, page = 1, NormalMode)
  private lazy val onSubmitViewOnly = routes.MemberPensionPaymentsListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.MemberPensionPaymentsListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.MemberPensionPaymentsListController.onPreviousViewOnly(
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
    .unsafeSet(PensionPaymentsReceivedPage(srn), true)

  "MemberPensionPaymentsListController" - {

    "viewModel should show 'Pension payments' when there are 0 payments" in {
      val userAnswersWithNoPayments = userAnswers
        .unsafeSet(PensionPaymentsReceivedPage(srn), false)

      val memberList: List[Option[NameDOB]] = List.empty

      val result = MemberPensionPaymentsListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithNoPayments,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = true,
        showBackLink = true
      )

      result.optViewOnlyDetails.value.heading mustBe Message("memberPensionPayments.memberList.viewOnly.heading")
      result.page.rows.size mustBe 0
    }

    "viewModel should show 1 pension payment when there is 1 payment" in {
      val memberDetails1 = NameDOB("testFirstName1", "testLastName1", LocalDate.of(1990, 12, 12))

      val userAnswersWithOnePayment = userAnswers
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails1)
        .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
        .unsafeSet(TotalAmountPensionPaymentsPage(srn, refineMV(1)), money)
        .unsafeSet(PensionPaymentsReceivedPage(srn), true)

      val memberList: List[Option[NameDOB]] = List(Some(memberDetails1))

      val result = MemberPensionPaymentsListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithOnePayment,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = false,
        showBackLink = true
      )

      result.optViewOnlyDetails.value.heading mustBe Message("memberPensionPayments.memberList.viewOnly.singular")

      result.page.rows.size mustBe 1
    }

    "viewModel should show 2 pension payments when there are 2 payments" in {
      val memberDetails1 = NameDOB("testFirstName1", "testLastName1", LocalDate.of(1990, 12, 12))
      val memberDetails2 = NameDOB("testFirstName2", "testLastName2", LocalDate.of(1991, 6, 15))

      val userAnswersWithTwoPayments = userAnswers
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails1)
        .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
        .unsafeSet(TotalAmountPensionPaymentsPage(srn, refineMV(1)), money)
        .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails2)
        .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(2)), SectionCompleted)
        .unsafeSet(TotalAmountPensionPaymentsPage(srn, refineMV(2)), money)
        .unsafeSet(PensionPaymentsReceivedPage(srn), true)

      val memberList: List[Option[NameDOB]] = List(Some(memberDetails1), Some(memberDetails2))

      val result = MemberPensionPaymentsListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        userAnswersWithTwoPayments,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = false,
        showBackLink = true
      )

      result.optViewOnlyDetails.value.heading mustBe Message(
        "memberPensionPayments.memberList.viewOnly.plural",
        Message("2")
      )
    }

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val memberList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

      injected[TwoColumnsTripleAction].apply(
        MemberPensionPaymentsListController.form(injected[YesNoPageFormProvider]),
        MemberPensionPaymentsListController.viewModel(
          srn,
          page = 1,
          NormalMode,
          memberList,
          userAnswers,
          viewOnlyUpdated = false,
          schemeName = schemeName,
          noPageEnabled = false,
          showBackLink = true
        )
      )
    })

    act.like(renderPrePopView(onPageLoad, MemberPensionPaymentsListPage(srn), true, userAnswers) {
      implicit app => implicit request =>
        val memberList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

        injected[TwoColumnsTripleAction]
          .apply(
            MemberPensionPaymentsListController.form(injected[YesNoPageFormProvider]).fill(true),
            MemberPensionPaymentsListController.viewModel(
              srn,
              page = 1,
              NormalMode,
              memberList,
              userAnswers,
              viewOnlyUpdated = false,
              schemeName = schemeName,
              noPageEnabled = false,
              showBackLink = true
            )
          )
    })

    act.like(
      redirectNextPage(onSubmit, "value" -> "true")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
    )

    act.like(
      redirectNextPage(onSubmit, "value" -> "false")
        .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
        .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "MemberPensionPaymentsListController in view only mode" - {

    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          val memberList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

          injected[TwoColumnsTripleAction].apply(
            MemberPensionPaymentsListController.form(injected[YesNoPageFormProvider]),
            MemberPensionPaymentsListController.viewModel(
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
              schemeName = schemeName,
              noPageEnabled = false,
              showBackLink = true
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(TotalAmountPensionPaymentsPage(srn, index), money)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          val memberList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

          injected[TwoColumnsTripleAction].apply(
            MemberPensionPaymentsListController.form(injected[YesNoPageFormProvider]),
            MemberPensionPaymentsListController.viewModel(
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
              schemeName = schemeName,
              noPageEnabled = false,
              showBackLink = true
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    val noUserAnswers = currentUserAnswers
      .unsafeSet(PensionPaymentsReceivedPage(srn), false)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = noUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            MemberPensionPaymentsListController.form(injected[YesNoPageFormProvider]),
            MemberPensionPaymentsListController.viewModel(
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
              schemeName = schemeName,
              noPageEnabled = true,
              showBackLink = true
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
