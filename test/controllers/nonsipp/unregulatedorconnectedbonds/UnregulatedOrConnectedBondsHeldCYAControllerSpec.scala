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

package controllers.nonsipp.unregulatedorconnectedbonds

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.unregulatedorconnectedbonds.UnregulatedOrConnectedBondsHeldCYAController._
import eu.timepit.refined.refineMV
import models.SchemeHoldBond.Acquisition
import models.{CheckMode, Mode, NormalMode}
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.bonds._
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import views.html.CheckYourAnswersView

class UnregulatedOrConnectedBondsHeldCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeAll(): Unit =
    reset(mockPsrSubmissionService)

  private val index = refineMV[OneTo5000](1)

  private def onPageLoad(mode: Mode) =
    routes.UnregulatedOrConnectedBondsHeldCYAController.onPageLoad(srn, index, mode)

  private def onSubmit(mode: Mode) = routes.UnregulatedOrConnectedBondsHeldCYAController.onSubmit(srn, mode)

  private val filledUserAnswers = defaultUserAnswers
    .unsafeSet(NameOfBondsPage(srn, index), otherName)
    .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), Acquisition)
    .unsafeSet(WhenDidSchemeAcquireBondsPage(srn, index), localDate)
    .unsafeSet(CostOfBondsPage(srn, index), money)
    .unsafeSet(BondsFromConnectedPartyPage(srn, index), true)
    .unsafeSet(AreBondsUnregulatedPage(srn, index), true)
    .unsafeSet(IncomeFromBondsPage(srn, index), money)

  "UnregulatedOrConnectedBondsHeldCYAController" - {
    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), filledUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                index,
                schemeName,
                otherName,
                Acquisition,
                Some(localDate),
                money,
                Some(true),
                areBondsUnregulated = true,
                money,
                mode
              )
            )
          )
        }.withName(s"render correct $mode view")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(MockPSRSubmissionService.submitPsrDetails())
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any())(any(), any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"redirect to next page when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in $mode mode")
      )
    }
  }
}
