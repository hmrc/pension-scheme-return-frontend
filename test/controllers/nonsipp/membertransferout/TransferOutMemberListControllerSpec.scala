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

package controllers.nonsipp.membertransferout

import services.PsrSubmissionService
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import config.Refined.{Max300, Max5}
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.TwoColumnsTripleAction
import eu.timepit.refined.refineMV
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import forms.YesNoPageFormProvider
import models._
import pages.nonsipp.membertransferout.{SchemeTransferOutPage, TransferOutMemberListPage, WhenWasTransferMadePage}
import viewmodels.models.SectionCompleted
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import controllers.nonsipp.membertransferout.TransferOutMemberListController._
import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import org.mockito.Mockito._

import scala.concurrent.Future

class TransferOutMemberListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.TransferOutMemberListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.TransferOutMemberListController.onSubmit(srn, page = 1, NormalMode)
  private lazy val onSubmitViewOnly = routes.TransferOutMemberListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.TransferOutMemberListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.TransferOutMemberListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private val index = refineMV[Max300.Refined](1)
  private val page = 1

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  val userAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(SchemeTransferOutPage(srn), true)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "TransferOutMemberListController" - {

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

    act.like(renderPrePopView(onPageLoad, TransferOutMemberListPage(srn), true, userAnswers) {
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

    "TransferOutMemberListControllerSpec in view only mode" - {
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
              form(injected[YesNoPageFormProvider]),
              viewModel(
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
        .unsafeSet(WhenWasTransferMadePage(srn, index, refineMV[Max5.Refined](1)), localDate)

      act.like(
        renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
          implicit app => implicit request =>
            val memberList: List[Option[NameDOB]] = userAnswers.membersOptionList(srn)

            injected[TwoColumnsTripleAction].apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(
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
                noPageEnabled = false
              )
            )
        }.withName("OnPageLoadViewOnly renders ok with changed flag")
      )

      val noUserAnswers = currentUserAnswers.unsafeSet(SchemeTransferOutPage(srn), false)

      act.like(
        renderView(onPageLoadViewOnly, userAnswers = noUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
          implicit app => implicit request =>
            injected[TwoColumnsTripleAction].apply(
              form(injected[YesNoPageFormProvider]),
              viewModel(
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

      act.like(
        redirectToPage(
          onPreviousViewOnly,
          controllers.nonsipp.membertransferout.routes.TransferOutMemberListController
            .onPageLoadViewOnly(srn, 1, yearString, submissionNumberOne, submissionNumberZero)
        ).withName(
          "Submit previous view only redirects to the controller with parameters for the previous submission"
        )
      )
    }
  }
}
