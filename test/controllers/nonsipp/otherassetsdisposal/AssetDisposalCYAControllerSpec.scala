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

package controllers.nonsipp.otherassetsdisposal

import services.PsrSubmissionService
import pages.nonsipp.otherassetsdisposal._
import pages.nonsipp.otherassetsheld.WhatIsOtherAssetPage
import utils.nonsipp.summary.{OtherAssetsDisposalCheckAnswersUtils, OtherAssetsDisposalViewModelParameters}
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.CheckYourAnswersView
import utils.IntUtils.given
import pages.nonsipp.FbVersionPage
import models._
import viewmodels.models.SectionJourneyStatus
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

import scala.concurrent.Future

class AssetDisposalCYAControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] =
    List(bind[PsrSubmissionService].toInstance(mockPsrSubmissionService))

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private def onPageLoad(mode: Mode) =
    routes.AssetDisposalCYAController.onPageLoad(srn, assetIndex, disposalIndex, mode)
  private def onSubmit(mode: Mode) =
    routes.AssetDisposalCYAController.onSubmit(srn, assetIndex, disposalIndex, mode)

  private lazy val onSubmitViewOnly = routes.AssetDisposalCYAController.onSubmitViewOnly(
    srn,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onPageLoadViewOnly = routes.AssetDisposalCYAController.onPageLoadViewOnly(
    srn,
    assetIndex,
    disposalIndex,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private val assetIndex = 1
  private val disposalIndex = 1
  private val page = 1

  private val dateAssetSold = Some(localDate)
  private val considerationAssetSold = Some(money)
  private val isBuyerConnectedParty = Some(true)
  private val otherAsset = "name"

  private val userAnswersSoldPartnership = defaultUserAnswers
    .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex), HowDisposed.Sold)
    .unsafeSet(WhatIsOtherAssetPage(srn, assetIndex), otherAsset)
    .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex), considerationAssetSold.get)
    .unsafeSet(AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex), IdentityType.UKPartnership)
    .unsafeSet(PartnershipBuyerNamePage(srn, assetIndex, disposalIndex), recipientName)
    .unsafeSet(WhenWasAssetSoldPage(srn, assetIndex, disposalIndex), dateAssetSold.get)
    .unsafeSet(IsBuyerConnectedPartyPage(srn, assetIndex, disposalIndex), isBuyerConnectedParty.get)
    .unsafeSet(OtherAssetsDisposalProgress(srn, assetIndex, disposalIndex), SectionJourneyStatus.Completed)

  private val userAnswersSoldIndividual = userAnswersSoldPartnership
    .unsafeSet(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex), IdentityType.Individual)
    .unsafeSet(IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex), recipientName)
    .unsafeSet(AssetIndividualBuyerNiNumberPage(srn, assetIndex, disposalIndex), conditionalYesNoNino)
    .unsafeRemove(PartnershipBuyerNamePage(srn, assetIndex, disposalIndex))
    .unsafeSet(IsBuyerConnectedPartyPage(srn, assetIndex, disposalIndex), false)
    .unsafeSet(AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex), false)

  private val userAnswersSoldCompany = userAnswersSoldPartnership
    .unsafeSet(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex), IdentityType.UKCompany)
    .unsafeSet(CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex), recipientName)
    .unsafeSet(AssetCompanyBuyerCrnPage(srn, assetIndex, disposalIndex), ConditionalYesNo.no(noCrnReason))
    .unsafeRemove(PartnershipBuyerNamePage(srn, assetIndex, disposalIndex))
    .unsafeSet(IsBuyerConnectedPartyPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex), true)

  private val userAnswersSoldOther = userAnswersSoldPartnership
    .unsafeSet(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex), IdentityType.Other)
    .unsafeSet(OtherBuyerDetailsPage(srn, assetIndex, disposalIndex), otherRecipientDetails)
    .unsafeRemove(PartnershipBuyerNamePage(srn, assetIndex, disposalIndex))
    .unsafeSet(IsBuyerConnectedPartyPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex), true)

  private val userAnswersTransferred = defaultUserAnswers
    .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex), HowDisposed.Transferred)
    .unsafeSet(WhatIsOtherAssetPage(srn, assetIndex), otherAsset)
    .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(OtherAssetsDisposalProgress(srn, assetIndex, disposalIndex), SectionJourneyStatus.Completed)

  private val userAnswersOther = defaultUserAnswers
    .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex), HowDisposed.Other(otherDetails))
    .unsafeSet(WhatIsOtherAssetPage(srn, assetIndex), otherAsset)
    .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(OtherAssetsDisposalProgress(srn, assetIndex, disposalIndex), SectionJourneyStatus.Completed)

  "AssetDisposalCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), userAnswersSoldPartnership) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            OtherAssetsDisposalCheckAnswersUtils.viewModel(
              OtherAssetsDisposalViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasAssetDisposed = HowDisposed.Sold,
                dateAssetSold,
                otherAsset,
                assetDisposedType = Some(IdentityType.UKPartnership),
                isBuyerConnectedParty,
                considerationAssetSold,
                independentValuation = Some(true),
                anyPartAssetStillHeld = true,
                Some(recipientName),
                None,
                None,
                mode
              ),
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Sold to UKPartnership journey")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersSoldIndividual) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            OtherAssetsDisposalCheckAnswersUtils.viewModel(
              OtherAssetsDisposalViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasAssetDisposed = HowDisposed.Sold,
                dateAssetSold,
                otherAsset,
                assetDisposedType = Some(IdentityType.Individual),
                Some(false),
                considerationAssetSold,
                independentValuation = Some(false),
                anyPartAssetStillHeld = true,
                Some(recipientName),
                Some(nino.value),
                None,
                mode
              ),
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Sold to Individual journey")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersSoldCompany) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            OtherAssetsDisposalCheckAnswersUtils.viewModel(
              OtherAssetsDisposalViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasAssetDisposed = HowDisposed.Sold,
                dateAssetSold,
                otherAsset,
                assetDisposedType = Some(IdentityType.UKCompany),
                isBuyerConnectedParty,
                considerationAssetSold,
                independentValuation = Some(true),
                anyPartAssetStillHeld = true,
                Some(recipientName),
                None,
                Some(noCrnReason),
                mode
              ),
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Sold to UKCompany journey")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersSoldOther) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            OtherAssetsDisposalCheckAnswersUtils.viewModel(
              OtherAssetsDisposalViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasAssetDisposed = HowDisposed.Sold,
                dateAssetSold,
                otherAsset,
                assetDisposedType = Some(IdentityType.Other),
                isBuyerConnectedParty,
                considerationAssetSold,
                independentValuation = Some(true),
                anyPartAssetStillHeld = true,
                Some(otherRecipientName),
                Some(otherRecipientDescription),
                None,
                mode
              ),
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Sold to Other journey")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersTransferred) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            OtherAssetsDisposalCheckAnswersUtils.viewModel(
              OtherAssetsDisposalViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasAssetDisposed = HowDisposed.Transferred,
                None,
                otherAsset,
                assetDisposedType = None,
                None,
                None,
                independentValuation = None,
                anyPartAssetStillHeld = true,
                None,
                None,
                None,
                mode
              ),
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Transferred journey")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersOther) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            OtherAssetsDisposalCheckAnswersUtils.viewModel(
              OtherAssetsDisposalViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasAssetDisposed = HowDisposed.Other(otherDetails),
                None,
                otherAsset,
                assetDisposedType = None,
                None,
                None,
                independentValuation = None,
                anyPartAssetStillHeld = true,
                None,
                None,
                None,
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
          .before(
            when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
              .thenReturn(Future.successful(Some(())))
          )
          .after {
            verify(mockPsrSubmissionService, times(1))
              .submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any())
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
          page = routes.ReportedOtherAssetsDisposalListController.onPageLoad(srn, 1),
          userAnswers = userAnswersSoldPartnership
            .unsafeSet(
              OtherAssetsDisposalProgress(srn, assetIndex, disposalIndex),
              SectionJourneyStatus.InProgress("any")
            ),
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

  "BondsDisposalCYAController in view only mode" - {

    val currentUserAnswers = defaultUserAnswers
      .unsafeSet(FbVersionPage(srn), "002")
      .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex), HowDisposed.Sold)
      .unsafeSet(WhatIsOtherAssetPage(srn, assetIndex), otherAsset)
      .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex), true)
      .unsafeSet(TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex), considerationAssetSold.get)
      .unsafeSet(AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex), true)
      .unsafeSet(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex), IdentityType.UKPartnership)
      .unsafeSet(PartnershipBuyerNamePage(srn, assetIndex, disposalIndex), recipientName)
      .unsafeSet(WhenWasAssetSoldPage(srn, assetIndex, disposalIndex), dateAssetSold.get)
      .unsafeSet(IsBuyerConnectedPartyPage(srn, assetIndex, disposalIndex), isBuyerConnectedParty.get)
      .unsafeSet(OtherAssetsDisposalProgress(srn, assetIndex, disposalIndex), SectionJourneyStatus.Completed)

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")
      .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex), HowDisposed.Sold)

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            OtherAssetsDisposalCheckAnswersUtils.viewModel(
              OtherAssetsDisposalViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasAssetDisposed = HowDisposed.Sold,
                dateAssetSold,
                otherAsset,
                assetDisposedType = Some(IdentityType.UKPartnership),
                isBuyerConnectedParty,
                considerationAssetSold,
                independentValuation = Some(true),
                anyPartAssetStillHeld = true,
                Some(recipientName),
                None,
                None,
                ViewOnlyMode
              ),
              viewOnlyUpdated = false,
              optYear = Some(yearString),
              optCurrentVersion = Some(submissionNumberTwo),
              optPreviousVersion = Some(submissionNumberOne),
              isMaximumReached = false
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
          .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
        verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(using any(), any(), any())
      ).withName("Submit redirects to view only ReportedOtherAssetsDisposalListController page")
    )
  }

}
