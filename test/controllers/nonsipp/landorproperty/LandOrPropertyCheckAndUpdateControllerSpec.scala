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

package controllers.nonsipp.landorproperty

import utils.nonsipp.summary.LandOrPropertyCheckAnswersUtils
import views.html.PrePopCheckYourAnswersView
import config.Constants.incomplete
import models.{IdentitySubject, _}
import pages.nonsipp.common.IdentityTypePage
import eu.timepit.refined.api.Refined
import config.RefinedTypes.OneTo5000
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import utils.IntUtils.given
import pages.nonsipp.landorproperty._

class LandOrPropertyCheckAndUpdateControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index: Refined[Int, OneTo5000] = 1

  private def onPageLoad = routes.LandOrPropertyCheckAndUpdateController.onPageLoad(srn, index)
  private def onSubmit = routes.LandOrPropertyCheckAndUpdateController.onSubmit(srn, index)

  private val schemeHoldLandProperty = schemeHoldLandPropertyGen.sample.value

  private val prePodDataMissingUserAnswers = defaultUserAnswers
    .unsafeSet(LandPropertyInUKPage(srn, index), true)
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
    .unsafeSet(LandRegistryTitleNumberPage(srn, index), ConditionalYesNo.no("reason"))
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), schemeHoldLandProperty)
    .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index), localDate)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index), money)
    .unsafeSet(LandPropertyIndependentValuationPage(srn, index), false)
    .unsafeSet(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller), IdentityType.Individual)
    .unsafeSet(LandPropertyIndividualSellersNamePage(srn, index), recipientName)
    .unsafeSet(IndividualSellerNiPage(srn, index), ConditionalYesNo.no("reason"))
    .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index), false)

  private val completedUserAnswers = prePodDataMissingUserAnswers
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, index), true)
    .unsafeSet(IsLandPropertyLeasedPage(srn, index), true)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, index), money)
    .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index), ("Lessee Name", money, localDate))
    .unsafeSet(IsLesseeConnectedPartyPage(srn, index), false)

  "LandOrPropertyCheckAndUpdateController" - {

    act.like(
      renderView(onPageLoad, prePodDataMissingUserAnswers) { implicit app => implicit request =>
        injected[PrePopCheckYourAnswersView].apply(
          LandOrPropertyCheckAndUpdateController.viewModel(
            srn = srn,
            index = index,
            sections = LandOrPropertyCheckAnswersUtils
              .viewModel(
                srn,
                index,
                schemeName,
                true,
                ConditionalYesNo.no("reason"),
                schemeHoldLandProperty,
                Some(localDate),
                money,
                Some(false),
                Some(IdentityType.Individual),
                Some(recipientName),
                None,
                Some("reason"),
                Some(false),
                Left(incomplete),
                Left(incomplete),
                Left(incomplete),
                address,
                None,
                NormalMode,
                true,
                None,
                None,
                None
              )
              .page
              .sections
          )
        )
      }.withName(s"render correct view when prePopulation data missing")
    )

    act.like(
      renderView(onPageLoad, completedUserAnswers) { implicit app => implicit request =>
        injected[PrePopCheckYourAnswersView].apply(
          LandOrPropertyCheckAndUpdateController.viewModel(
            srn = srn,
            index = index,
            sections = LandOrPropertyCheckAnswersUtils
              .viewModel(
                srn,
                index,
                schemeName,
                true,
                ConditionalYesNo.no("reason"),
                schemeHoldLandProperty,
                Some(localDate),
                money,
                Some(false),
                Some(IdentityType.Individual),
                Some(recipientName),
                None,
                Some("reason"),
                Some(false),
                Right(true),
                Right(true),
                Right(money),
                address,
                Some((Right("Lessee Name"), Right(money), Right(localDate), Right(false))),
                NormalMode,
                true,
                None,
                None,
                None
              )
              .page
              .sections
          )
        )
      }.withName(s"render correct view when data complete")
    )

    act.like(
      redirectToPage(onSubmit, routes.IsLandOrPropertyResidentialController.onPageLoad(srn, index, NormalMode))
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
