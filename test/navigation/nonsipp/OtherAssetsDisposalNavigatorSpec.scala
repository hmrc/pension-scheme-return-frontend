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

import pages.nonsipp.otherassetsdisposal._
import utils.BaseSpec
import config.RefinedTypes.{Max50, Max5000}
import utils.IntUtils.given
import navigation.{Navigator, NavigatorBehaviours}
import models._
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen

class OtherAssetsDisposalNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  override val navigator: Navigator = new NonSippNavigator

  private val assetIndexOne: Max5000 = 1
  private val assetIndexTwo: Max5000 = 2
  private val disposalIndex: Max50 = 1

  "OtherAssetsDisposalNavigator" - {

    "In NormalMode" - {

      "OtherAssetsDisposalPage" - {
        act.like(
          normalmode
            .navigateToWithData(
              OtherAssetsDisposalPage.apply,
              Gen.const(true),
              (srn, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.WhatYouWillNeedOtherAssetsDisposalController
                  .onPageLoad(srn)
            )
            .withName(
              "go from Did the scheme dispose of any other assets page to WhatYouWillNeedOtherAssetsDisposal page" +
                " when yes is selected"
            )
        )

        act.like(
          normalmode
            .navigateToWithData(
              OtherAssetsDisposalPage.apply,
              Gen.const(false),
              (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
            )
            .withName(
              "go from Did the scheme dispose of any other assets page to taskList page" +
                " when no is selected"
            )
        )

      }

      "WhatYouWillNeedOtherAssetsDisposalPage" - {
        act.like(
          normalmode
            .navigateTo(
              WhatYouWillNeedOtherAssetsDisposalPage.apply,
              (srn, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.StartReportingAssetsDisposalController
                  .onPageLoad(srn, page = 1)
            )
            .withName(
              "go from WhatYouWillNeedOtherAssetsDisposal page to Start reporting a disposal page"
            )
        )
      }

      "Start reporting a disposal page" - {
        act.like(
          normalmode
            .navigateToWithIndex(
              assetIndexOne,
              OtherAssetsDisposalListPage.apply,
              (srn, _: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.HowWasAssetDisposedOfController
                  .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
            )
            .withName(
              "go from start reporting a disposal page page to HowWasAssetDisposedOfPage page"
            )
        )
      }

      "HowWasAssetDisposedOfPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndexOne,
              disposalIndex,
              HowWasAssetDisposedOfPage.apply,
              Gen.const(HowDisposed.Sold),
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.WhenWasAssetSoldController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from HowWasAssetDisposedOfPage to WhenWasAssetSold page")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndexOne,
              disposalIndex,
              HowWasAssetDisposedOfPage.apply,
              Gen.const(HowDisposed.Transferred),
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from HowWasAssetDisposedOfPage to AnyPartAssetStillHeld page (Transferred)")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndexOne,
              disposalIndex,
              HowWasAssetDisposedOfPage.apply,
              Gen.const(HowDisposed.Other("test details")),
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from HowWasAssetDisposedOfPage to AnyPartAssetStillHeld page (Other)")
        )
      }

      "WhenWasAssetSoldPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              WhenWasAssetSoldPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.TotalConsiderationSaleAssetController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from when was asset sold page to TotalConsiderationSaleAsset page")
        )

      }

      "TotalConsiderationSaleAssetPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              TotalConsiderationSaleAssetPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.TypeOfAssetBuyerController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from when was asset sold page to type of asset buyer page")
        )

      }

      "TypeOfAssetBuyerPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndexOne,
              disposalIndex,
              TypeOfAssetBuyerPage.apply,
              Gen.const(IdentityType.Individual),
              (srn, _: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.IndividualNameOfAssetBuyerController
                  .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
            )
            .withName("go from type of asset buyer page to IndividualNameOfAssetBuyer page when Individual")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndexOne,
              disposalIndex,
              TypeOfAssetBuyerPage.apply,
              Gen.const(IdentityType.UKCompany),
              (srn, _: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.CompanyNameOfAssetBuyerController
                  .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
            )
            .withName("go from type of asset buyer page to IndividualNameOfAssetBuyer page when UKCompany")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndexOne,
              disposalIndex,
              TypeOfAssetBuyerPage.apply,
              Gen.const(IdentityType.UKPartnership),
              (srn, _: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.PartnershipBuyerNameController
                  .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
            )
            .withName("go from type of asset buyer page to PartnershipBuyerNamePage page when UKPartnership")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndexOne,
              disposalIndex,
              TypeOfAssetBuyerPage.apply,
              Gen.const(IdentityType.Other),
              (srn, _: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.OtherBuyerDetailsController
                  .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
            )
            .withName("go from type of asset buyer page to OtherBuyerDetails page when other")
        )
      }

      "IndividualNameOfAssetBuyerPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              IndividualNameOfAssetBuyerPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.AssetIndividualBuyerNiNumberController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from IndividualNameOfAssetBuyerPage to AssetIndividualBuyerNiNumber page")
        )
      }

      "AssetIndividualBuyerNiNumberPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              AssetIndividualBuyerNiNumberPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from AssetIndividualBuyerNiNumber to IsBuyerConnectedParty page")
        )
      }

      "PartnershipBuyerNamePage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              PartnershipBuyerNamePage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.PartnershipBuyerUtrController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from PartnershipBuyerNamePage to PartnershipBuyerUtrPage")
        )
      }

      "PartnershipBuyerUtrPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              PartnershipBuyerUtrPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from PartnershipBuyerUtrPage to IsBuyerConnectedPartyPage")
        )
      }

      "OtherBuyerDetailsPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              OtherBuyerDetailsPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from OtherBuyerDetailsPage to IsBuyerConnectedPartyPage")
        )
      }

      "CompanyNameOfAssetBuyerPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              CompanyNameOfAssetBuyerPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.AssetCompanyBuyerCrnController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from CompanyNameOfAssetBuyerPage to AssetCompanyBuyerCrnController")
        )
      }

      "AssetCompanyBuyerCrnPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              AssetCompanyBuyerCrnPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from AssetCompanyBuyerCrnPage to IsBuyerConnectedParty")
        )
      }

      "IsBuyerConnectedPartyPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              IsBuyerConnectedPartyPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.AssetSaleIndependentValuationController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from IsBuyerConnectedPartyPage to AssetSaleIndependentValuation")
        )
      }

      "AssetSaleIndependentValuationPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              AssetSaleIndependentValuationPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from AssetSaleIndependentValuationPage to AnyPartAssetStillHeldPage")
        )
      }

      "AnyPartAssetStillHeldPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              AnyPartAssetStillHeldPage.apply,
              (srn, assetIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from AnyPartAssetStillHeldPage to AssetDisposalCYA")
        )
      }

      "OtherAssetsDisposalCYAPage" - {
        act.like(
          normalmode
            .navigateTo(
              OtherAssetsDisposalCYAPage.apply,
              (srn, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                  .onPageLoad(srn, page = 1)
            )
            .withName("go from Other Assets Disposal CYA page to Reported Other Assets Disposals page")
        )
      }

      "ReportedOtherAssetsDisposalListPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              (srn, _: Max5000, _: Max50) => ReportedOtherAssetsDisposalListPage(srn = srn, addDisposal = true),
              (srn, _: Int, _: Int, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.StartReportingAssetsDisposalController
                  .onPageLoad(srn, page = 1)
            )
            .withName(
              "go from Reported Other Assets Disposal List page to Start Reporting Assets Disposal page when addDisposal is true"
            )
        )
      }

      "ReportedOtherAssetsDisposalListPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndexOne,
              disposalIndex,
              (srn, _: Max5000, _: Max50) => ReportedOtherAssetsDisposalListPage(srn = srn, addDisposal = false),
              (srn, _: Int, _: Int, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
            )
            .withName(
              "go from Reported Other Assets Disposal List page to Start Reporting Assets Disposal page when addDisposal is false"
            )
        )
      }

      "RemoveAssetDisposalPage" - {

        "When there are no other asset disposals" - {
          act.like(
            normalmode
              .navigateTo(
                RemoveAssetDisposalPage.apply,
                (srn, mode) =>
                  controllers.nonsipp.otherassetsdisposal.routes.OtherAssetsDisposalController
                    .onPageLoad(srn, mode)
              )
              .withName("go from RemoveAssetDisposal to OtherAssetsDisposal")
          )
        }

        "When there is a disposal for the same asset" - {
          act.like(
            normalmode
              .navigateTo(
                RemoveAssetDisposalPage.apply,
                (srn, _) =>
                  controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                    .onPageLoad(srn, page = 1),
                srn =>
                  defaultUserAnswers
                    .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndexOne, disposalIndex), HowDisposed.Transferred)
              )
              .withName("go from RemoveAssetDisposalPage to ReportedOtherAssetsDisposalList")
          )
        }

        "When there is a disposal for a different asset" - {
          act.like(
            normalmode
              .navigateTo(
                RemoveAssetDisposalPage.apply,
                (srn, _) =>
                  controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                    .onPageLoad(srn, page = 1),
                srn =>
                  defaultUserAnswers
                    .unsafeSet(HowWasAssetDisposedOfPage(srn, assetIndexTwo, disposalIndex), HowDisposed.Transferred)
              )
              .withName("go from RemoveAssetDisposalPage to ReportedOtherAssetsDisposalList")
          )
        }
      }
    }

    "In CheckMode" - {

      "OtherAssetsDisposalCYAPage" - {
        act.like(
          checkmode
            .navigateTo(
              OtherAssetsDisposalCYAPage.apply,
              (srn, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                  .onPageLoad(srn, page = 1)
            )
            .withName("go from Other Assets Disposal CYA to Reported Other Assets Disposals page")
        )
      }
    }
  }

  "in CheckMode" - {

    "HowWasAssetDisposedOfPage" - {

      act.like(
        checkmode
          .navigateToWithDoubleIndexAndData(
            assetIndexOne,
            disposalIndex,
            HowWasAssetDisposedOfPage.apply,
            Gen.const(HowDisposed.Sold),
            (srn, _: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.WhenWasAssetSoldController
                .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
          )
          .withName("go from HowWasAssetDisposedOfPage to WhenWasAssetSold")
      )

      act.like(
        checkmode
          .navigateToWithDoubleIndexAndData(
            assetIndexOne,
            disposalIndex,
            HowWasAssetDisposedOfPage.apply,
            Gen.const(HowDisposed.Transferred),
            (srn, _: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
                .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
          )
          .withName("go from HowWasAssetDisposedOfPage to AnyPartAssetStillHeld (Transferred)")
      )

      act.like(
        checkmode
          .navigateToWithDoubleIndexAndData(
            assetIndexOne,
            disposalIndex,
            HowWasAssetDisposedOfPage.apply,
            Gen.const(HowDisposed.Other("test details")),
            (srn, _: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
                .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
          )
          .withName("ggo from HowWasAssetDisposedOfPage to AnyPartAssetStillHeld (Other)")
      )
    }

    "WhenWasAssetSoldPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            WhenWasAssetSoldPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from when was asset sold page to TotalConsiderationSaleAsset page")
      )
    }

    "AnyPartAssetStillHeldPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            AnyPartAssetStillHeldPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from AnyPartAssetStillHeldPage to AssetDisposalCYA")
      )
    }

    "AssetSaleIndependentValuationPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            AssetSaleIndependentValuationPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from AssetSaleIndependentValuationPage to AnyPartAssetStillHeldPage")
      )
    }

    "TotalConsiderationSaleAssetPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            TotalConsiderationSaleAssetPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from when was asset sold page to type of asset buyer page")
      )

    }

    "TypeOfAssetBuyerPage" - {
      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            assetIndexOne,
            disposalIndex,
            TypeOfAssetBuyerPage.apply,
            Gen.const(IdentityType.Individual),
            (srn, _: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.IndividualNameOfAssetBuyerController
                .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
          )
          .withName("go from type of asset buyer page to IndividualNameOfAssetBuyer page when Individual")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            assetIndexOne,
            disposalIndex,
            TypeOfAssetBuyerPage.apply,
            Gen.const(IdentityType.UKCompany),
            (srn, _: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.CompanyNameOfAssetBuyerController
                .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
          )
          .withName("go from type of asset buyer page to IndividualNameOfAssetBuyer page when UKCompany")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            assetIndexOne,
            disposalIndex,
            TypeOfAssetBuyerPage.apply,
            Gen.const(IdentityType.UKPartnership),
            (srn, _: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.PartnershipBuyerNameController
                .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
          )
          .withName("go from type of asset buyer page to PartnershipBuyerNamePage page when UKPartnership")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            assetIndexOne,
            disposalIndex,
            TypeOfAssetBuyerPage.apply,
            Gen.const(IdentityType.Other),
            (srn, _: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.OtherBuyerDetailsController
                .onPageLoad(srn, assetIndexOne, disposalIndex, NormalMode)
          )
          .withName("go from type of asset buyer page to OtherBuyerDetails page when other")
      )
    }

    "IndividualNameOfAssetBuyerPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            IndividualNameOfAssetBuyerPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from IndividualNameOfAssetBuyerPage to AssetDisposalCYA page")
      )
    }

    "AssetIndividualBuyerNiNumberPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            AssetIndividualBuyerNiNumberPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from AssetIndividualBuyerNiNumber to AssetDisposalCYA page")
      )
    }

    "PartnershipBuyerNamePage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            PartnershipBuyerNamePage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from PartnershipBuyerNamePage to AssetDisposalCYA")
      )
    }

    "PartnershipBuyerUtrPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            PartnershipBuyerUtrPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from PartnershipBuyerUtrPage to AssetDisposalCYA")
      )
    }

    "OtherBuyerDetailsPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            OtherBuyerDetailsPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from OtherBuyerDetailsPage to AssetDisposalCYA Page")
      )
    }

    "CompanyNameOfAssetBuyerPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            CompanyNameOfAssetBuyerPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from CompanyNameOfAssetBuyerPage to AssetDisposalCYA page")
      )
    }

    "AssetCompanyBuyerCrnPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            AssetCompanyBuyerCrnPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from AssetCompanyBuyerCrnPage to AssetDisposalCYA")
      )
    }

    "IsBuyerConnectedPartyPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndexOne,
            disposalIndex,
            IsBuyerConnectedPartyPage.apply,
            (srn, assetIndex: Int, disposalIndex: Int, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from IsBuyerConnectedPartyPage to AssetDisposalCYA")
      )
    }

  }

}
