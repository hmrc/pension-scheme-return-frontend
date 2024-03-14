/*
 * Copyright 2023 HM Revenue & Customs
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

import config.Refined.{Max5000, OneTo5000}
import eu.timepit.refined.refineMV
import models.{IdentitySubject, IdentityType, NormalMode, SchemeHoldAsset}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.otherassetsheld._
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class OtherAssetsHeldNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index = refineMV[OneTo5000](1)
  private val subject = IdentitySubject.OtherAssetSeller

  "OtherAssetsHeldNavigator" - {
    "OtherAssetsHeldPage" - {
      act.like(
        normalmode
          .navigateToWithData(
            OtherAssetsHeldPage,
            Gen.const(true),
            (srn, _) => controllers.nonsipp.otherassetsheld.routes.WhatYouWillNeedOtherAssetsController.onPageLoad(srn)
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
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad(),
            srn =>
              defaultUserAnswers.unsafeSet(
                WhyDoesSchemeHoldAssetsPage(srn, index),
                SchemeHoldAsset.Transfer
              )
          )
          .withName(
            "go from WhyDoesSchemeHoldAssets page to Unauthorised page when holding is transfer"
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
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad(),
            srn =>
              defaultUserAnswers.unsafeSet(
                WhyDoesSchemeHoldAssetsPage(srn, index),
                SchemeHoldAsset.Contribution
              )
          )
          .withName(
            "go from WhenDidSchemeAcquireAssets page to Unauthorised page when holding is contribution"
          )
      )

      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            WhenDidSchemeAcquireAssetsPage,
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad(),
            srn =>
              defaultUserAnswers.unsafeSet(
                WhyDoesSchemeHoldAssetsPage(srn, index),
                SchemeHoldAsset.Transfer
              )
          )
          .withName(
            "go from WhenDidSchemeAcquireAssets page to Unauthorised page when holding is transfer"
          )
      )
    }

    "IdentityType navigation" - {
      "NormalMode" - {
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
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName(
            "go from OtherAssetIndividualSellerNINumberPage to Unauthorised"
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
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName(
            "go from CompanyRecipientCrnPage to Unauthorised"
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
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName(
            "go from PartnershipRecipientUtrPage to Unauthorised"
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
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName(
            "go from OtherRecipientDetailsPage to Unauthorised"
          )
      )
    }

  }
}
