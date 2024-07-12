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
import config.Refined.{OneTo50, OneTo5000}
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CheckYourAnswersView
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import controllers.nonsipp.landorpropertydisposal.LandPropertyDisposalCYAController._
import pages.nonsipp.landorpropertydisposal._
import eu.timepit.refined.refineMV
import pages.nonsipp.FbVersionPage
import models._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

import scala.concurrent.Future

class LandPropertyDisposalCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] =
    List(bind[PsrSubmissionService].toInstance(mockPsrSubmissionService))

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetails(any(), any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private def onPageLoad(mode: Mode) =
    routes.LandPropertyDisposalCYAController.onPageLoad(srn, assetIndex, disposalIndex, mode)
  private def onSubmit(mode: Mode) =
    routes.LandPropertyDisposalCYAController.onSubmit(srn, assetIndex, disposalIndex, mode)

  private lazy val onSubmitViewOnly = routes.LandPropertyDisposalCYAController.onSubmitViewOnly(
    srn,
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

  private val assetIndex = refineMV[OneTo5000](1)
  private val disposalIndex = refineMV[OneTo50](1)

  private val dateSold = Some(localDate)
  private val considerationAssetSold = Some(money)
  private val isBuyerConnectedParty = Some(true)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(HowWasPropertyDisposedOfPage(srn, assetIndex, disposalIndex), HowDisposed.Sold)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, assetIndex), address)
    .unsafeSet(LandOrPropertyStillHeldPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(TotalProceedsSaleLandPropertyPage(srn, assetIndex, disposalIndex), money)
    .unsafeSet(DisposalIndependentValuationPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, assetIndex, disposalIndex), IdentityType.UKPartnership)
    .unsafeSet(PartnershipBuyerNamePage(srn, assetIndex, disposalIndex), recipientName)
    .unsafeSet(WhenWasPropertySoldPage(srn, assetIndex, disposalIndex), dateSold.get)
    .unsafeSet(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, assetIndex, disposalIndex), isBuyerConnectedParty.get)

  "LandPropertyDisposalCYAController" - {

    List(NormalMode, CheckMode).foreach { mode =>
      act.like(
        renderView(onPageLoad(mode), userAnswers) { implicit app => implicit request =>
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
              viewOnlyUpdated = true
            )
          )
        }.withName(s"render correct $mode view for Sold journey")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .before(
            when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any(), any())(any(), any(), any()))
              .thenReturn(Future.successful(Some(())))
          )
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetails(any(), any(), any())(any(), any(), any())
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

  "LandPropertyDisposalCYAController in view only mode" - {

    val currentUserAnswers = userAnswers
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
              compilationOrSubmissionDate = Some(submissionDateTwo)
            )
          )
      }.withName("OnPageLoadViewOnly renders ok with no changed flag")
    )
    act.like(
      redirectToPage(
        onSubmitViewOnly,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(srn, yearString, submissionNumberTwo, submissionNumberOne)
      ).after(
          verify(mockPsrSubmissionService, never()).submitPsrDetails(any(), any(), any())(any(), any(), any())
        )
        .withName("Submit redirects to view only tasklist")
    )
  }
}
