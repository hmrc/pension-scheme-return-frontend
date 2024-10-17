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
import controllers.nonsipp.memberdetails.SchemeMemberDetailsAnswersController._
import play.api.inject.bind
import views.html.CheckYourAnswersView
import cats.implicits.catsSyntaxOptionId
import eu.timepit.refined.refineMV
import pages.nonsipp.FbVersionPage
import models._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails._
import org.mockito.Mockito._
import config.RefinedTypes.Max300
import controllers.ControllerBaseSpec
import viewmodels.DisplayMessage.Message
import viewmodels.models.{MemberState, SectionCompleted}

import scala.concurrent.Future

class SchemeMemberDetailsAnswersControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val page = 1

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  private def onPageLoad(mode: Mode) =
    routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, index, mode)

  private def onSubmit(mode: Mode) =
    routes.SchemeMemberDetailsAnswersController.onSubmit(srn, index, mode)

  private lazy val onPageLoadViewOnly = routes.SchemeMemberDetailsAnswersController.onPageLoadViewOnly(
    srn,
    index,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onSubmitViewOnly = routes.SchemeMemberDetailsAnswersController.onSubmitViewOnly(
    srn,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private val noNinoReason = "test reason"

  private val userAnswersWithNino = defaultUserAnswers
    .unsafeSet(FbVersionPage(srn), "002")
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, index), SectionCompleted)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
    .unsafeSet(MemberDetailsNinoPage(srn, index), nino)

  private val userAnswersWithoutNino = defaultUserAnswers
    .unsafeSet(FbVersionPage(srn), "001")
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(MemberDetailsCompletedPage(srn, refineMV(1)), SectionCompleted)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index), false)
    .unsafeSet(NoNINOPage(srn, index), noNinoReason)

  "SchemeMemberDetailsCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), userAnswersWithNino)(
          implicit app =>
            implicit request =>
              injected[CheckYourAnswersView].apply(
                viewModel(
                  index,
                  srn,
                  mode,
                  memberDetails,
                  hasNINO = true,
                  Some(nino),
                  None,
                  viewOnlyUpdated = true
                )
              )
        ).withName(s"render correct $mode view when nino provided")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersWithoutNino)(
          implicit app =>
            implicit request =>
              injected[CheckYourAnswersView].apply(
                viewModel(
                  index,
                  srn,
                  mode,
                  memberDetails,
                  hasNINO = false,
                  None,
                  Some(noNinoReason),
                  viewOnlyUpdated = true
                )
              )
        ).withName(s"render the correct $mode view when no nino provided")
      )

      act.like(
        redirectToPage(
          onPageLoad(mode),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(MemberDetailsPage(srn, index)).success.value
        ).withName(s"when member details are missing in $mode")
      )

      act.like(
        redirectToPage(
          onPageLoad(mode),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(DoesMemberHaveNinoPage(srn, index)).success.value
        ).withName(s"when does member have NINO is missing in $mode")
      )

      act.like(
        redirectToPage(
          onPageLoad(mode),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(MemberDetailsNinoPage(srn, index)).success.value
        ).withName(s"when NINO is missing in $mode")
      )

      act.like(
        redirectToPage(
          onPageLoad(mode),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithoutNino.remove(NoNINOPage(srn, index)).success.value
        ).withName(s"when no NINO reason is missing in $mode")
      )

      act.like(journeyRecoveryPage(onPageLoad(mode)).updateName(s"onPageLoad on $mode " + _))
      act.like(journeyRecoveryPage(onSubmit(mode)).updateName(s"onSubmit on $mode " + _))
    }

    "Member status" - {

      "on Check" - {

        act.like(
          saveAndContinue(
            onSubmit(NormalMode),
            userAnswersWithNino,
            (ua: UserAnswers) => List(ua.get(MemberStatus(srn, refineMV(1))).exists(_._new))
          ).withName(s"Set Member status to New when Status is Check and its a new member (no previous member state)")
        )

        act.like(
          saveAndContinue(
            onSubmit(NormalMode),
            userAnswersWithNino.unsafeSet(MemberStatus(srn, refineMV(1)), MemberState.Changed),
            (ua: UserAnswers) => List(ua.get(MemberStatus(srn, refineMV(1))).exists(_.changed))
          ).withName(s"DO NOT Set Member status to New when Status is Check and member already has a state of Changed")
        )
      }

      "on Change" - {

        act.like(
          saveAndContinue(
            call = onSubmit(CheckMode),
            userAnswers = userAnswersWithNino.unsafeSet(MemberStatus(srn, refineMV(1)), MemberState.New),
            pureUserAnswers = userAnswersWithNino.unsafeSet(MemberStatus(srn, refineMV(1)), MemberState.New).some,
            expectations = (ua: UserAnswers) =>
              List(
                ua.get(MemberStatus(srn, refineMV(1))).exists(_._new),
                ua.get(SafeToHardDelete(srn, refineMV(1))).isEmpty
              )
          ).withName(s"DO NOT Set Member status to Changed when member details have not changed")
        )

        act.like(
          saveAndContinue(
            call = onSubmit(CheckMode),
            userAnswers = userAnswersWithNino.unsafeSet(MemberStatus(srn, refineMV(1)), MemberState.New),
            pureUserAnswers = userAnswersWithNino.unsafeSet(MemberStatus(srn, refineMV(1)), MemberState.New).some,
            expectations = (ua: UserAnswers) =>
              List(
                ua.get(MemberStatus(srn, refineMV(1))).exists(_._new),
                ua.get(SafeToHardDelete(srn, refineMV(1))).isEmpty
              )
          ).withName(s"DO NOT Set Member status to Changed when member PSR version is not present for member")
        )

        act.like(
          saveAndContinue(
            call = onSubmit(CheckMode),
            userAnswers = userAnswersWithNino.unsafeSet(MemberStatus(srn, refineMV(1)), MemberState.Changed),
            pureUserAnswers = userAnswersWithNino.unsafeSet(MemberStatus(srn, refineMV(1)), MemberState.New).some,
            expectations = (ua: UserAnswers) =>
              List(
                ua.get(MemberStatus(srn, refineMV(1))).exists(_.changed),
                ua.get(SafeToHardDelete(srn, refineMV(1))).isEmpty
              )
          ).withName(s"Set Member status to Changed when a member detail has changed")
        )

        act.like(
          saveAndContinue(
            onSubmit(CheckMode),
            userAnswersWithNino.unsafeSet(MemberStatus(srn, refineMV(1)), MemberState.New),
            pureUserAnswers = userAnswersWithNino.unsafeSet(MemberStatus(srn, refineMV(1)), MemberState.New).some,
            (ua: UserAnswers) => List(ua.get(MemberStatus(srn, refineMV(1))).exists(_._new))
          ).withName(
            s"Keep Member status as New when member state is New and member details have not changed"
          )
        )
      }
    }

    "viewModel" - {

      def buildViewModel(mode: Mode) =
        viewModel(
          index,
          srn,
          mode,
          memberDetails,
          hasNINO = true,
          Some(nino),
          None,
          viewOnlyUpdated = true
        )

      "contain the correct title" - {
        "NormalMode" in {
          buildViewModel(NormalMode).title mustBe Message("checkYourAnswers.title")
        }
        "CheckMode" in {
          buildViewModel(CheckMode).title mustBe Message("changeMemberDetails.title")
        }
      }

      "contain the correct heading" - {
        "NormalMode" in {
          buildViewModel(NormalMode).heading mustBe Message("checkYourAnswers.heading")
        }
        "CheckMode" in {
          buildViewModel(CheckMode).heading mustBe Message(
            "changeMemberDetails.heading",
            Message(memberDetails.fullName)
          )
        }
      }

      "contain all rows if has nino is true and nino is present" in {
        val vm =
          viewModel(
            index,
            srn,
            NormalMode,
            memberDetails,
            hasNINO = true,
            Some(nino),
            None,
            viewOnlyUpdated = true
          )
        vm.page.sections.map(_.rows.size).sum mustBe 5
      }

      "contain all rows if has nino is false and no nino reason is present" in {
        val vm =
          viewModel(
            index,
            srn,
            NormalMode,
            memberDetails,
            hasNINO = false,
            None,
            Some("test reason"),
            viewOnlyUpdated = true
          )
        vm.page.sections.map(_.rows.size).sum mustBe 5
      }
    }
  }

  "SchemeMemberDetailsAnswersController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
      .unsafeSet(MemberDetailsCompletedPage(srn, index), SectionCompleted)
      .unsafeSet(DoesMemberHaveNinoPage(srn, index), false)
      .unsafeSet(NoNINOPage(srn, index), noNinoReason)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")

    act.like(
      renderView(
        onPageLoadViewOnly,
        userAnswers = currentUserAnswers,
        optPreviousAnswers = Some(previousUserAnswers)
      ) { implicit app => implicit request =>
        injected[CheckYourAnswersView].apply(
          viewModel(
            index,
            srn,
            ViewOnlyMode,
            memberDetails,
            hasNINO = false,
            None,
            Some("test reason"),
            viewOnlyUpdated = false,
            optYear = Some(yearString),
            optCurrentVersion = Some(submissionNumberTwo),
            optPreviousVersion = Some(submissionNumberOne),
            compilationOrSubmissionDate = Some(submissionDateTwo)
          )
        )
      }
    )
    act
      .like(
        redirectToPage(
          onSubmitViewOnly,
          controllers.nonsipp.memberdetails.routes.SchemeMembersListController
            .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne)
        ).after(
            verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
          )
          .withName("Submit redirects to view only tasklist")
      )
  }
}
