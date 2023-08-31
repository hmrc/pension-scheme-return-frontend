/*
 * Copyright 2023 HM Revenue & Customs
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

import controllers.ControllerBaseSpec
import controllers.nonsipp.memberdetails.SchemeMemberDetailsAnswersController._
import eu.timepit.refined.refineMV
import models.CheckOrChange
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsNinoPage, MemberDetailsPage, NoNINOPage}
import viewmodels.DisplayMessage.Message
import views.html.CheckYourAnswersView

class SchemeMemberDetailsAnswersControllerSpec extends ControllerBaseSpec {

  private def onPageLoad(checkOrChange: CheckOrChange) =
    routes.SchemeMemberDetailsAnswersController.onPageLoad(srn, refineMV(1), checkOrChange)

  private def onSubmit(checkOrChange: CheckOrChange) =
    routes.SchemeMemberDetailsAnswersController.onSubmit(srn, refineMV(1), checkOrChange)

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

    List(CheckOrChange.Check, CheckOrChange.Change).foreach { checkOrChange =>
      act.like(
        renderView(onPageLoad(checkOrChange), userAnswersWithNino)(
          implicit app =>
            implicit request =>
              injected[CheckYourAnswersView].apply(
                viewModel(refineMV(1), srn, checkOrChange, memberDetails, hasNINO = true, Some(nino), None)
              )
        ).withName(s"render correct ${checkOrChange.name} view when nino provided")
      )

      act.like(
        renderView(onPageLoad(checkOrChange), userAnswersWithoutNino)(
          implicit app =>
            implicit request =>
              injected[CheckYourAnswersView].apply(
                viewModel(
                  refineMV(1),
                  srn,
                  checkOrChange,
                  memberDetails,
                  hasNINO = false,
                  None,
                  Some(noNinoReason)
                )
              )
        ).withName(s"render the correct ${checkOrChange.name} view when no nino provided")
      )

      act.like(
        redirectToPage(
          onPageLoad(checkOrChange),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(MemberDetailsPage(srn, refineMV(1))).success.value
        ).withName(s"when member details are missing on ${checkOrChange.name}")
      )

      act.like(
        redirectToPage(
          onPageLoad(checkOrChange),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(DoesMemberHaveNinoPage(srn, refineMV(1))).success.value
        ).withName(s"when does member have NINO is missing on ${checkOrChange.name}")
      )

      act.like(
        redirectToPage(
          onPageLoad(checkOrChange),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(MemberDetailsNinoPage(srn, refineMV(1))).success.value
        ).withName(s"when NINO is missing on ${checkOrChange.name}")
      )

      act.like(
        redirectToPage(
          onPageLoad(checkOrChange),
          controllers.routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithoutNino.remove(NoNINOPage(srn, refineMV(1))).success.value
        ).withName(s"when no NINO reason is missing on ${checkOrChange.name}")
      )

      act.like(journeyRecoveryPage(onPageLoad(checkOrChange)).updateName(s"onPageLoad on ${checkOrChange.name}" + _))
      act.like(journeyRecoveryPage(onSubmit(checkOrChange)).updateName(s"onSubmit on ${checkOrChange.name}" + _))
    }

    "viewModel" - {

      def buildViewModel(checkOrChange: CheckOrChange) =
        viewModel(
          refineMV(1),
          srn,
          checkOrChange,
          memberDetails,
          hasNINO = true,
          Some(nino),
          None
        )

      "contain the correct title" - {
        "CheckOrChange is Check" in {
          buildViewModel(CheckOrChange.Check).title mustBe Message("checkYourAnswers.title")
        }
        "CheckOrChange is Change" in {
          buildViewModel(CheckOrChange.Change).title mustBe Message("changeMemberDetails.title")
        }
      }

      "contain the correct heading" - {
        "CheckOrChange is Check" in {
          buildViewModel(CheckOrChange.Check).heading mustBe Message("checkYourAnswers.heading")
        }
        "CheckOrChange is Change" in {
          buildViewModel(CheckOrChange.Change).heading mustBe Message(
            "changeMemberDetails.heading",
            Message(memberDetails.fullName)
          )
        }
      }

      "contain all rows if has nino is true and nino is present" in {
        val vm =
          viewModel(refineMV(1), srn, CheckOrChange.Check, memberDetails, hasNINO = true, Some(nino), None)
        vm.page.sections.map(_.rows.size).sum mustBe 5
      }

      "contain all rows if has nino is false and no nino reason is present" in {
        val vm =
          viewModel(
            refineMV(1),
            srn,
            CheckOrChange.Check,
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
