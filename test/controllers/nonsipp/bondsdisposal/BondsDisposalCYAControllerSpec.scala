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

package controllers.nonsipp.bondsdisposal

import services.PsrSubmissionService
import controllers.nonsipp.bondsdisposal.BondsDisposalCYAController._
import config.Refined.{OneTo50, OneTo5000}
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CheckYourAnswersView
import eu.timepit.refined.refineMV
import pages.nonsipp.FbVersionPage
import models._
import pages.nonsipp.bondsdisposal._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds.{CostOfBondsPage, NameOfBondsPage, WhyDoesSchemeHoldBondsPage}

import scala.concurrent.Future

class BondsDisposalCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] =
    List(bind[PsrSubmissionService].toInstance(mockPsrSubmissionService))

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private def onPageLoad(mode: Mode) =
    routes.BondsDisposalCYAController.onPageLoad(srn, bondIndex, disposalIndex, mode)
  private def onSubmit(mode: Mode) =
    routes.BondsDisposalCYAController.onSubmit(srn, bondIndex, disposalIndex, mode)

  private lazy val onSubmitViewOnly = routes.BondsDisposalCYAController.onSubmitViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onPageLoadViewOnly = routes.BondsDisposalCYAController.onPageLoadViewOnly(
    srn,
    bondIndex,
    disposalIndex,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val bondIndex = refineMV[OneTo5000](1)
  private val disposalIndex = refineMV[OneTo50](1)

  private val dateBondsSold = Some(localDate)
  private val considerationBondsSold = Some(money)
  private val nameOfBuyer = Some(buyerName)
  private val isBuyerConnectedParty = Some(true)

  private val soldUserAnswers = defaultUserAnswers
    .unsafeSet(NameOfBondsPage(srn, bondIndex), "name")
    .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, bondIndex), SchemeHoldBond.Acquisition)
    .unsafeSet(CostOfBondsPage(srn, bondIndex), money)
    .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex), HowDisposed.Sold)
    .unsafeSet(WhenWereBondsSoldPage(srn, bondIndex, disposalIndex), dateBondsSold.get)
    .unsafeSet(TotalConsiderationSaleBondsPage(srn, bondIndex, disposalIndex), considerationBondsSold.get)
    .unsafeSet(BuyerNamePage(srn, bondIndex, disposalIndex), nameOfBuyer.get)
    .unsafeSet(IsBuyerConnectedPartyPage(srn, bondIndex, disposalIndex), isBuyerConnectedParty.get)
    .unsafeSet(BondsStillHeldPage(srn, bondIndex, disposalIndex), bondsStillHeld)

  "BondsDisposalCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), soldUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                bondIndex,
                disposalIndex,
                "name",
                acquisitionType = SchemeHoldBond.Acquisition,
                costOfBonds = money,
                howBondsDisposed = HowDisposed.Sold,
                dateBondsSold,
                considerationBondsSold,
                Some(buyerName),
                isBuyerConnectedParty,
                bondsStillHeld,
                schemeName,
                mode
              ),
              viewOnlyUpdated = true
            )
          )
        }.withName(s"render correct $mode view for Sold journey")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any())
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

  "BondsDisposalCYAController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(NameOfBondsPage(srn, bondIndex), "name")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, bondIndex), SchemeHoldBond.Acquisition)
      .unsafeSet(CostOfBondsPage(srn, bondIndex), money)
      .unsafeSet(HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex), HowDisposed.Sold)
      .unsafeSet(WhenWereBondsSoldPage(srn, bondIndex, disposalIndex), dateBondsSold.get)
      .unsafeSet(TotalConsiderationSaleBondsPage(srn, bondIndex, disposalIndex), considerationBondsSold.get)
      .unsafeSet(BuyerNamePage(srn, bondIndex, disposalIndex), nameOfBuyer.get)
      .unsafeSet(IsBuyerConnectedPartyPage(srn, bondIndex, disposalIndex), isBuyerConnectedParty.get)
      .unsafeSet(BondsStillHeldPage(srn, bondIndex, disposalIndex), bondsStillHeld)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(NameOfBondsPage(srn, bondIndex), "name")

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                bondIndex,
                disposalIndex,
                "name",
                acquisitionType = SchemeHoldBond.Acquisition,
                costOfBonds = money,
                howBondsDisposed = HowDisposed.Sold,
                dateBondsSold,
                considerationBondsSold,
                Some(buyerName),
                isBuyerConnectedParty,
                bondsStillHeld,
                schemeName,
                ViewOnlyMode
              ),
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
          .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only RemoveBondsDisposalController page")
    )
  }

}
