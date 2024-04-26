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
import config.Refined.{OneTo50, OneTo5000}
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.CheckYourAnswersView
import eu.timepit.refined.refineMV
import controllers.nonsipp.otherassetsdisposal.AssetDisposalCYAController._
import models._
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito._

import scala.concurrent.Future

class AssetDisposalCYAControllerSpec extends ControllerBaseSpec {

  private implicit val mockPsrSubmissionService: PsrSubmissionService = mock[PsrSubmissionService]

  override protected val additionalBindings: List[GuiceableModule] =
    List(bind[PsrSubmissionService].toInstance(mockPsrSubmissionService))

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  private def onPageLoad(mode: Mode) =
    routes.AssetDisposalCYAController.onPageLoad(srn, assetIndex, disposalIndex, mode)
  private def onSubmit(mode: Mode) =
    routes.AssetDisposalCYAController.onSubmit(srn, assetIndex, disposalIndex, mode)

  private val assetIndex = refineMV[OneTo5000](1)
  private val disposalIndex = refineMV[OneTo50](1)

  private val dateAssetSold = Some(localDate)
  private val considerationAssetSold = Some(money)
  private val isBuyerConnectedParty = Some(true)
  private val otherAsset = "name"

  private val userAnswers = defaultUserAnswers
    .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndex, disposalIndex), HowDisposed.Sold)
    .unsafeSet(WhatIsOtherAssetPage(srn, assetIndex), otherAsset)
    .unsafeSet(AnyPartAssetStillHeldPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(TotalConsiderationSaleAssetPage(srn, assetIndex, disposalIndex), considerationAssetSold.get)
    .unsafeSet(AssetSaleIndependentValuationPage(srn, assetIndex, disposalIndex), true)
    .unsafeSet(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex), IdentityType.UKPartnership)
    .unsafeSet(PartnershipBuyerNamePage(srn, assetIndex, disposalIndex), recipientName)
    .unsafeSet(WhenWasAssetSoldPage(srn, assetIndex, disposalIndex), dateAssetSold.get)
    .unsafeSet(IsBuyerConnectedPartyPage(srn, assetIndex, disposalIndex), isBuyerConnectedParty.get)

  "AssetDisposalCYAController" - {

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
              )
            )
          )
        }.withName(s"render correct $mode view for Sold journey")
      )

      act.like(
        redirectNextPage(onSubmit(mode))
          .after({
            verify(mockPsrSubmissionService, times(1)).submitPsrDetailsWithUA(any(), any())(any(), any(), any())
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
