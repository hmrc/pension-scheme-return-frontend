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
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CheckYourAnswersView
import cats.implicits.catsSyntaxOptionId
import eu.timepit.refined.refineMV
import models.{CheckMode, Mode, NormalMode}
import models.UserAnswers
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails._
import org.mockito.Mockito.{reset, when}
import viewmodels.DisplayMessage.Message
import viewmodels.models.MemberState

import scala.concurrent.Future

class SchemeMemberDetailsAnswersControllerSpec extends ControllerBaseSpec {

  private def onPageLoad(mode: Mode) =
    routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), mode)

  private def onSubmit(mode: Mode) =
    routes.SchemeMemberDetailsAnswersController.onSubmit(srn, refineMV(1), mode)

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

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
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), true)
    .unsafeSet(MemberDetailsNinoPage(srn, refineMV(1)), nino)

  private val userAnswersWithoutNino = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), false)
    .unsafeSet(NoNINOPage(srn, refineMV(1)), noNinoReason)

  "SchemeMemberDetailsCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), userAnswersWithNino)(
          implicit app =>
            implicit request =>
              injected[CheckYourAnswersView].apply(
                viewModel(refineMV(1), srn, mode, memberDetails, hasNINO = true, Some(nino), None)
              )
        ).withName(s"render correct $mode view when nino provided")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersWithoutNino)(
          implicit app =>
            implicit request =>
              injected[CheckYourAnswersView].apply(
                viewModel(
                  refineMV(1),
                  srn,
                  mode,
                  memberDetails,
                  hasNINO = false,
                  None,
                  Some(noNinoReason)
                )
              )
        ).withName(s"render the correct $mode view when no nino provided")
      )

      act.like(
        redirectToPage(
          onPageLoad(mode),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(MemberDetailsPage(srn, refineMV(1))).success.value
        ).withName(s"when member details are missing in $mode")
      )

      act.like(
        redirectToPage(
          onPageLoad(mode),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(DoesMemberHaveNinoPage(srn, refineMV(1))).success.value
        ).withName(s"when does member have NINO is missing in $mode")
      )

      act.like(
        redirectToPage(
          onPageLoad(mode),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(MemberDetailsNinoPage(srn, refineMV(1))).success.value
        ).withName(s"when NINO is missing in $mode")
      )

      act.like(
        redirectToPage(
          onPageLoad(mode),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithoutNino.remove(NoNINOPage(srn, refineMV(1))).success.value
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
          refineMV(1),
          srn,
          mode,
          memberDetails,
          hasNINO = true,
          Some(nino),
          None
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
          viewModel(refineMV(1), srn, NormalMode, memberDetails, hasNINO = true, Some(nino), None)
        vm.page.sections.map(_.rows.size).sum mustBe 5
      }

      "contain all rows if has nino is false and no nino reason is present" in {
        val vm =
          viewModel(
            refineMV(1),
            srn,
            NormalMode,
            memberDetails,
            hasNINO = false,
            None,
            Some("test reason")
          )
        vm.page.sections.map(_.rows.size).sum mustBe 5
      }
    }
  }
}
