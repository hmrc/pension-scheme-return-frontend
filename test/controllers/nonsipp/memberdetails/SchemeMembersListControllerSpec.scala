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

import play.api.test.FakeRequest
import services.PsrSubmissionService
import pages.nonsipp.membercontributions.MemberContributionsPage
import play.api.mvc.Call
import models.ManualOrUpload.{Manual, Upload}
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import play.api.inject.bind
import views.html.ListView
import utils.IntUtils.given
import eu.timepit.refined._
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import forms.YesNoPageFormProvider
import models.{NormalMode, UserAnswers, ViewOnlyMode}
import controllers.nonsipp.memberdetails.SchemeMembersListController._
import viewmodels.models.{MemberState, SectionCompleted, SectionJourneyStatus}
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails._
import org.mockito.Mockito._
import config.RefinedTypes.OneTo300
import controllers.{ControllerBaseSpec, ControllerBehaviours}

class SchemeMembersListControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

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
    .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
    .unsafeSet(MemberStatus(srn, 1), MemberState.New)
    .unsafeSet(MemberDetailsCompletedPage(srn, 1), SectionCompleted)
    .unsafeSet(MemberDetailsManualProgress(srn, 1), SectionJourneyStatus.Completed)

  private val userAnswersWith300MembersDetails =
    (1 to 300).foldLeft(defaultUserAnswers)((ua, i) =>
      ua.unsafeSet(MemberDetailsPage(srn, refineV[OneTo300](i).value), memberDetails)
        .unsafeSet(MemberStatus(srn, refineV[OneTo300](i).value), MemberState.New)
    )

  private val index = 1

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  val userAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, 1), SectionCompleted)
    .unsafeSet(MemberDetailsManualProgress(srn, 1), SectionJourneyStatus.Completed)
    .unsafeSet(MemberContributionsPage(srn), true)

  private val userAnswersToCheck = userAnswers
    .unsafeSet(MembersDetailsChecked(srn), false)

  private val userAnswersChecked = userAnswers
    .unsafeSet(MembersDetailsChecked(srn), true)

  "SchemeMembersListController" - {
    "incomplete members must be filtered" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
        .unsafeSet(MemberDetailsCompletedPage(srn, 1), SectionCompleted)
        .unsafeSet(MemberDetailsPage(srn, 2), memberDetails)

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
        renderView(onPageLoadManual, userAnswersWithMembersDetails)(implicit app =>
          implicit request =>
            injected[ListView].apply(
              form(injected[YesNoPageFormProvider], Manual),
              viewModel(
                srn,
                1,
                Manual,
                NormalMode,
                List((1, ((index - 1).toString, memberDetails.fullName))),
                viewOnlyUpdated = false
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
        ).before(MockPsrSubmissionService.submitPsrDetailsWithUA())
          .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
      )

      "when user answers has 300 members" - {
        act.like(
          redirectToPage(
            onSubmitManual,
            routes.HowToUploadController.onPageLoad(srn),
            userAnswersWith300MembersDetails,
            "value" -> "true"
          ).before(MockPsrSubmissionService.submitPsrDetailsWithUA())
            .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
        )
      }

      act.like(
        redirectNextPage(onSubmitManual, "value" -> "false")
          .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
          .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
      )

      act.like(invalidForm(onSubmitManual))
      act.like(journeyRecoveryPage(onSubmitManual).updateName("onSubmit" + _))
    }

    "on Upload" - {
      act.like(
        renderView(onPageLoadUpload, userAnswersWithMembersDetails)(implicit app =>
          implicit request =>
            injected[ListView].apply(
              form(injected[YesNoPageFormProvider], Upload),
              viewModel(
                srn,
                1,
                Upload,
                NormalMode,
                List((1, ((index - 1).toString, memberDetails.fullName))),
                viewOnlyUpdated = false
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
        ).before(MockPsrSubmissionService.submitPsrDetailsWithUA())
          .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
      )

      "when user answers has more than 300 members" - {
        act.like(
          redirectToPage(
            onSubmitUpload,
            routes.HowToUploadController.onPageLoad(srn),
            userAnswersWith300MembersDetails,
            "value" -> "true"
          ).before(MockPsrSubmissionService.submitPsrDetailsWithUA())
            .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
        )
      }

      act.like(
        redirectNextPage(onSubmitUpload, "value" -> "false")
          .before(MockPsrSubmissionService.submitPsrDetailsWithUA())
          .after(MockPsrSubmissionService.verify.submitPsrDetailsWithUA(times(0)))
      )

      act.like(invalidForm(onSubmitUpload))
      act.like(journeyRecoveryPage(onSubmitUpload).updateName("onSubmit" + _))
    }

    "view only mode" - {

      val currentUserAnswers = defaultUserAnswers
        .unsafeSet(FbVersionPage(srn), "002")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)
        .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
        .unsafeSet(MemberDetailsCompletedPage(srn, 1), SectionCompleted)
        .unsafeSet(MemberDetailsManualProgress(srn, 1), SectionJourneyStatus.Completed)

      val previousUserAnswers = currentUserAnswers
        .unsafeSet(FbVersionPage(srn), "001")
        .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)
        .unsafeSet(MemberDetailsPage(srn, 1), memberDetails)
        .unsafeSet(MemberDetailsCompletedPage(srn, 1), SectionCompleted)

      act.like(
        renderView(
          onPageLoadViewOnly,
          userAnswers = currentUserAnswers,
          optPreviousAnswers = Some(previousUserAnswers)
        ) { implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider], Upload),
            viewModel(
              srn = srn,
              page = 1,
              manualOrUpload = Upload,
              mode = ViewOnlyMode,
              filteredMembers = List((1, ((index - 1).toString, memberDetails.fullName))),
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
        }.withName("OnPageLoadViewOnly renders ok with viewOnlyUpdated false")
      )

      val updatedUserAnswers = currentUserAnswers
        .unsafeSet(MemberDetailsNinoPage(srn, 1), nino)

      act.like(
        renderView(
          onPageLoadViewOnly,
          userAnswers = updatedUserAnswers,
          optPreviousAnswers = Some(previousUserAnswers)
        ) { implicit app => implicit request =>
          injected[ListView].apply(
            form(injected[YesNoPageFormProvider], Upload),
            viewModel(
              srn = srn,
              page = 1,
              manualOrUpload = Upload,
              mode = ViewOnlyMode,
              filteredMembers = List((1, ((index - 1).toString, memberDetails.fullName))),
              viewOnlyUpdated = true,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
        }.withName("OnPageLoadViewOnly renders ok with viewOnlyUpdated true")
      )

      act.like(
        redirectToPage(
          onSubmitViewOnly,
          controllers.nonsipp.routes.ViewOnlyTaskListController
            .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
        ).withName("Submit redirects to view only tasklist")
      )

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

    }

    "Check scenario" - {
      act.like(
        renderViewWithPrePopSession(onPageLoadManual, userAnswersToCheck)(implicit app =>
          implicit request =>
            injected[ListView].apply(
              form(injected[YesNoPageFormProvider], Manual),
              viewModel(
                srn,
                1,
                Manual,
                NormalMode,
                List((1, ((index - 1).toString, memberDetails.fullName))),
                viewOnlyUpdated = false,
                prePopNotChecked = true
              )
            )
        ).withName("OnPageLoad with checked = Some(false) renders ok with check messsage")
      )

      act.like(
        renderViewWithPrePopSession(onPageLoadManual, userAnswersChecked)(implicit app =>
          implicit request =>
            injected[ListView].apply(
              form(injected[YesNoPageFormProvider], Manual),
              viewModel(
                srn,
                1,
                Manual,
                NormalMode,
                List((1, ((index - 1).toString, memberDetails.fullName))),
                viewOnlyUpdated = false,
                prePopNotChecked = false
              )
            )
        ).withName("OnPageLoad with checked = Some(false) renders ok with normal radios")
      )

    }
  }
}
