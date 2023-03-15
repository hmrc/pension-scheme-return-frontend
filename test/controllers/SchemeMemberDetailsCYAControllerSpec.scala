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

import config.Refined.OneToThree
import controllers.SchemeMemberDetailsCYAController._
import eu.timepit.refined.refineMV
import models.{NameDOB, NormalMode}
import pages.{MemberDetailsNinoPage, MemberDetailsPage, NationalInsuranceNumberPage}
import uk.gov.hmrc.domain.Nino
import views.html.CheckYourAnswersView

import java.time.LocalDate

class SchemeMemberDetailsCYAControllerSpec extends ControllerBaseSpec {

  lazy val onPageLoad = controllers.routes.SchemeMemberDetailsCYAController.onPageLoad(srn, refineMV(1))
  lazy val onSubmit = controllers.routes.SchemeMemberDetailsCYAController.onSubmit(srn, refineMV(1))

  private val memberDetails = NameDOB("testFirstName", "testLastName", LocalDate.of(2020, 12, 12))
  private val nino = Nino("AB123456A")

  private val fullUserAnswers = defaultUserAnswers
    .set(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .success
    .value
    .set(NationalInsuranceNumberPage(srn, refineMV(1)), true)
    .success
    .value
    .set(MemberDetailsNinoPage(srn, refineMV(1)), nino)
    .success
    .value

  "SchemeMemberDetailsCYAController" should {
    behave.like(
      renderView(onPageLoad, fullUserAnswers)(
        implicit app =>
          implicit request =>
            injected[CheckYourAnswersView]
              .apply(viewModel(refineMV(1), srn, NormalMode, memberDetails, hasNINO = true, Some(nino)))
      )
    )

    "when member details is missing" should {
      behave.like(
        redirectToPage(
          onPageLoad,
          routes.JourneyRecoveryController.onPageLoad(),
          fullUserAnswers.remove(MemberDetailsPage(srn, refineMV(1))).success.value
        )
      )
    }

    "when dose member have NINO is missing" should {
      behave.like(
        redirectToPage(
          onPageLoad,
          routes.JourneyRecoveryController.onPageLoad(),
          fullUserAnswers.remove(NationalInsuranceNumberPage(srn, refineMV(1))).success.value
        )
      )
    }

    "when NINO is missing" should {
      behave.like(
        redirectToPage(
          onPageLoad,
          routes.JourneyRecoveryController.onPageLoad(),
          fullUserAnswers.remove(MemberDetailsNinoPage(srn, refineMV(1))).success.value
        )
      )
    }

    behave.like(journeyRecoveryPage("onPageLoad", onPageLoad))
    behave.like(journeyRecoveryPage("onSubmit", onSubmit))

    "viewModel" should {
      "contain all rows if nino is present" in {
        val vm = viewModel(refineMV(1), srn, NormalMode, memberDetails, hasNINO = true, Some(nino))
        vm.rows.size mustBe 5
      }

      "contain 4 rows if nino is not present" in {
        val vm = viewModel(refineMV(1), srn, NormalMode, memberDetails, hasNINO = true, None)
        vm.rows.size mustBe 4
      }
    }
  }
}
