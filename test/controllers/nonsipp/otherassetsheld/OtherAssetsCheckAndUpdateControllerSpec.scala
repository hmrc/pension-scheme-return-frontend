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

package controllers.nonsipp.otherassetsheld

import play.api.mvc.Call
import pages.nonsipp.otherassetsheld._
import utils.nonsipp.summary.{OtherAssetsCheckAnswersUtils, OtherAssetsViewModelParameters}
import views.html.PrePopCheckYourAnswersView
import utils.IntUtils.given
import config.Constants.incomplete
import uk.gov.hmrc.domain.Nino
import pages.nonsipp.common.IdentityTypePage
import viewmodels.models.SectionJourneyStatus
import config.RefinedTypes.OneTo5000
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import models._
import models.SchemeHoldAsset._
import controllers.nonsipp.otherassetsheld.OtherAssetsCheckAndUpdateController.viewModel
import eu.timepit.refined.api.Refined

class OtherAssetsCheckAndUpdateControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index: Refined[Int, OneTo5000] = 1

  private def onPageLoad: Call = routes.OtherAssetsCheckAndUpdateController.onPageLoad(srn, index)
  private def onSubmit: Call = routes.OtherAssetsCheckAndUpdateController.onSubmit(srn, index)

  private val prePodDataMissingUserAnswers = defaultUserAnswers
    .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
    .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), SchemeHoldAsset.Acquisition)
    .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
    .unsafeSet(IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller), IdentityType.Individual)
    .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, index), individualName)
    .unsafeSet(OtherAssetIndividualSellerNINumberPage(srn, index), ConditionalYesNo.yes[String, Nino](nino))
    .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index), true)
    .unsafeSet(CostOfOtherAssetPage(srn, index), money)
    .unsafeSet(IndependentValuationPage(srn, index), true)
    .unsafeSet(OtherAssetsProgress(srn, index), SectionJourneyStatus.Completed)

  private val completedUserAnswers = prePodDataMissingUserAnswers
    .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), true)
    .unsafeSet(IncomeFromAssetPage(srn, index), money)

  "OtherAssetsCheckAndUpdateController" - {

    act.like(
      renderView(onPageLoad, prePodDataMissingUserAnswers) { implicit app => implicit request =>
        val sections = OtherAssetsCheckAnswersUtils
          .viewModel(
            parameters = OtherAssetsViewModelParameters(
              srn = srn,
              index = index,
              schemeName = schemeName,
              description = otherAssetDescription,
              isTangibleMoveableProperty = Left(incomplete),
              whyHeld = SchemeHoldAsset.Acquisition,
              acquisitionOrContributionDate = Some(localDate),
              sellerIdentityType = Some(IdentityType.Individual),
              sellerName = Some(individualName),
              sellerDetails = Some(nino.toString),
              sellerReasonNoDetails = None,
              isSellerConnectedParty = Some(true),
              totalCost = money,
              isIndependentValuation = Some(true),
              totalIncome = Left(incomplete),
              mode = NormalMode
            ),
            viewOnlyUpdated = false
          )
          .page
          .sections

        injected[PrePopCheckYourAnswersView].apply(
          viewModel(
            srn = srn,
            index = index,
            sections = sections
          )
        )
      }.withName(s"render correct view when prePopulation data missing")
    )

    act.like(
      renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
        val sections = OtherAssetsCheckAnswersUtils
          .viewModel(
            parameters = OtherAssetsViewModelParameters(
              srn = srn,
              index = index,
              schemeName = schemeName,
              description = otherAssetDescription,
              isTangibleMoveableProperty = Right(true),
              whyHeld = SchemeHoldAsset.Acquisition,
              acquisitionOrContributionDate = Some(localDate),
              sellerIdentityType = Some(IdentityType.Individual),
              sellerName = Some(individualName),
              sellerDetails = Some(nino.toString),
              sellerReasonNoDetails = None,
              isSellerConnectedParty = Some(true),
              totalCost = money,
              isIndependentValuation = Some(true),
              totalIncome = Right(money),
              mode = NormalMode
            ),
            viewOnlyUpdated = false
          )
          .page
          .sections

        injected[PrePopCheckYourAnswersView].apply(
          viewModel(
            srn = srn,
            index = index,
            sections = sections
          )
        )
      }.withName(s"render correct view when data complete")
    )

    act.like(
      redirectToPage(onSubmit, routes.IsAssetTangibleMoveablePropertyController.onPageLoad(srn, index, NormalMode))
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .updateName("onPageLoad" + _)
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .updateName("onSubmit" + _)
    )
  }
}
