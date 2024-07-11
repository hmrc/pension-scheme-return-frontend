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

package controllers.nonsipp.memberdetails

import services.PsrSubmissionService
import play.api.mvc.Call
import models.ManualOrUpload.{Manual, Upload}
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import config.Refined.OneTo300
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.ListView
import eu.timepit.refined._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import forms.YesNoPageFormProvider
import models.{NameDOB, NormalMode, ViewOnlyMode}
import controllers.nonsipp.memberdetails.SchemeMembersListController._
import viewmodels.models.{MemberState, SectionCompleted}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails._
import org.mockito.Mockito._

class SchemeMembersListControllerSpec extends ControllerBaseSpec {

  lazy val onPageLoadManual: Call = routes.SchemeMembersListController.onPageLoad(srn, 1, Manual)
  lazy val onPageLoadUpload: Call = routes.SchemeMembersListController.onPageLoad(srn, 1, Upload)
  lazy val onPageLoadViewOnly: Call = routes.SchemeMembersListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  lazy val onPreviousViewOnly: Call = routes.SchemeMembersListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  lazy val onSubmitManual: Call = routes.SchemeMembersListController.onSubmit(srn, 1, Manual)
  lazy val onSubmitUpload: Call = routes.SchemeMembersListController.onSubmit(srn, 1, Upload)
  lazy val onSubmitViewOnly: Call = routes.SchemeMembersListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val userAnswersWithMembersDetails = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberStatus(srn, refineMV(1)), MemberState.New)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)

  private val userAnswersWith300MembersDetails =
    (1 to 300).foldLeft(defaultUserAnswers)(
      (ua, i) =>
        ua.unsafeSet(MemberDetailsPage(srn, refineV[OneTo300](i).value), memberDetails)
          .unsafeSet(MemberStatus(srn, refineV[OneTo300](i).value), MemberState.New)
    )

  private val index = 1

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "SchemeMembersListController" - {
    "incomplete members must be filtered" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
        .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
        .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)

      val completedMembers = userAnswers.get(MembersDetailsCompletedPages(srn)).getOrElse(Map.empty)
      val unfilteredMemberDetails = userAnswers.membersDetails(srn)
      val filteredMemberDetails = completedMembers.keySet
        .intersect(unfilteredMemberDetails.keySet)
        .map(k => k -> unfilteredMemberDetails(k))
        .toMap

      unfilteredMemberDetails.size must be > filteredMemberDetails.size
    }

    "on Manual" - {
      act.like(
        renderView(onPageLoadManual, userAnswersWithMembersDetails)(
          implicit app =>
            implicit request =>
              injected[ListView].apply(
                form(injected[YesNoPageFormProvider], Manual),
                viewModel(
                  srn,
                  1,
                  Manual,
                  NormalMode,
                  List((refineMV(1), ((index - 1).toString, memberDetails.fullName))),
                  false
                )
              )
        )
      )

      act.like(
        redirectToPage(
          onPageLoadManual,
          routes.PensionSchemeMembersController.onPageLoad(srn),
          defaultUserAnswers
        )
      )

      act.like(journeyRecoveryPage(onPageLoadManual).updateName("onPageLoad" + _))

      act.like(
        redirectToPage(
          onSubmitManual,
          routes.PensionSchemeMembersController.onPageLoad(srn),
          userAnswersWithMembersDetails,
          "value" -> "true"
        )
      )

      "when user answers has 300 members" - {
        act.like(
          redirectToPage(
            onSubmitManual,
            routes.HowToUploadController.onPageLoad(srn),
            userAnswersWith300MembersDetails,
            "value" -> "true"
          )
        )
      }

      act.like(redirectNextPage(onSubmitManual, "value" -> "false"))
      act.like(invalidForm(onSubmitManual))
      act.like(journeyRecoveryPage(onSubmitManual).updateName("onSubmit" + _))
    }

    "on Upload" - {
      act.like(
        renderView(onPageLoadUpload, userAnswersWithMembersDetails)(
          implicit app =>
            implicit request =>
              injected[ListView].apply(
                form(injected[YesNoPageFormProvider], Upload),
                viewModel(
                  srn,
                  1,
                  Upload,
                  NormalMode,
                  List((refineMV(1), ((index - 1).toString, memberDetails.fullName))),
                  false
                )
              )
        )
      )

      act.like(
        redirectToPage(
          onPageLoadUpload,
          routes.PensionSchemeMembersController.onPageLoad(srn),
          defaultUserAnswers
        )
      )

      act.like(journeyRecoveryPage(onPageLoadUpload).updateName("onPageLoad" + _))

      act.like(
        redirectToPage(
          onSubmitUpload,
          routes.PensionSchemeMembersController.onPageLoad(srn),
          userAnswersWithMembersDetails,
          "value" -> "true"
        )
      )

      "when user answers has more than 300 members" - {
        act.like(
          redirectToPage(
            onSubmitUpload,
            routes.HowToUploadController.onPageLoad(srn),
            userAnswersWith300MembersDetails,
            "value" -> "true"
          )
        )
      }

      act.like(redirectNextPage(onSubmitUpload, "value" -> "false"))
      act.like(invalidForm(onSubmitUpload))
      act.like(journeyRecoveryPage(onSubmitUpload).updateName("onSubmit" + _))
    }

    "view only mode" - {

      val currentUserAnswers = defaultUserAnswers
        .unsafeSet(FbVersionPage(srn), "002")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
        .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)

      val previousUserAnswers = currentUserAnswers
        .unsafeSet(FbVersionPage(srn), "001")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)
        .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
        .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)

      act.like(
        renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
          implicit app => implicit request =>
            val memberList: List[Option[NameDOB]] = currentUserAnswers.membersOptionList(srn)

            injected[ListView].apply(
              form(injected[YesNoPageFormProvider], Upload),
              viewModel(
                srn = srn,
                page = 1,
                manualOrUpload = Upload,
                mode = ViewOnlyMode,
                filteredMembers = List((refineMV(1), ((index - 1).toString, memberDetails.fullName))),
                viewOnlyUpdated = false,
                optYear = Some(yearString),
                optCurrentVersion = Some(submissionNumberTwo),
                optPreviousVersion = Some(submissionNumberOne),
                compilationOrSubmissionDate = Some(submissionDateTwo)
              )
            )
        }.withName("OnPageLoadViewOnly renders ok with no changed flag")
      )

      val updatedUserAnswers = currentUserAnswers

      act.like(
        renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
          implicit app => implicit request =>
            val memberList: List[Option[NameDOB]] = updatedUserAnswers.membersOptionList(srn)

            injected[ListView].apply(
              form(injected[YesNoPageFormProvider], Upload),
              viewModel(
                srn = srn,
                page = 1,
                manualOrUpload = Upload,
                mode = ViewOnlyMode,
                filteredMembers = List((refineMV(1), ((index - 1).toString, memberDetails.fullName))),
                viewOnlyUpdated = false,
                optYear = Some(yearString),
                optCurrentVersion = Some(submissionNumberTwo),
                optPreviousVersion = Some(submissionNumberOne),
                compilationOrSubmissionDate = Some(submissionDateTwo)
              )
            )
        }.withName("OnPageLoadViewOnly renders ok with changed flag")
      )

      act.like(
        redirectToPage(
          onSubmitViewOnly,
          controllers.nonsipp.routes.ViewOnlyTaskListController
            .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
        ).after(
            verify(mockPsrSubmissionService, never()).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
          )
          .withName("Submit redirects to view only tasklist")
      )

      act.like(
        redirectToPage(
          onPreviousViewOnly,
          controllers.nonsipp.membercontributions.routes.MemberContributionListController
            .onPageLoadViewOnly(srn, 1, yearString, submissionNumberOne, submissionNumberZero)
        ).withName(
          "Submit previous view only redirects to the controller with parameters for the previous submission"
        )
      )
    }
  }
}
