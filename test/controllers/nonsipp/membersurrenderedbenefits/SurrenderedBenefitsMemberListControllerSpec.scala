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

package controllers.nonsipp.membersurrenderedbenefits

import pages.nonsipp.memberdetails.{MemberDetailsCompletedPage, MemberDetailsPage}
import views.html.TwoColumnsTripleAction
import eu.timepit.refined.refineMV
import pages.nonsipp.{CompilationOrSubmissionDatePage, FbVersionPage}
import pages.nonsipp.membersurrenderedbenefits.{SurrenderedBenefitsAmountPage, SurrenderedBenefitsPage}
import models._
import controllers.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsMemberListController._
import play.api.test.FakeRequest
import config.RefinedTypes.Max300
import controllers.ControllerBaseSpec
import viewmodels.DisplayMessage.Message
import viewmodels.models.SectionCompleted

import java.time.LocalDate

class SurrenderedBenefitsMemberListControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.SurrenderedBenefitsMemberListController.onPageLoad(srn, page = 1, NormalMode)
  private lazy val onSubmit = routes.SurrenderedBenefitsMemberListController.onSubmit(srn, page = 1, NormalMode)
  private lazy val onSubmitViewOnly = routes.SurrenderedBenefitsMemberListController.onSubmitViewOnly(
    srn,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPageLoadViewOnly = routes.SurrenderedBenefitsMemberListController.onPageLoadViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private lazy val onPreviousViewOnly = routes.SurrenderedBenefitsMemberListController.onPreviousViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )
  private val index = refineMV[Max300.Refined](1)
  private val page = 1

  private val userAnswers: UserAnswers =
    defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
      .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
      .unsafeSet(SurrenderedBenefitsPage(srn), true)
      .unsafeSet(SurrenderedBenefitsAmountPage(srn, refineMV(1)), money)

  private val testMemberList: List[(Max300, NameDOB, Option[SurrenderedBenefitsAmount])] = List(
    (refineMV[Max300.Refined](1), memberDetails, Some(money))
  )

  "SurrenderedBenefitsMemberListController" - {

    "viewModel should show 'Surrendered Benefits ' when there are 0 Surrendered Benefits" in {

      val result = SurrenderedBenefitsMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList = List.empty,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message("surrenderedBenefits.memberList.viewOnly.heading")
    }

    "viewModel should show '1 Surrendered Benefit' when there is 1 Surrendered Benefit" in {
      val result = SurrenderedBenefitsMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        testMemberList,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message("surrenderedBenefits.memberList.viewOnly.singular")
    }

    "viewModel should show '2 Surrendered Benefits' when there is 2 Surrendered Benefit" in {

      val memberDetails1 = NameDOB("testFirstName1", "testLastName1", LocalDate.of(1990, 12, 12))
      val memberDetails2 = NameDOB("testFirstName2", "testLastName2", LocalDate.of(1990, 12, 12))

      val memberList: List[(Max300, NameDOB, Option[SurrenderedBenefitsAmount])] = List(
        (refineMV[Max300.Refined](1), memberDetails1, Some(money)),
        (refineMV[Max300.Refined](1), memberDetails2, Some(money))
      )

      val result = SurrenderedBenefitsMemberListController.viewModel(
        srn,
        page = 1,
        ViewOnlyMode,
        memberList,
        viewOnlyUpdated = false,
        schemeName = schemeName,
        noPageEnabled = false
      )

      result.optViewOnlyDetails.value.heading mustBe Message(
        "surrenderedBenefits.memberList.viewOnly.plural",
        Message("2")
      )
    }

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[TwoColumnsTripleAction].apply(
        viewModel(
          srn,
          page = 1,
          NormalMode,
          testMemberList,
          viewOnlyUpdated = false,
          schemeName = schemeName,
          noPageEnabled = false
        )
      )
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  "SurrenderedBenefitsMemberListControllerSpec in view only mode" - {

    val currentUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateTwo)

    val previousUserAnswers = userAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompilationOrSubmissionDatePage(srn), submissionDateOne)

    "must return OK and render the correct view without back link" in {

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
              testMemberList,
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

    act.like {
      val updatedUserAnswers = currentUserAnswers
        .unsafeSet(SurrenderedBenefitsAmountPage(srn, index), otherMoney)

      val memberList: List[(Max300, NameDOB, Option[SurrenderedBenefitsAmount])] = List(
        (refineMV[Max300.Refined](1), memberDetails, Some(otherMoney))
      )
      renderView(onPageLoadViewOnly, userAnswers = updatedUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              memberList,
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
    }

    act.like {
      val noUserAnswers = currentUserAnswers
        .unsafeSet(SurrenderedBenefitsPage(srn), false)
        .unsafeRemove(SurrenderedBenefitsAmountPage(srn, refineMV(1)))

      renderView(onPageLoadViewOnly, userAnswers = noUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[TwoColumnsTripleAction].apply(
            viewModel(
              srn,
              page,
              mode = ViewOnlyMode,
              List.empty,
              viewOnlyUpdated = true,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              schemeName = schemeName,
              noPageEnabled = true
            )
          )
      }.withName("OnPageLoadViewOnly renders ok NO records")
    }

    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).withName("Submit redirects to view only tasklist")
    )

  }
}
