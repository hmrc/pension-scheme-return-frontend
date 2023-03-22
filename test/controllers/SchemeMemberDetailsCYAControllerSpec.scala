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

package controllers

import controllers.SchemeMemberDetailsCYAController._
import eu.timepit.refined.refineMV
import models.NormalMode
import pages.{MemberDetailsNinoPage, MemberDetailsPage, NationalInsuranceNumberPage, NoNINOPage}
import uk.gov.hmrc.domain.Nino
import utils.UserAnswersUtils._
import views.html.CheckYourAnswersView

class SchemeMemberDetailsCYAControllerSpec extends ControllerBaseSpec {

  lazy val onPageLoad = controllers.routes.SchemeMemberDetailsCYAController.onPageLoad(srn, refineMV(1))
  lazy val onSubmit = controllers.routes.SchemeMemberDetailsCYAController.onSubmit(srn, refineMV(1))

  private val nino = Nino("AB123456A")
  private val noNinoReason = "test reason"

  private val userAnswersWithNino = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(NationalInsuranceNumberPage(srn, refineMV(1)), true)
    .unsafeSet(MemberDetailsNinoPage(srn, refineMV(1)), nino)

  private val userAnswersWithoutNino = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(NationalInsuranceNumberPage(srn, refineMV(1)), false)
    .unsafeSet(NoNINOPage(srn, refineMV(1)), noNinoReason)

  "SchemeMemberDetailsCYAController" should {

    act.like(
      renderView(onPageLoad, userAnswersWithNino)(
        implicit app =>
          implicit request =>
            injected[CheckYourAnswersView].apply(
              viewModel(refineMV(1), srn, NormalMode, memberDetails, hasNINO = true, Some(nino), None)
            )
      ).withName("render correct view when nino provided")
    )

    act.like(
      renderView(onPageLoad, userAnswersWithoutNino)(
        implicit app =>
          implicit request =>
            injected[CheckYourAnswersView].apply(
              viewModel(refineMV(1), srn, NormalMode, memberDetails, hasNINO = false, None, Some(noNinoReason))
            )
      ).withName("render the correct view when no nino provided")
    )

    "when member details is missing" should {
      act.like(
        redirectToPage(
          onPageLoad,
          routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(MemberDetailsPage(srn, refineMV(1))).success.value
        )
      )
    }

    "when dose member have NINO is missing" should {
      act.like(
        redirectToPage(
          onPageLoad,
          routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(NationalInsuranceNumberPage(srn, refineMV(1))).success.value
        )
      )
    }

    "when NINO is missing" should {
      act.like(
        redirectToPage(
          onPageLoad,
          routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithNino.remove(MemberDetailsNinoPage(srn, refineMV(1))).success.value
        )
      )
    }

    "when no NINO reason is missing" should {
      act.like(
        redirectToPage(
          onPageLoad,
          routes.JourneyRecoveryController.onPageLoad(),
          userAnswersWithoutNino.remove(NoNINOPage(srn, refineMV(1))).success.value
        )
      )
    }

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    "viewModel" should {
      "contain all rows if has nino is true and nino is present" in {
        val vm = viewModel(refineMV(1), srn, NormalMode, memberDetails, hasNINO = true, Some(nino), None)
        vm.rows.size mustBe 5
      }

      "contain all rows if has nino is false and no nino reason is present" in {
        val vm = viewModel(refineMV(1), srn, NormalMode, memberDetails, hasNINO = false, None, Some("test reason"))
        vm.rows.size mustBe 5
      }
    }
  }
}
