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

package controllers.nonsipp.sharesdisposal

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.shares._
import controllers.nonsipp.sharesdisposal.SharesDisposalCYAController._
import play.api.inject.bind
import views.html.CheckYourAnswersView
import eu.timepit.refined.refineMV
import pages.nonsipp.sharesdisposal._
import models._
import viewmodels.models.SectionJourneyStatus
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import config.RefinedTypes.{OneTo50, OneTo5000}
import controllers.ControllerBaseSpec
import models.PointOfEntry.{HowWereSharesDisposedPointOfEntry, NoPointOfEntry}
import org.mockito.ArgumentCaptor
import pages.nonsipp.FbVersionPage
import uk.gov.hmrc.domain.Nino

import scala.concurrent.Future

class SharesDisposalCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]
  private implicit val mockSaveService: SaveService = mock[SaveService]

  override protected val additionalBindings: List[GuiceableModule] =
    List(bind[PsrSubmissionService].toInstance(mockPsrSubmissionService), bind[SaveService].toInstance(mockSaveService))

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    reset(mockSaveService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
    when(mockSaveService.save(any())(any(), any())).thenReturn(Future.successful(()))
  }

  private def onPageLoad(mode: Mode) =
    routes.SharesDisposalCYAController.onPageLoad(srn, shareIndex, disposalIndex, mode)
  private def onSubmit(mode: Mode) =
    routes.SharesDisposalCYAController.onSubmit(srn, shareIndex, disposalIndex, mode)

  private lazy val onSubmitViewOnly = routes.SharesDisposalCYAController.onSubmitViewOnly(
    srn,
    1,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onPageLoadViewOnly = routes.SharesDisposalCYAController.onPageLoadViewOnly(
    srn,
    shareIndex,
    disposalIndex,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val shareIndex = refineMV[OneTo5000](1)
  private val disposalIndex = refineMV[OneTo50](1)

  // Shares parameters
  private val nameOfCompany = Some(companyName)
  private val acquisitionDate = Some(localDate)
  // Sold-specific parameters
  private val dateSharesSold = Some(localDate)
  private val numberSharesSold = Some(totalShares)
  private val considerationSharesSold = Some(money)
  private val buyerIdentity = Some(IdentityType.Individual)
  private val nameOfBuyer = Some(buyerName)
  private val buyerDetails = nino
  private val buyerReasonNoDetails = None
  private val isBuyerConnectedParty = Some(true)
  private val isIndependentValuation = Some(true)
  // Redeemed-specific parameters
  private val dateSharesRedeemed = Some(localDate)
  private val numberSharesRedeemed = Some(totalShares)
  private val considerationSharesRedeemed = Some(money)
  // Final parameter
  private val sharesStillHeld = totalShares - 1

  private val soldUserAnswers = defaultUserAnswers
  // Shares pages
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndex), TypeOfShares.SponsoringEmployer)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), nameOfCompany.get)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, shareIndex), SchemeHoldShare.Acquisition)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, shareIndex), acquisitionDate.get)
    // Shares Disposal pages
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex), HowSharesDisposed.Sold)
    .unsafeSet(WhenWereSharesSoldPage(srn, shareIndex, disposalIndex), dateSharesSold.get)
    .unsafeSet(HowManySharesSoldPage(srn, shareIndex, disposalIndex), numberSharesSold.get)
    .unsafeSet(TotalConsiderationSharesSoldPage(srn, shareIndex, disposalIndex), considerationSharesSold.get)
    .unsafeSet(WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex), buyerIdentity.get)
    .unsafeSet(SharesIndividualBuyerNamePage(srn, shareIndex, disposalIndex), nameOfBuyer.get)
    .unsafeSet(
      IndividualBuyerNinoNumberPage(srn, shareIndex, disposalIndex),
      ConditionalYesNo.yes[String, Nino](buyerDetails)
    )
    .unsafeSet(IsBuyerConnectedPartyPage(srn, shareIndex, disposalIndex), isBuyerConnectedParty.get)
    .unsafeSet(IndependentValuationPage(srn, shareIndex, disposalIndex), isIndependentValuation.get)
    .unsafeSet(HowManyDisposalSharesPage(srn, shareIndex, disposalIndex), sharesStillHeld)
    .unsafeSet(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex), HowWereSharesDisposedPointOfEntry)
    .unsafeSet(SharesDisposalProgress(srn, shareIndex, disposalIndex), SectionJourneyStatus.Completed)

  private val redeemedUserAnswers = defaultUserAnswers
  // Shares pages
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndex), TypeOfShares.Unquoted)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), nameOfCompany.get)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, shareIndex), SchemeHoldShare.Contribution)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, shareIndex), acquisitionDate.get)
    // Shares Disposal pages
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex), HowSharesDisposed.Redeemed)
    .unsafeSet(WhenWereSharesRedeemedPage(srn, shareIndex, disposalIndex), dateSharesRedeemed.get)
    .unsafeSet(HowManySharesRedeemedPage(srn, shareIndex, disposalIndex), numberSharesRedeemed.get)
    .unsafeSet(TotalConsiderationSharesRedeemedPage(srn, shareIndex, disposalIndex), considerationSharesRedeemed.get)
    .unsafeSet(HowManyDisposalSharesPage(srn, shareIndex, disposalIndex), sharesStillHeld)
    .unsafeSet(SharesDisposalProgress(srn, shareIndex, disposalIndex), SectionJourneyStatus.Completed)

  private val transferredUserAnswers = defaultUserAnswers
  // Shares pages
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndex), TypeOfShares.ConnectedParty)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), nameOfCompany.get)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, shareIndex), SchemeHoldShare.Transfer)
    // Shares Disposal pages
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex), HowSharesDisposed.Transferred)
    .unsafeSet(HowManyDisposalSharesPage(srn, shareIndex, disposalIndex), sharesStillHeld)
    .unsafeSet(SharesDisposalProgress(srn, shareIndex, disposalIndex), SectionJourneyStatus.Completed)

  private val otherUserAnswers = defaultUserAnswers
  // Shares pages
    .unsafeSet(TypeOfSharesHeldPage(srn, shareIndex), TypeOfShares.SponsoringEmployer)
    .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), nameOfCompany.get)
    .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, shareIndex), SchemeHoldShare.Acquisition)
    .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, shareIndex), acquisitionDate.get)
    // Shares Disposal pages
    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex), HowSharesDisposed.Other(otherDetails))
    .unsafeSet(HowManyDisposalSharesPage(srn, shareIndex, disposalIndex), sharesStillHeld)
    .unsafeSet(SharesDisposalProgress(srn, shareIndex, disposalIndex), SectionJourneyStatus.Completed)

  "SharesDisposalCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      // Sold
      act.like(
        renderView(onPageLoad(mode), soldUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                shareIndex,
                disposalIndex,
                TypeOfShares.SponsoringEmployer,
                companyName,
                SchemeHoldShare.Acquisition,
                acquisitionDate,
                HowSharesDisposed.Sold,
                dateSharesSold,
                numberSharesSold,
                considerationSharesSold,
                buyerIdentity,
                Some(buyerName),
                Some(buyerDetails.toString),
                buyerReasonNoDetails,
                isBuyerConnectedParty,
                isIndependentValuation,
                None,
                None,
                None,
                sharesStillHeld,
                schemeName,
                mode
              ),
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.after({
            verify(mockSaveService, times(1)).save(any())(any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"render correct $mode view for Sold journey")
      )

      // Redeemed
      act.like(
        renderView(onPageLoad(mode), redeemedUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                shareIndex,
                disposalIndex,
                TypeOfShares.Unquoted,
                companyName,
                SchemeHoldShare.Contribution,
                acquisitionDate,
                HowSharesDisposed.Redeemed,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                dateSharesRedeemed,
                numberSharesRedeemed,
                considerationSharesRedeemed,
                sharesStillHeld,
                schemeName,
                mode
              ),
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.after({
            verify(mockSaveService, times(1)).save(any())(any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"render correct $mode view for Redeemed journey")
      )

      // Transferred
      act.like(
        renderView(onPageLoad(mode), transferredUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                shareIndex,
                disposalIndex,
                TypeOfShares.ConnectedParty,
                companyName,
                SchemeHoldShare.Transfer,
                None,
                HowSharesDisposed.Transferred,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                sharesStillHeld,
                schemeName,
                mode
              ),
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.after({
            verify(mockSaveService, times(1)).save(any())(any(), any())
            reset(mockPsrSubmissionService)
          })
          .withName(s"render correct $mode view for Transferred journey")
      )

      // Other
      act.like(
        renderView(onPageLoad(mode), otherUserAnswers) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                shareIndex,
                disposalIndex,
                TypeOfShares.SponsoringEmployer,
                companyName,
                SchemeHoldShare.Acquisition,
                acquisitionDate,
                HowSharesDisposed.Other(otherDetails),
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                None,
                sharesStillHeld,
                schemeName,
                mode
              ),
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Other journey")
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
        redirectToPage(
          call = onPageLoad(mode),
          page = routes.SharesDisposalListController.onPageLoad(srn, 1),
          userAnswers = soldUserAnswers
            .unsafeSet(SharesDisposalProgress(srn, shareIndex, disposalIndex), SectionJourneyStatus.InProgress("any")),
          previousUserAnswers = emptyUserAnswers
        ).withName(s"Redirect to shares list when incomplete when in $mode mode")
      )

      act.like(
        journeyRecoveryPage(onSubmit(mode))
          .updateName("onSubmit" + _)
          .withName(s"redirect to journey recovery page on submit when in $mode mode")
      )
    }
  }

  "SharesDisposalCYAController PointOfEntry" - {
    val captor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

    act.like(
      renderView(
        onPageLoad(CheckMode),
        soldUserAnswers
      ) { implicit app => implicit request =>
        injected[CheckYourAnswersView].apply(
          viewModel(
            ViewModelParameters(
              srn,
              shareIndex,
              disposalIndex,
              TypeOfShares.SponsoringEmployer,
              companyName,
              SchemeHoldShare.Acquisition,
              acquisitionDate,
              HowSharesDisposed.Sold,
              dateSharesSold,
              numberSharesSold,
              considerationSharesSold,
              buyerIdentity,
              Some(buyerName),
              Some(buyerDetails.toString),
              buyerReasonNoDetails,
              isBuyerConnectedParty,
              isIndependentValuation,
              None,
              None,
              None,
              sharesStillHeld,
              schemeName,
              CheckMode
            ),
            viewOnlyUpdated = true,
            isMaximumReached = false
          )
        )
      }.after({
          verify(mockSaveService).save(captor.capture())(any(), any())
          val userAnswers = captor.getValue
          userAnswers.get(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex)) mustBe Some(NoPointOfEntry)
          reset(mockPsrSubmissionService)
        })
        .withName(s"onPageLoad should clear out point of entry when completed")
    )

    act.like(
      redirectToPage(
        call = onPageLoad(CheckMode),
        page = routes.SharesDisposalListController.onPageLoad(srn, 1),
        userAnswers = soldUserAnswers
          .unsafeSet(SharesDisposalProgress(srn, shareIndex, disposalIndex), SectionJourneyStatus.InProgress(anyUrl))
          .unsafeSet(SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex), HowWereSharesDisposedPointOfEntry),
        previousUserAnswers = emptyUserAnswers
      ).after {
          verify(mockSaveService, never).save(any())(any(), any())
          reset(mockPsrSubmissionService)
        }
        .withName(s"onPageLoad should not clear out point of entry when in progress")
    )
  }

  "SharesDisposalCYAController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      // Shares pages
      .unsafeSet(TypeOfSharesHeldPage(srn, shareIndex), TypeOfShares.SponsoringEmployer)
      .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), nameOfCompany.get)
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, shareIndex), SchemeHoldShare.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, shareIndex), acquisitionDate.get)
      // Shares Disposal pages
      .unsafeSet(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex), HowSharesDisposed.Sold)
      .unsafeSet(WhenWereSharesSoldPage(srn, shareIndex, disposalIndex), dateSharesSold.get)
      .unsafeSet(HowManySharesSoldPage(srn, shareIndex, disposalIndex), numberSharesSold.get)
      .unsafeSet(TotalConsiderationSharesSoldPage(srn, shareIndex, disposalIndex), considerationSharesSold.get)
      .unsafeSet(WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex), buyerIdentity.get)
      .unsafeSet(SharesIndividualBuyerNamePage(srn, shareIndex, disposalIndex), nameOfBuyer.get)
      .unsafeSet(
        IndividualBuyerNinoNumberPage(srn, shareIndex, disposalIndex),
        ConditionalYesNo.yes[String, Nino](buyerDetails)
      )
      .unsafeSet(IsBuyerConnectedPartyPage(srn, shareIndex, disposalIndex), isBuyerConnectedParty.get)
      .unsafeSet(IndependentValuationPage(srn, shareIndex, disposalIndex), isIndependentValuation.get)
      .unsafeSet(HowManyDisposalSharesPage(srn, shareIndex, disposalIndex), sharesStillHeld)
      .unsafeSet(SharesDisposalProgress(srn, shareIndex, disposalIndex), SectionJourneyStatus.Completed)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(CompanyNameRelatedSharesPage(srn, shareIndex), nameOfCompany.get)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                shareIndex,
                disposalIndex,
                TypeOfShares.SponsoringEmployer,
                companyName,
                SchemeHoldShare.Acquisition,
                acquisitionDate,
                HowSharesDisposed.Sold,
                dateSharesSold,
                numberSharesSold,
                considerationSharesSold,
                buyerIdentity,
                Some(buyerName),
                Some(buyerDetails.toString),
                buyerReasonNoDetails,
                isBuyerConnectedParty,
                isIndependentValuation,
                None,
                None,
                None,
                sharesStillHeld,
                schemeName,
                ViewOnlyMode
              ),
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              compilationOrSubmissionDate = Some(submissionDateTwo),
              isMaximumReached = false
            )
          )
      }.after({
          verify(mockSaveService, never()).save(any())(any(), any())
          reset(mockPsrSubmissionService)
        })
        .withName("OnPageLoadViewOnly renders ok with no changed flag")
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
          .onPageLoadViewOnly(srn, 1, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only ReportedSharesDisposalListController page")
    )
  }

}
