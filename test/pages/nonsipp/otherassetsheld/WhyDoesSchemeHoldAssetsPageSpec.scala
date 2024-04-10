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

package pages.nonsipp.otherassetsheld

import models.PointOfEntry._
import config.Refined.OneTo5000
import controllers.TestValues
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import uk.gov.hmrc.domain.Nino
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}
import viewmodels.models.SectionCompleted
import pages.behaviours.PageBehaviours
import models._
import models.SchemeHoldAsset._

class WhyDoesSchemeHoldAssetsPageSpec extends PageBehaviours with TestValues {

  "WhyDoesSchemeHoldAssetsPage" - {

    val index = refineMV[OneTo5000](1)
    val srn = srnGen.sample.value
    val subject = IdentitySubject.OtherAssetSeller
    val recipientDetails = RecipientDetails(otherRecipientName, otherRecipientDescription)

    beRetrievable[SchemeHoldAsset](WhyDoesSchemeHoldAssetsPage(srn, index))

    beSettable[SchemeHoldAsset](WhyDoesSchemeHoldAssetsPage(srn, index))

    beRemovable[SchemeHoldAsset](WhyDoesSchemeHoldAssetsPage(srn, index))

    "cleanup" - {

      "dependent data for Acquisition -> Contribution" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
          .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), false)
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Acquisition)
          .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
          .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.Individual)
          .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, index), individualName)
          .unsafeSet(
            OtherAssetIndividualSellerNINumberPage(srn, index),
            ConditionalYesNo.no[String, Nino](noninoReason)
          )
          .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index), true)
          .unsafeSet(CostOfOtherAssetPage(srn, index), money)
          .unsafeSet(IndependentValuationPage(srn, index), false)
          .unsafeSet(IncomeFromAssetPage(srn, index), money)
          .unsafeSet(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)

        val result = userAnswers
          .set(WhyDoesSchemeHoldAssetsPage(srn, index), Contribution)
          .success
          .value
          .set(OtherAssetsCYAPointOfEntry(srn, index), AssetAcquisitionToContributionPointOfEntry)
          .success
          .value

        // Updated fields
        result.get(WhyDoesSchemeHoldAssetsPage(srn, index)) must be(Some(Contribution))
        result.get(OtherAssetsCYAPointOfEntry(srn, index)) must be(Some(AssetAcquisitionToContributionPointOfEntry))
        // Cleaned up fields
        result.get(IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller)) must be(empty)
        result.get(IndividualNameOfOtherAssetSellerPage(srn, index)) must be(empty)
        result.get(OtherAssetIndividualSellerNINumberPage(srn, index)) must be(empty)
        result.get(OtherAssetSellerConnectedPartyPage(srn, index)) must be(empty)
        // Retained fields
        result.get(OtherAssetsHeldPage(srn)) must be(Some(true))
        result.get(WhatIsOtherAssetPage(srn, index)) must be(Some(otherAssetDescription))
        result.get(IsAssetTangibleMoveablePropertyPage(srn, index)) must be(Some(false))
        result.get(WhenDidSchemeAcquireAssetsPage(srn, index)) must be(Some(localDate))
        result.get(CostOfOtherAssetPage(srn, index)) must be(Some(money))
        result.get(IndependentValuationPage(srn, index)) must be(Some(false))
        result.get(IncomeFromAssetPage(srn, index)) must be(Some(money))
        result.get(OtherAssetsCompleted(srn, index)) must be(Some(SectionCompleted))
      }

      "dependent data for Acquisition -> Transfer" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
          .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), false)
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Acquisition)
          .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
          .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.Other)
          .unsafeSet(OtherRecipientDetailsPage(srn, index, IdentitySubject.OtherAssetSeller), recipientDetails)
          .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index), true)
          .unsafeSet(CostOfOtherAssetPage(srn, index), money)
          .unsafeSet(IndependentValuationPage(srn, index), false)
          .unsafeSet(IncomeFromAssetPage(srn, index), money)
          .unsafeSet(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)

        val result = userAnswers
          .set(WhyDoesSchemeHoldAssetsPage(srn, index), Transfer)
          .success
          .value
          .set(OtherAssetsCYAPointOfEntry(srn, index), AssetAcquisitionToTransferPointOfEntry)
          .success
          .value

        // Updated fields
        result.get(WhyDoesSchemeHoldAssetsPage(srn, index)) must be(Some(Transfer))
        result.get(OtherAssetsCYAPointOfEntry(srn, index)) must be(Some(AssetAcquisitionToTransferPointOfEntry))
        // Cleaned up fields
        result.get(WhenDidSchemeAcquireAssetsPage(srn, index)) must be(empty)
        result.get(IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller)) must be(empty)
        result.get(OtherAssetSellerConnectedPartyPage(srn, index)) must be(empty)
        result.get(IndependentValuationPage(srn, index)) must be(empty)
        // Retained fields
        result.get(OtherAssetsHeldPage(srn)) must be(Some(true))
        result.get(WhatIsOtherAssetPage(srn, index)) must be(Some(otherAssetDescription))
        result.get(IsAssetTangibleMoveablePropertyPage(srn, index)) must be(Some(false))
        result.get(CostOfOtherAssetPage(srn, index)) must be(Some(money))
        result.get(IncomeFromAssetPage(srn, index)) must be(Some(money))
        result.get(OtherAssetsCompleted(srn, index)) must be(Some(SectionCompleted))
      }

      "dependent data for Contribution -> Acquisition" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
          .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), false)
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Contribution)
          .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
          .unsafeSet(CostOfOtherAssetPage(srn, index), money)
          .unsafeSet(IndependentValuationPage(srn, index), false)
          .unsafeSet(IncomeFromAssetPage(srn, index), money)
          .unsafeSet(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)

        val result = userAnswers
          .set(WhyDoesSchemeHoldAssetsPage(srn, index), Acquisition)
          .success
          .value
          .set(OtherAssetsCYAPointOfEntry(srn, index), AssetContributionToAcquisitionPointOfEntry)
          .success
          .value

        // Updated fields
        result.get(WhyDoesSchemeHoldAssetsPage(srn, index)) must be(Some(Acquisition))
        result.get(OtherAssetsCYAPointOfEntry(srn, index)) must be(Some(AssetContributionToAcquisitionPointOfEntry))
        // Cleaned up fields - none
        // Retained fields
        result.get(OtherAssetsHeldPage(srn)) must be(Some(true))
        result.get(WhatIsOtherAssetPage(srn, index)) must be(Some(otherAssetDescription))
        result.get(WhenDidSchemeAcquireAssetsPage(srn, index)) must be(Some(localDate))
        result.get(IsAssetTangibleMoveablePropertyPage(srn, index)) must be(Some(false))
        result.get(CostOfOtherAssetPage(srn, index)) must be(Some(money))
        result.get(IndependentValuationPage(srn, index)) must be(Some(false))
        result.get(IncomeFromAssetPage(srn, index)) must be(Some(money))
        result.get(OtherAssetsCompleted(srn, index)) must be(Some(SectionCompleted))
      }

      "dependent data for Contribution -> Transfer" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
          .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), false)
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Contribution)
          .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
          .unsafeSet(CostOfOtherAssetPage(srn, index), money)
          .unsafeSet(IndependentValuationPage(srn, index), false)
          .unsafeSet(IncomeFromAssetPage(srn, index), money)
          .unsafeSet(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)

        val result = userAnswers
          .set(WhyDoesSchemeHoldAssetsPage(srn, index), Transfer)
          .success
          .value
          .set(OtherAssetsCYAPointOfEntry(srn, index), AssetContributionToTransferPointOfEntry)
          .success
          .value

        // Updated fields
        result.get(WhyDoesSchemeHoldAssetsPage(srn, index)) must be(Some(Transfer))
        result.get(OtherAssetsCYAPointOfEntry(srn, index)) must be(Some(AssetContributionToTransferPointOfEntry))
        // Cleaned up fields
        result.get(WhenDidSchemeAcquireAssetsPage(srn, index)) must be(empty)
        result.get(IndependentValuationPage(srn, index)) must be(empty)
        // Retained fields
        result.get(OtherAssetsHeldPage(srn)) must be(Some(true))
        result.get(WhatIsOtherAssetPage(srn, index)) must be(Some(otherAssetDescription))
        result.get(IsAssetTangibleMoveablePropertyPage(srn, index)) must be(Some(false))
        result.get(CostOfOtherAssetPage(srn, index)) must be(Some(money))
        result.get(IncomeFromAssetPage(srn, index)) must be(Some(money))
        result.get(OtherAssetsCompleted(srn, index)) must be(Some(SectionCompleted))
      }

      "dependent data for Transfer -> Acquisition" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
          .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), false)
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Transfer)
          .unsafeSet(CostOfOtherAssetPage(srn, index), money)
          .unsafeSet(IncomeFromAssetPage(srn, index), money)
          .unsafeSet(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)

        val result = userAnswers
          .set(WhyDoesSchemeHoldAssetsPage(srn, index), Acquisition)
          .success
          .value
          .set(OtherAssetsCYAPointOfEntry(srn, index), AssetTransferToAcquisitionPointOfEntry)
          .success
          .value

        // Updated fields
        result.get(WhyDoesSchemeHoldAssetsPage(srn, index)) must be(Some(Acquisition))
        result.get(OtherAssetsCYAPointOfEntry(srn, index)) must be(Some(AssetTransferToAcquisitionPointOfEntry))
        // Cleaned up fields - none
        // Retained fields
        result.get(OtherAssetsHeldPage(srn)) must be(Some(true))
        result.get(WhatIsOtherAssetPage(srn, index)) must be(Some(otherAssetDescription))
        result.get(IsAssetTangibleMoveablePropertyPage(srn, index)) must be(Some(false))
        result.get(CostOfOtherAssetPage(srn, index)) must be(Some(money))
        result.get(IncomeFromAssetPage(srn, index)) must be(Some(money))
        result.get(OtherAssetsCompleted(srn, index)) must be(Some(SectionCompleted))
      }

      "dependent data for Transfer -> Contribution" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
          .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), false)
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Transfer)
          .unsafeSet(CostOfOtherAssetPage(srn, index), money)
          .unsafeSet(IncomeFromAssetPage(srn, index), money)
          .unsafeSet(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)

        val result = userAnswers
          .set(WhyDoesSchemeHoldAssetsPage(srn, index), Contribution)
          .success
          .value
          .set(OtherAssetsCYAPointOfEntry(srn, index), AssetTransferToContributionPointOfEntry)
          .success
          .value

        // Updated fields
        result.get(WhyDoesSchemeHoldAssetsPage(srn, index)) must be(Some(Contribution))
        result.get(OtherAssetsCYAPointOfEntry(srn, index)) must be(Some(AssetTransferToContributionPointOfEntry))
        // Cleaned up fields - none
        // Retained fields
        result.get(OtherAssetsHeldPage(srn)) must be(Some(true))
        result.get(WhatIsOtherAssetPage(srn, index)) must be(Some(otherAssetDescription))
        result.get(IsAssetTangibleMoveablePropertyPage(srn, index)) must be(Some(false))
        result.get(CostOfOtherAssetPage(srn, index)) must be(Some(money))
        result.get(IncomeFromAssetPage(srn, index)) must be(Some(money))
        result.get(OtherAssetsCompleted(srn, index)) must be(Some(SectionCompleted))
      }

      "nothing for unchanged answer (Acquisition)" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
          .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), false)
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Acquisition)
          .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
          .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.Individual)
          .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, index), individualName)
          .unsafeSet(
            OtherAssetIndividualSellerNINumberPage(srn, index),
            ConditionalYesNo.no[String, Nino](noninoReason)
          )
          .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index), true)
          .unsafeSet(CostOfOtherAssetPage(srn, index), money)
          .unsafeSet(IndependentValuationPage(srn, index), false)
          .unsafeSet(IncomeFromAssetPage(srn, index), money)
          .unsafeSet(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)

        val result = userAnswers
          .set(WhyDoesSchemeHoldAssetsPage(srn, index), Acquisition)
          .success
          .value
          .set(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .success
          .value

        // Retained fields - all
        result.get(OtherAssetsHeldPage(srn)) must be(Some(true))
        result.get(WhatIsOtherAssetPage(srn, index)) must be(Some(otherAssetDescription))
        result.get(IsAssetTangibleMoveablePropertyPage(srn, index)) must be(Some(false))
        result.get(WhyDoesSchemeHoldAssetsPage(srn, index)) must be(Some(Acquisition))
        result.get(WhenDidSchemeAcquireAssetsPage(srn, index)) must be(Some(localDate))
        result.get(IdentityTypePage(srn, index, subject)) must be(Some(IdentityType.Individual))
        result.get(IndividualNameOfOtherAssetSellerPage(srn, index)) must be(Some(individualName))
        result.get(OtherAssetIndividualSellerNINumberPage(srn, index)) must be(
          Some(ConditionalYesNo.no[String, Nino](noninoReason))
        )
        result.get(OtherAssetSellerConnectedPartyPage(srn, index)) must be(Some(true))
        result.get(CostOfOtherAssetPage(srn, index)) must be(Some(money))
        result.get(IndependentValuationPage(srn, index)) must be(Some(false))
        result.get(IncomeFromAssetPage(srn, index)) must be(Some(money))
        result.get(OtherAssetsCYAPointOfEntry(srn, index)) must be(Some(NoPointOfEntry))
        result.get(OtherAssetsCompleted(srn, index)) must be(Some(SectionCompleted))
      }

      "nothing for unchanged answer (Contribution)" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
          .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), false)
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Contribution)
          .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
          .unsafeSet(CostOfOtherAssetPage(srn, index), money)
          .unsafeSet(IndependentValuationPage(srn, index), false)
          .unsafeSet(IncomeFromAssetPage(srn, index), money)
          .unsafeSet(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)

        val result = userAnswers
          .set(WhyDoesSchemeHoldAssetsPage(srn, index), Contribution)
          .success
          .value
          .set(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .success
          .value

        // Retained fields - all
        result.get(OtherAssetsHeldPage(srn)) must be(Some(true))
        result.get(WhatIsOtherAssetPage(srn, index)) must be(Some(otherAssetDescription))
        result.get(IsAssetTangibleMoveablePropertyPage(srn, index)) must be(Some(false))
        result.get(WhyDoesSchemeHoldAssetsPage(srn, index)) must be(Some(Contribution))
        result.get(WhenDidSchemeAcquireAssetsPage(srn, index)) must be(Some(localDate))
        result.get(CostOfOtherAssetPage(srn, index)) must be(Some(money))
        result.get(IndependentValuationPage(srn, index)) must be(Some(false))
        result.get(IncomeFromAssetPage(srn, index)) must be(Some(money))
        result.get(OtherAssetsCYAPointOfEntry(srn, index)) must be(Some(NoPointOfEntry))
        result.get(OtherAssetsCompleted(srn, index)) must be(Some(SectionCompleted))
      }

      "nothing for unchanged answer (Transfer)" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
          .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), false)
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Transfer)
          .unsafeSet(CostOfOtherAssetPage(srn, index), money)
          .unsafeSet(IncomeFromAssetPage(srn, index), money)
          .unsafeSet(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)

        val result = userAnswers
          .set(WhyDoesSchemeHoldAssetsPage(srn, index), Transfer)
          .success
          .value
          .set(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .success
          .value

        // Retained fields - all
        result.get(OtherAssetsHeldPage(srn)) must be(Some(true))
        result.get(WhatIsOtherAssetPage(srn, index)) must be(Some(otherAssetDescription))
        result.get(IsAssetTangibleMoveablePropertyPage(srn, index)) must be(Some(false))
        result.get(WhyDoesSchemeHoldAssetsPage(srn, index)) must be(Some(Transfer))
        result.get(CostOfOtherAssetPage(srn, index)) must be(Some(money))
        result.get(IncomeFromAssetPage(srn, index)) must be(Some(money))
        result.get(OtherAssetsCYAPointOfEntry(srn, index)) must be(Some(NoPointOfEntry))
        result.get(OtherAssetsCompleted(srn, index)) must be(Some(SectionCompleted))
      }

      "all data for removal" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(OtherAssetsHeldPage(srn), true)
          .unsafeSet(WhatIsOtherAssetPage(srn, index), otherAssetDescription)
          .unsafeSet(IsAssetTangibleMoveablePropertyPage(srn, index), false)
          .unsafeSet(WhyDoesSchemeHoldAssetsPage(srn, index), Acquisition)
          .unsafeSet(WhenDidSchemeAcquireAssetsPage(srn, index), localDate)
          .unsafeSet(IdentityTypePage(srn, index, subject), IdentityType.Individual)
          .unsafeSet(IndividualNameOfOtherAssetSellerPage(srn, index), individualName)
          .unsafeSet(
            OtherAssetIndividualSellerNINumberPage(srn, index),
            ConditionalYesNo.no[String, Nino](noninoReason)
          )
          .unsafeSet(OtherAssetSellerConnectedPartyPage(srn, index), true)
          .unsafeSet(CostOfOtherAssetPage(srn, index), money)
          .unsafeSet(IndependentValuationPage(srn, index), false)
          .unsafeSet(IncomeFromAssetPage(srn, index), money)
          .unsafeSet(OtherAssetsCYAPointOfEntry(srn, index), NoPointOfEntry)
          .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)

        val result = userAnswers
          .remove(WhyDoesSchemeHoldAssetsPage(srn, index))
          .success
          .value

        // Cleaned up fields
        result.get(WhyDoesSchemeHoldAssetsPage(srn, index)) must be(empty)
        result.get(WhenDidSchemeAcquireAssetsPage(srn, index)) must be(empty)
        result.get(IdentityTypePage(srn, index, subject)) must be(empty)
        result.get(IndividualNameOfOtherAssetSellerPage(srn, index)) must be(empty)
        result.get(OtherAssetIndividualSellerNINumberPage(srn, index)) must be(empty)
        result.get(OtherAssetSellerConnectedPartyPage(srn, index)) must be(empty)
        result.get(IndependentValuationPage(srn, index)) must be(empty)
        // Retained fields
        result.get(OtherAssetsHeldPage(srn)) must be(Some(true))
        result.get(WhatIsOtherAssetPage(srn, index)) must be(Some(otherAssetDescription))
        result.get(IsAssetTangibleMoveablePropertyPage(srn, index)) must be(Some(false))
        result.get(CostOfOtherAssetPage(srn, index)) must be(Some(money))
        result.get(IncomeFromAssetPage(srn, index)) must be(Some(money))
        result.get(OtherAssetsCYAPointOfEntry(srn, index)) must be(Some(NoPointOfEntry))
        result.get(OtherAssetsCompleted(srn, index)) must be(Some(SectionCompleted))
      }
    }
  }
}
