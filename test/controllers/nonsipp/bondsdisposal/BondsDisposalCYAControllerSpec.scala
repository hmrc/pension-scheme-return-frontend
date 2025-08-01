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

import services.{PsrSubmissionService, SaveService}
import models.PointOfEntry.{HowWereBondsDisposedPointOfEntry, NoPointOfEntry}
import utils.nonsipp.summary.{BondsDisposalCheckAnswersUtils, BondsDisposalViewModelParameters}
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.CheckYourAnswersView
import utils.IntUtils.given
import pages.nonsipp.FbVersionPage
import models._
import pages.nonsipp.bondsdisposal._
import viewmodels.models.SectionJourneyStatus
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.bonds.{CostOfBondsPage, NameOfBondsPage, WhyDoesSchemeHoldBondsPage}

import scala.concurrent.Future

class BondsDisposalCYAControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]
  private implicit val mockSaveService: SaveService = mock[SaveService]

  override protected val additionalBindings: List[GuiceableModule] =
    List(bind[PsrSubmissionService].toInstance(mockPsrSubmissionService), bind[SaveService].toInstance(mockSaveService))

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    reset(mockSaveService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
    when(mockSaveService.save(any())(using any(), any())).thenReturn(Future.successful(()))
  }

  private def onPageLoad(mode: Mode) =
    routes.BondsDisposalCYAController.onPageLoad(srn, bondIndex, disposalIndex, mode)
  private def onSubmit(mode: Mode) =
    routes.BondsDisposalCYAController.onSubmit(srn, bondIndex, disposalIndex, mode)

  private lazy val onSubmitViewOnly = routes.BondsDisposalCYAController.onSubmitViewOnly(
    srn,
    page,
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

  private val bondIndex = 1
  private val disposalIndex = 1
  private val page = 1

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
    .unsafeSet(BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex), HowWereBondsDisposedPointOfEntry)
    .unsafeSet(BondsDisposalProgress(srn, bondIndex, disposalIndex), SectionJourneyStatus.Completed)

  "BondsDisposalCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), soldUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            BondsDisposalCheckAnswersUtils.viewModel(
              BondsDisposalViewModelParameters(
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
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.after {
          verify(mockSaveService, times(1)).save(any())(using any(), any())
          reset(mockPsrSubmissionService)
        }.withName(s"render correct $mode view for Sold journey")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .after {
            verify(mockPsrSubmissionService, times(1))
              .submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any())
            reset(mockPsrSubmissionService)
          }
          .withName(s"redirect to next page when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onPageLoad(mode))
          .updateName("onPageLoad" + _)
          .withName(s"redirect to journey recovery page on page load when in $mode mode")
      )

      act.like(
        redirectToPage(
          call = onPageLoad(mode),
          page = routes.ReportBondsDisposalListController.onPageLoad(srn, 1),
          userAnswers = soldUserAnswers
            .unsafeSet(BondsDisposalProgress(srn, bondIndex, disposalIndex), SectionJourneyStatus.InProgress(anyUrl)),
          previousUserAnswers = emptyUserAnswers
        ).withName(s"Redirect to bonds list when incomplete when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in $mode mode")
      )
    }

  }

  "BondsDisposalCYAController PointOfEntry" - {
    val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

    act.like(
      renderView(
        onPageLoad(CheckMode),
        soldUserAnswers
      ) { implicit app => implicit request =>
        injected[CheckYourAnswersView].apply(
          BondsDisposalCheckAnswersUtils.viewModel(
            BondsDisposalViewModelParameters(
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
              CheckMode
            ),
            viewOnlyUpdated = true,
            isMaximumReached = false
          )
        )
      }.after {
        verify(mockSaveService).save(captor.capture())(using any(), any())
        val userAnswers = captor.getValue
        userAnswers.get(BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex)) mustBe Some(NoPointOfEntry)
        reset(mockPsrSubmissionService)
      }.withName(s"onPageLoad should clear out point of entry when completed")
    )

    act.like(
      redirectToPage(
        call = onPageLoad(CheckMode),
        page = routes.ReportBondsDisposalListController.onPageLoad(srn, 1),
        userAnswers = soldUserAnswers
          .unsafeSet(BondsDisposalProgress(srn, bondIndex, disposalIndex), SectionJourneyStatus.InProgress(anyUrl))
          .unsafeSet(BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex), HowWereBondsDisposedPointOfEntry),
        previousUserAnswers = emptyUserAnswers
      ).after {
        verify(mockSaveService, never).save(any())(using any(), any())
        reset(mockPsrSubmissionService)
      }.withName(s"onPageLoad should not clear out point of entry when in progress")
    )
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
      .unsafeSet(BondsDisposalProgress(srn, bondIndex, disposalIndex), SectionJourneyStatus.Completed)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(NameOfBondsPage(srn, bondIndex), "name")

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            BondsDisposalCheckAnswersUtils.viewModel(
              BondsDisposalViewModelParameters(
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
              isMaximumReached = false
            )
          )
      }.after {
        verify(mockSaveService, never()).save(any())(using any(), any())
        reset(mockPsrSubmissionService)
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.bondsdisposal.routes.ReportBondsDisposalListController
          .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
        verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(using any(), any(), any())
      ).withName("Submit redirects to view only RemoveBondsDisposalController page")
    )
  }

}
