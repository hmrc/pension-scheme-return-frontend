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

package controllers.nonsipp.receivetransfer

import services.PsrSubmissionService
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.TwoColumnsTripleAction
import pages.nonsipp.receivetransfer.{DidSchemeReceiveTransferPage, TransferReceivedMemberListPage}
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import forms.YesNoPageFormProvider
import models._
import viewmodels.models.SectionCompleted
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import org.mockito.Mockito._
import controllers.nonsipp.receivetransfer.TransferReceivedMemberListController._
import eu.timepit.refined.refineMV

import scala.concurrent.Future

class TransferReceivedMemberListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.TransferReceivedMemberListController.onPageLoad(srn, page, NormalMode)
  private lazy val onSubmit = routes.TransferReceivedMemberListController.onSubmit(srn, page, NormalMode)
  private lazy val onSubmitViewOnly = routes.TransferReceivedMemberListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.TransferReceivedMemberListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.TransferReceivedMemberListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val page = 1

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  val userAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(DidSchemeReceiveTransferPage(srn), true)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "TransferReceivedMemberListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val memberList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

      injected[TwoColumnsTripleAction].apply(
        form(injected[YesNoPageFormProvider]),
        viewModel(
          srn,
          page,
          NormalMode,
          memberList,
          userAnswers,
          viewOnlyUpdated = false,
          schemeName = schemeName,
          noPageEnabled = false
        )
      )
    })

    act.like(renderPrePopView(onPageLoad, TransferReceivedMemberListPage(srn), true, userAnswers) {
      implicit app => implicit request =>
        val memberList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

        injected[TwoColumnsTripleAction]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(
              srn,
              page,
              NormalMode,
              memberList,
              userAnswers,
              viewOnlyUpdated = false,
              schemeName = schemeName,
              noPageEnabled = false
            )
          )
    })

    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, userAnswers, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(invalidForm(onSubmit, userAnswers))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "TransferReceivedMemberListController in view only mode" - {

    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          val memberList: List[Option[NameDOB]] = currentUserAnswers.membersOptionList(srn)

          injected[TwoColumnsTripleAction].apply(
            TransferReceivedMemberListController.form(injected[YesNoPageFormProvider]),
            TransferReceivedMemberListController.viewModel(
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
              noPageEnabled = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )

    val updatedUserAnswers = currentUserAnswers
      .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          val memberList: List[Option[NameDOB]] = updatedUserAnswers.membersOptionList(srn)

          injected[TwoColumnsTripleAction].apply(
            TransferReceivedMemberListController.form(injected[YesNoPageFormProvider]),
            TransferReceivedMemberListController.viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              memberList,
              updatedUserAnswers,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              schemeName = schemeName,
              noPageEnabled = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with changed flag")
    )

    val noUserAnswers = currentUserAnswers.unsafeSet(DidSchemeReceiveTransferPage(srn), false)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = noUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            TransferReceivedMemberListController.form(injected[YesNoPageFormProvider]),
            TransferReceivedMemberListController.viewModel(
              srn,
              page = 1,
              mode = ViewOnlyMode,
              List.empty,
              noUserAnswers,
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              schemeName = schemeName,
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
        .withName("Submit redirects to view only taskList")
    )

    act.like(
      redirectToPage(
        onPreviousViewOnly,
        controllers.nonsipp.receivetransfer.routes.TransferReceivedMemberListController
          .onPageLoadViewOnly(srn, 1, yearString, submissionNumberOne, submissionNumberZero)
      ).withName(
        "Submit previous view only redirects to the controller with parameters for the previous submission"
      )
    )
  }

}
