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

package navigation.nonsipp

import utils.BaseSpec
import config.Refined.{Max5000, OneTo5000}
import models.SchemeId.Srn
import eu.timepit.refined.refineMV
import navigation.{Navigator, NavigatorBehaviours}
import models._
import pages.nonsipp.common._
import pages.nonsipp.otherassetsheld._
import models.PointOfEntry._
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen

class OtherAssetsHeldNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index = refineMV[OneTo5000](1)
  private val index2 = refineMV[OneTo5000](2)
  private val subject = IdentitySubject.OtherAssetSeller
  private val recipientDetails = RecipientDetails(
    "testName",
    "testDescription"
  )

  "OtherAssetsHeldNavigator" - {

    "in NormalMode" - {

      "OtherAssetsHeldPage" - {
        act.like(
          normalmode
            .navigateToWithData(
              OtherAssetsHeldPage,
              Gen.const(true),
              (srn, _) =>
                controllers.nonsipp.otherassetsheld.routes.WhatYouWillNeedOtherAssetsController.onPageLoad(srn)
            )
            .withName("go from other assets held page to WhatYouWillNeedOtherAssets page when yes selected")
        )

        act.like(
          normalmode
            .navigateToWithData(
              OtherAssetsHeldPage,
              Gen.const(false),
              (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
            )
            .withName("go from other assets held page to task list page when no selected")
        )
      }

      "WhatYouWillNeedOtherAssetsPage" - {
        act.like(
          normalmode
            .navigateTo(
              WhatYouWillNeedOtherAssetsPage,
              (srn, _) =>
                controllers.nonsipp.otherassetsheld.routes.WhatIsOtherAssetController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from WhatYouWillNeedOtherAssets page to WhatIsOtherAsset page"
            )
        )
      }

      "WhatIsOtherAssetPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              WhatIsOtherAssetPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.IsAssetTangibleMoveablePropertyController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from WhatIsOtherAsset page to IsAssetTangibleMoveableProperty page"
            )
        )
      }

      "IsAssetTangibleMoveablePropertyPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              IsAssetTangibleMoveablePropertyPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.WhyDoesSchemeHoldAssetsController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from IsAssetTangibleMoveablePropertyPage to WhyDoesSchemeHoldAssetsPage"
            )
        )
      }

      "WhyDoesSchemeHoldAssetsPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.WhenDidSchemeAcquireAssetsController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  WhyDoesSchemeHoldAssetsPage(srn, index),
                  SchemeHoldAsset.Acquisition
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets page to WhenDidSchemeAcquireAssets page when holding is acquisition"
            )
        )

        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.WhenDidSchemeAcquireAssetsController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  WhyDoesSchemeHoldAssetsPage(srn, index),
                  SchemeHoldAsset.Contribution
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets page to WhenDidSchemeAcquireAssets page when holding is contribution"
            )
        )

        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.CostOfOtherAssetController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  WhyDoesSchemeHoldAssetsPage(srn, index),
                  SchemeHoldAsset.Transfer
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets page to CostOfOtherAsset page when holding is transfer"
            )
        )
      }

      "WhenDidSchemeAcquireAssetsPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              WhenDidSchemeAcquireAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.common.routes.IdentityTypeController
                  .onPageLoad(srn, index, NormalMode, IdentitySubject.OtherAssetSeller),
              srn =>
                defaultUserAnswers.unsafeSet(
                  WhyDoesSchemeHoldAssetsPage(srn, index),
                  SchemeHoldAsset.Acquisition
                )
            )
            .withName(
              "go from WhenDidSchemeAcquireAssets page to IdentityType page when holding is acquisition"
            )
        )

        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              WhenDidSchemeAcquireAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.CostOfOtherAssetController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  WhyDoesSchemeHoldAssetsPage(srn, index),
                  SchemeHoldAsset.Contribution
                )
            )
            .withName(
              "go from WhenDidSchemeAcquireAssets page to CostOfOtherAsset page when holding is contribution"
            )
        )
      }

      "IdentityType navigation" - {
        act.like(
          normalmode
            .navigateToWithDataIndexAndSubjectBoth(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.Individual),
              (srn, _: Max5000, _, _) =>
                controllers.nonsipp.otherassetsheld.routes.IndividualNameOfOtherAssetSellerController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName("go from identity type page to individual seller details page")
        )

        act.like(
          normalmode
            .navigateToWithDataIndexAndSubjectBoth(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.Other),
              controllers.nonsipp.common.routes.OtherRecipientDetailsController.onPageLoad
            )
            .withName("go from identity type page to other seller details page")
        )

        act.like(
          normalmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.UKCompany),
              controllers.nonsipp.otherassetsheld.routes.CompanyNameOfOtherAssetSellerController.onPageLoad
            )
            .withName("go from identity type page to company seller name page")
        )

        act.like(
          normalmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.UKPartnership),
              controllers.nonsipp.otherassetsheld.routes.PartnershipNameOfOtherAssetsSellerController.onPageLoad
            )
            .withName("go from identity type page to UKPartnership to partnership name of seller name page")
        )
      }

      "IndividualNameOfOtherAssetSellerPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              IndividualNameOfOtherAssetSellerPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetIndividualSellerNINumberController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from IndividualNameOfOtherAssetSellerPage to OtherAssetIndividualSellerNINumberPage"
            )
        )
      }

      "CompanyNameOfOtherAssetSellerPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              CompanyNameOfOtherAssetSellerPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.common.routes.CompanyRecipientCrnController
                  .onPageLoad(srn, index, NormalMode, IdentitySubject.OtherAssetSeller)
            )
            .withName(
              "go from CompanyNameOfOtherAssetSellerPage to CompanyRecipientCrnPage"
            )
        )
      }

      "PartnershipOtherAssetSellerNamePage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              PartnershipOtherAssetSellerNamePage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.common.routes.PartnershipRecipientUtrController
                  .onPageLoad(srn, index, NormalMode, IdentitySubject.OtherAssetSeller)
            )
            .withName(
              "go from PartnershipOtherAssetSellerNamePage to PartnershipRecipientUtrPage"
            )
        )
      }

      "IndividualNameOfOtherAssetSellerPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              OtherAssetIndividualSellerNINumberPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from OtherAssetIndividualSellerNINumberPage to OtherAssetSellerConnectedPartyPage"
            )
        )
      }

      "CompanyRecipientCrnPage" - {
        act.like(
          normalmode
            .navigateToWithIndexAndSubject(
              index,
              subject,
              CompanyRecipientCrnPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from CompanyRecipientCrnPage to OtherAssetSellerConnectedPartyPage"
            )
        )
      }

      "PartnershipRecipientUtrPage" - {
        act.like(
          normalmode
            .navigateToWithIndexAndSubject(
              index,
              subject,
              PartnershipRecipientUtrPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from PartnershipRecipientUtrPage to OtherAssetSellerConnectedPartyPage"
            )
        )
      }

      "OtherRecipientDetailsPage" - {
        act.like(
          normalmode
            .navigateToWithIndexAndSubject(
              index,
              subject,
              OtherRecipientDetailsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from OtherRecipientDetailsPage to OtherAssetSellerConnectedPartyPage"
            )
        )
      }

      "OtherAssetSellerConnectedPartyPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              OtherAssetSellerConnectedPartyPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.CostOfOtherAssetController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from OtherAssetSellerConnectedPartyPage to CostOfOtherAsset page"
            )
        )
      }

      "CostOfOtherAssetPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              CostOfOtherAssetPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.IndependentValuationController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  WhyDoesSchemeHoldAssetsPage(srn, index),
                  SchemeHoldAsset.Acquisition
                )
            )
            .withName(
              "go from CostOfOtherAssetPage to IndependentValuationPage when holding is acquisition"
            )
        )

        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              CostOfOtherAssetPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.IndependentValuationController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  WhyDoesSchemeHoldAssetsPage(srn, index),
                  SchemeHoldAsset.Contribution
                )
            )
            .withName(
              "go from CostOfOtherAssetPage to IndependentValuationPage when holding is contribution"
            )
        )

        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              CostOfOtherAssetPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.IncomeFromAssetController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  WhyDoesSchemeHoldAssetsPage(srn, index),
                  SchemeHoldAsset.Transfer
                )
            )
            .withName(
              "go from CostOfOtherAssetPage to IncomeFromAssetPage when holding is transfer"
            )
        )
      }

      "IndependentValuationPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              IndependentValuationPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.IncomeFromAssetController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from IndependentValuationPage to IncomeFromAssetPage"
            )
        )
      }

      "IncomeFromAssetPage" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              index,
              IncomeFromAssetPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from IncomeFromAssetPage to OtherAssetsCYA"
            )
        )
      }

      "OtherAssetsCYAPage" - {
        act.like(
          normalmode
            .navigateTo(
              OtherAssetsCYAPage,
              (srn, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
                  .onPageLoad(srn, 1, NormalMode)
            )
            .withName(
              "go from OtherAssetsCYA to OtherAssetsList"
            )
        )
      }

      "RemoveOtherAssetPage" - {
        "When there are no other assets" - {
          act.like(
            normalmode
              .navigateToWithIndex(
                index,
                RemoveOtherAssetPage,
                (srn, _: Max5000, _) =>
                  controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController
                    .onPageLoad(srn, NormalMode)
              )
              .withName("go from RemoveOtherAssetPage to Asset page")
          )
        }

        "When there is another asset" - {
          val customUserAnswers: Srn => UserAnswers = srn =>
            defaultUserAnswers.unsafeSet(
              WhatIsOtherAssetPage(srn, index2),
              otherName
            )
          act.like(
            normalmode
              .navigateToWithIndex(
                index,
                RemoveOtherAssetPage,
                (srn, _: Max5000, _) =>
                  controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
                    .onPageLoad(srn, 1, NormalMode),
                customUserAnswers
              )
              .withName("go from RemoveOtherAssetPage to Asset list page")
          )
        }
      }
    }

    "in CheckMode" - {

      "WhatIsOtherAssetPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhatIsOtherAssetPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from WhatIsOtherAsset to OtherAssetsCYA (POE N/A)"
            )
        )
      }

      "IsAssetTangibleMoveablePropertyPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              IsAssetTangibleMoveablePropertyPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from IsAssetTangibleMoveableProperty to OtherAssetsCYA (POE N/A)"
            )
        )
      }

      "WhyDoesSchemeHoldAssetsPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetAcquisitionToContributionPointOfEntry
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets to OtherAssetsCYA (Acquisition -> Contribution)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetAcquisitionToTransferPointOfEntry
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets to OtherAssetsCYA (Acquisition -> Transfer)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.common.routes.IdentityTypeController
                  .onPageLoad(srn, index, CheckMode, IdentitySubject.OtherAssetSeller),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetContributionToAcquisitionPointOfEntry
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets to IdentityType (Contribution -> Acquisition)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetContributionToTransferPointOfEntry
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets to OtherAssetsCYA (Contribution -> Transfer)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.WhenDidSchemeAcquireAssetsController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetTransferToAcquisitionPointOfEntry
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets to WhenDidSchemeAcquireAssets (Transfer -> Acquisition)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.WhenDidSchemeAcquireAssetsController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetTransferToContributionPointOfEntry
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets to WhenDidSchemeAcquireAssets (Transfer -> Contribution)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.WhenDidSchemeAcquireAssetsController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  WhenDidSchemeAcquireAssetsPointOfEntry
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets to WhenDidSchemeAcquireAssets (when point of entry is WhenDidSchemeAcquireAssetsPointOfEntry)"
            )
        )

        // If answer is unchanged, use NoPointOfEntry to redirect to CYA
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName(
              "go from WhyDoesSchemeHoldAssets to OtherAssetsCYA (answer unchanged, NoPOE set, CYA redirect)"
            )
        )
      }

      "WhenDidSchemeAcquireAssetsPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhenDidSchemeAcquireAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.common.routes.IdentityTypeController
                  .onPageLoad(srn, index, CheckMode, IdentitySubject.OtherAssetSeller),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetTransferToAcquisitionPointOfEntry
                )
            )
            .withName(
              "go from WhenDidSchemeAcquireAssets to IdentityType (AssetTransferToAcquisitionPOE)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhenDidSchemeAcquireAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.IndependentValuationController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetTransferToContributionPointOfEntry
                )
            )
            .withName(
              "go from WhenDidSchemeAcquireAssets to IndependentValuation (AssetTransferToContributionPOE)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhenDidSchemeAcquireAssetsPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName(
              "go from WhenDidSchemeAcquireAssets to IndependentValuation (No POE)"
            )
        )
      }

      "IdentityTypePage" - {
        // No PointOfEntry tests required here, as the page that follows the IdentityType page in CheckMode is always
        // the 'SellerNamePage' for that IdentityType
        act.like(
          checkmode
            .navigateToWithDataIndexAndSubjectBoth(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.Individual),
              (srn, _: Max5000, _, _) =>
                controllers.nonsipp.otherassetsheld.routes.IndividualNameOfOtherAssetSellerController
                  .onPageLoad(srn, index, CheckMode)
            )
            .withName("go from IdentityType to IndividualNameOfOtherAssetSeller (POE N/A)")
        )

        act.like(
          checkmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.UKCompany),
              controllers.nonsipp.otherassetsheld.routes.CompanyNameOfOtherAssetSellerController.onPageLoad
            )
            .withName("go from IdentityType to CompanyNameOfOtherAssetSeller (POE N/A)")
        )

        act.like(
          checkmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.UKPartnership),
              controllers.nonsipp.otherassetsheld.routes.PartnershipNameOfOtherAssetsSellerController.onPageLoad
            )
            .withName("go from IdentityType to PartnershipNameOfOtherAssetsSeller (POE N/A)")
        )

        act.like(
          checkmode
            .navigateToWithDataIndexAndSubjectBoth(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.Other),
              controllers.nonsipp.common.routes.OtherRecipientDetailsController.onPageLoad
            )
            .withName("go from IdentityType to OtherRecipientDetails (POE N/A)")
        )
      }

      "IndividualNameOfOtherAssetSellerPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              IndividualNameOfOtherAssetSellerPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName("go from IndividualNameOfOtherAssetSeller to OtherAssetsCYA (No POE)")
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              IndividualNameOfOtherAssetSellerPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetIndividualSellerNINumberController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  WhoWasAssetAcquiredFromPointOfEntry
                )
            )
            .withName(
              "go from IndividualNameOfOtherAssetSeller to OtherAssetIndividualSellerNINumber (Any other valid POE)"
            )
        )
      }

      "CompanyNameOfOtherAssetSellerPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              CompanyNameOfOtherAssetSellerPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName("go from CompanyNameOfOtherAssetSeller to OtherAssetsCYA (No POE)")
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              CompanyNameOfOtherAssetSellerPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.common.routes.CompanyRecipientCrnController
                  .onPageLoad(srn, index, CheckMode, IdentitySubject.OtherAssetSeller),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  WhoWasAssetAcquiredFromPointOfEntry
                )
            )
            .withName("go from CompanyNameOfOtherAssetSeller to CompanyRecipientCrn (Any other POE)")
        )
      }

      "PartnershipOtherAssetSellerNamePage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              PartnershipOtherAssetSellerNamePage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName("go from PartnershipOtherAssetSellerName to OtherAssetsCYA (No POE)")
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              PartnershipOtherAssetSellerNamePage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.common.routes.PartnershipRecipientUtrController
                  .onPageLoad(srn, index, CheckMode, IdentitySubject.OtherAssetSeller),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  WhoWasAssetAcquiredFromPointOfEntry
                )
            )
            .withName("go from PartnershipOtherAssetSellerName to PartnershipRecipientUtr (Any other POE)")
        )
      }

      "OtherAssetIndividualSellerNINumberPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              OtherAssetIndividualSellerNINumberPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName("go from OtherAssetIndividualSellerNINumber to OtherAssetsCYA (No POE)")
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              OtherAssetIndividualSellerNINumberPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  WhoWasAssetAcquiredFromPointOfEntry
                )
            )
            .withName("go from OtherAssetIndividualSellerNINumber to OtherAssetsCYA (WhoWasAssetAcquiredFromPOE)")
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              OtherAssetIndividualSellerNINumberPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetContributionToAcquisitionPointOfEntry
                )
            )
            .withName(
              "go from OtherAssetIndividualSellerNINumber to OtherAssetSellerConnectedParty (AssetContributionToAcquisitionPOE)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              OtherAssetIndividualSellerNINumberPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetTransferToAcquisitionPointOfEntry
                )
            )
            .withName(
              "go from OtherAssetIndividualSellerNINumber to OtherAssetSellerConnectedParty (AssetTransferToAcquisitionPointOfEntry)"
            )
        )
      }

      "CompanyRecipientCrnPage" - {
        act.like(
          checkmode
            .navigateToWithIndexAndSubject(
              index,
              IdentitySubject.OtherAssetSeller,
              CompanyRecipientCrnPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController.onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName("go from CompanyRecipientCrn to OtherAssetsCYA (No POE)")
        )

        act.like(
          checkmode
            .navigateToWithIndexAndSubject(
              index,
              IdentitySubject.OtherAssetSeller,
              CompanyRecipientCrnPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController.onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName("go from CompanyRecipientCrn to OtherAssetsCYA (WhoWasAssetAcquiredFromPOE)")
        )

        act.like(
          checkmode
            .navigateToWithIndexAndSubject(
              index,
              IdentitySubject.OtherAssetSeller,
              CompanyRecipientCrnPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetContributionToAcquisitionPointOfEntry
                )
            )
            .withName(
              "go from CompanyRecipientCrn to OtherAssetSellerConnectedParty (AssetContributionToAcquisitionPOE)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndexAndSubject(
              index,
              IdentitySubject.OtherAssetSeller,
              CompanyRecipientCrnPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetTransferToAcquisitionPointOfEntry
                )
            )
            .withName("go from CompanyRecipientCrn to OtherAssetSellerConnectedParty (AssetTransferToAcquisitionPOE)")
        )
      }

      "PartnershipRecipientUtrPage" - {
        act.like(
          checkmode
            .navigateToWithIndexAndSubject(
              index,
              IdentitySubject.OtherAssetSeller,
              PartnershipRecipientUtrPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController.onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName("go from PartnershipRecipientUtr to OtherAssetsCYA (No POE)")
        )

        act.like(
          checkmode
            .navigateToWithIndexAndSubject(
              index,
              IdentitySubject.OtherAssetSeller,
              PartnershipRecipientUtrPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController.onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName("go from PartnershipRecipientUtr to OtherAssetsCYA (WhoWasAssetAcquiredFromPOE)")
        )

        act.like(
          checkmode
            .navigateToWithIndexAndSubject(
              index,
              IdentitySubject.OtherAssetSeller,
              PartnershipRecipientUtrPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetContributionToAcquisitionPointOfEntry
                )
            )
            .withName(
              "go from PartnershipRecipientUtr to OtherAssetSellerConnectedParty (AssetContributionToAcquisitionPOE)"
            )
        )

        act.like(
          checkmode
            .navigateToWithIndexAndSubject(
              index,
              IdentitySubject.OtherAssetSeller,
              PartnershipRecipientUtrPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetTransferToAcquisitionPointOfEntry
                )
            )
            .withName(
              "go from PartnershipRecipientUtr to OtherAssetSellerConnectedParty (AssetTransferToAcquisitionPOE)"
            )
        )
      }

      "OtherRecipientDetailsPage" - {
        act.like(
          checkmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              OtherRecipientDetailsPage,
              Gen.const(recipientDetails),
              controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController.onPageLoad,
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName("go from OtherRecipientDetails to OtherAssetsCYA (No POE)")
        )

        act.like(
          checkmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              OtherRecipientDetailsPage,
              Gen.const(recipientDetails),
              controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController.onPageLoad,
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  WhoWasAssetAcquiredFromPointOfEntry
                )
            )
            .withName("go from OtherRecipientDetails to OtherAssetsCYA (WhoWasAssetAcquiredFromPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              OtherRecipientDetailsPage,
              Gen.const(recipientDetails),
              controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController.onPageLoad,
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetContributionToAcquisitionPointOfEntry
                )
            )
            .withName(
              "go from OtherRecipientDetails to OtherAssetSellerConnectedParty (AssetContributionToAcquisitionPOE)"
            )
        )

        act.like(
          checkmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              OtherRecipientDetailsPage,
              Gen.const(recipientDetails),
              controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController.onPageLoad,
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetTransferToAcquisitionPointOfEntry
                )
            )
            .withName("go from OtherRecipientDetails to OtherAssetSellerConnectedParty (AssetTransferToAcquisitionPOE)")
        )
      }

      "OtherAssetSellerConnectedPartyPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              OtherAssetSellerConnectedPartyPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  NoPointOfEntry
                )
            )
            .withName("go from OtherAssetSellerConnectedParty to OtherAssetsCYA (No POE)")
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              OtherAssetSellerConnectedPartyPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetContributionToAcquisitionPointOfEntry
                )
            )
            .withName("go from OtherAssetSellerConnectedParty to OtherAssetsCYA (AssetContributionToAcquisitionPOE)")
        )

        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              OtherAssetSellerConnectedPartyPage,
              (srn, index: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.IndependentValuationController
                  .onPageLoad(srn, index, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  OtherAssetsCYAPointOfEntry(srn, index),
                  AssetTransferToAcquisitionPointOfEntry
                )
            )
            .withName("go from OtherAssetSellerConnectedParty to IndependentValuation (AssetTransferToAcquisitionPOE)")
        )
      }

      "CostOfOtherAssetPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              CostOfOtherAssetPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from CostOfOtherAsset to OtherAssetsCYA (POE N/A)"
            )
        )
      }

      "IndependentValuationPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              IndependentValuationPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from IndependentValuation to OtherAssetsCYA (POE N/A)"
            )
        )
      }

      "IncomeFromAssetPage" - {
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              IncomeFromAssetPage,
              (srn, _: Max5000, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsCYAController
                  .onPageLoad(srn, index, NormalMode)
            )
            .withName(
              "go from IncomeFromAsset to OtherAssetsCYA (POE N/A)"
            )
        )
      }

      "OtherAssetsCYAPage" - {
        act.like(
          checkmode
            .navigateTo(
              OtherAssetsCYAPage,
              (srn, _) =>
                controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController
                  .onPageLoad(srn, 1, CheckMode)
            )
            .withName(
              "go from OtherAssetsCYA to OtherAssetsList"
            )
        )
      }
    }
  }
}
