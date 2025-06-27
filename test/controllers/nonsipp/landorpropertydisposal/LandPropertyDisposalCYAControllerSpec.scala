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

package controllers.nonsipp.landorpropertydisposal

import services.PsrSubmissionService
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.CheckYourAnswersView
import controllers.nonsipp.landorpropertydisposal.LandPropertyDisposalCYAController._
import pages.nonsipp.landorpropertydisposal._
import pages.nonsipp.FbVersionPage
import models._
import viewmodels.models.SectionJourneyStatus
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._
import utils.IntUtils.given
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage

import scala.concurrent.Future

class LandPropertyDisposalCYAControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] =
    List(bind[PsrSubmissionService].toInstance(mockPsrSubmissionService))

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private def onPageLoad(mode: Mode) =
    routes.LandPropertyDisposalCYAController.onPageLoad(srn, assetIndex, disposalIndex, mode)
  private def onSubmit(mode: Mode) =
    routes.LandPropertyDisposalCYAController.onSubmit(srn, assetIndex, disposalIndex, mode)

  private lazy val onSubmitViewOnly = routes.LandPropertyDisposalCYAController.onSubmitViewOnly(
    srn,
    page,
    yearString,
    submissionNumberTwo,
    submissionNumberOne
  )

  private lazy val onPageLoadViewOnly = routes.LandPropertyDisposalCYAController.onPageLoadViewOnly(
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

  private val dateSold = Some(localDate)
  private val considerationAssetSold = Some(money)
  private val isBuyerConnectedParty = Some(true)

  private val userAnswersSold = defaultUserAnswers
    .unsafeSet(HowWasPropertyDisposedOfPage(srn, assetIndex, disposalIndex), HowDisposed.Sold)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, assetIndex), address)
    .unsafeSet(LandOrPropertyStillHeldPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(TotalProceedsSaleLandPropertyPage(srn, assetIndex, disposalIndex), money)
    .unsafeSet(DisposalIndependentValuationPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, assetIndex, disposalIndex), isBuyerConnectedParty.get)
    .unsafeSet(LandOrPropertyDisposalProgress(srn, assetIndex, disposalIndex), SectionJourneyStatus.Completed)
    .unsafeSet(WhenWasPropertySoldPage(srn, assetIndex, disposalIndex), dateSold.get)

  private val userAnswersPartnershipBuyer = userAnswersSold
    .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, assetIndex, disposalIndex), IdentityType.UKPartnership)
    .unsafeSet(PartnershipBuyerNamePage(srn, assetIndex, disposalIndex), recipientName)

  private val userAnswersIndividualBuyer = userAnswersSold
    .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, assetIndex, disposalIndex), IdentityType.Individual)
    .unsafeSet(LandOrPropertyIndividualBuyerNamePage(srn, assetIndex, disposalIndex), recipientName)
    .unsafeSet(IndividualBuyerNinoNumberPage(srn, assetIndex, disposalIndex), conditionalYesNoNino)

  private val userAnswersCompanyBuyer = userAnswersSold
    .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, assetIndex, disposalIndex), IdentityType.UKCompany)
    .unsafeSet(CompanyBuyerNamePage(srn, assetIndex, disposalIndex), recipientName)
    .unsafeSet(CompanyBuyerCrnPage(srn, assetIndex, disposalIndex), ConditionalYesNo.no(noCrnReason))

  private val userAnswersOtherBuyer = userAnswersSold
    .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, assetIndex, disposalIndex), IdentityType.Other)
    .unsafeSet(OtherBuyerDetailsPage(srn, assetIndex, disposalIndex), otherRecipientDetails)

  private val userAnswersTransferred = defaultUserAnswers
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, assetIndex), address)
    .unsafeSet(HowWasPropertyDisposedOfPage(srn, assetIndex, disposalIndex), HowDisposed.Transferred)
    .unsafeSet(LandOrPropertyStillHeldPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(LandOrPropertyDisposalProgress(srn, assetIndex, disposalIndex), SectionJourneyStatus.Completed)

  private val userAnswersOtherDisposalType = defaultUserAnswers
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, assetIndex), address)
    .unsafeSet(HowWasPropertyDisposedOfPage(srn, assetIndex, disposalIndex), HowDisposed.Other(otherDetails))
    .unsafeSet(LandOrPropertyStillHeldPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(LandOrPropertyDisposalProgress(srn, assetIndex, disposalIndex), SectionJourneyStatus.Completed)

  private val incompleteUserAnswers = userAnswersPartnershipBuyer
    .unsafeSet(
      LandOrPropertyDisposalProgress(srn, assetIndex, disposalIndex),
      SectionJourneyStatus.InProgress("some-url")
    )

  "LandPropertyDisposalCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), userAnswersPartnershipBuyer) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasPropertyDisposed = HowDisposed.Sold,
                dateSold,
                address,
                landOrPropertyDisposedType = Some(IdentityType.UKPartnership),
                isBuyerConnectedParty,
                considerationAssetSold,
                independentValuation = Some(true),
                landOrPropertyStillHeld = true,
                Some(recipientName),
                None,
                None,
                mode
              ),
              srn,
              mode,
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Sold to UKPartnership journey")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersIndividualBuyer) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasPropertyDisposed = HowDisposed.Sold,
                dateSold,
                address,
                landOrPropertyDisposedType = Some(IdentityType.Individual),
                isBuyerConnectedParty,
                considerationAssetSold,
                independentValuation = Some(true),
                landOrPropertyStillHeld = true,
                Some(recipientName),
                Some(nino.value),
                None,
                mode
              ),
              srn,
              mode,
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Sold to Individual journey")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersCompanyBuyer) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasPropertyDisposed = HowDisposed.Sold,
                dateSold,
                address,
                landOrPropertyDisposedType = Some(IdentityType.UKCompany),
                isBuyerConnectedParty,
                considerationAssetSold,
                independentValuation = Some(true),
                landOrPropertyStillHeld = true,
                Some(recipientName),
                None,
                Some(noCrnReason),
                mode
              ),
              srn,
              mode,
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Sold to UKCompany journey")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersOtherBuyer) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasPropertyDisposed = HowDisposed.Sold,
                dateSold,
                address,
                landOrPropertyDisposedType = Some(IdentityType.Other),
                isBuyerConnectedParty,
                considerationAssetSold,
                independentValuation = Some(true),
                landOrPropertyStillHeld = true,
                Some(otherRecipientName),
                Some(otherRecipientDescription),
                None,
                mode
              ),
              srn,
              mode,
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Sold to Other journey")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersTransferred) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasPropertyDisposed = HowDisposed.Transferred,
                None,
                addressLookUpPage = address,
                None,
                None,
                None,
                None,
                landOrPropertyStillHeld = true,
                None,
                None,
                None,
                mode
              ),
              srn,
              mode,
              viewOnlyUpdated = true,
              isMaximumReached = false
            )
          )
        }.withName(s"render correct $mode view for Transferred journey")
      )

      act.like(
        renderView(onPageLoad(mode), userAnswersOtherDisposalType) { implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasPropertyDisposed = HowDisposed.Other(otherDetails),
                None,
                addressLookUpPage = address,
                None,
                None,
                None,
                None,
                landOrPropertyStillHeld = true,
                None,
                None,
                None,
                mode
              ),
              srn,
              mode,
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
        redirectToPage(
          onPageLoad(mode),
          routes.LandOrPropertyDisposalListController.onPageLoad(srn, 1),
          incompleteUserAnswers
        ).updateName(_ + s" - in progress redirect to disposal list page ($mode)")
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

  "LandPropertyDisposalCYAController in view only mode" - {

    val currentUserAnswers = userAnswersPartnershipBuyer
      .unsafeSet(FbVersionPage(srn), "002")

    val previousUserAnswers = currentUserAnswers
      .unsafeSet(FbVersionPage(srn), "001")

    act.like(
      renderView(onPageLoadViewOnly, userAnswers = currentUserAnswers, optPreviousAnswers = Some(previousUserAnswers)) {
        implicit app => implicit request =>
          injected[CheckYourAnswersView].apply(
            viewModel(
              ViewModelParameters(
                srn,
                assetIndex,
                disposalIndex,
                schemeName,
                howWasPropertyDisposed = HowDisposed.Sold,
                dateSold,
                address,
                landOrPropertyDisposedType = Some(IdentityType.UKPartnership),
                isBuyerConnectedParty,
                considerationAssetSold,
                independentValuation = Some(true),
                landOrPropertyStillHeld = true,
                Some(recipientName),
                None,
                None,
                ViewOnlyMode
              ),
              srn,
              ViewOnlyMode,
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
        controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController
          .onPageLoadViewOnly(srn, page, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
        verify(mockPsrSubmissionService, never()).submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any())
      ).withName("Submit redirects to land or property disposal list page")
    )
  }
}
