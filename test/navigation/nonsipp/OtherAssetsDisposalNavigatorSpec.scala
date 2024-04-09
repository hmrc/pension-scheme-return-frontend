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
import config.Refined.{Max50, Max5000}
import eu.timepit.refined.refineMV
import org.scalacheck.Gen
import navigation.{Navigator, NavigatorBehaviours}
import models._

class OtherAssetsDisposalNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  override val navigator: Navigator = new NonSippNavigator

  private val assetIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  "OtherAssetsDisposalNavigator" - {

    "In NormalMode" - {

      "OtherAssetsDisposalPage" - {
        act.like(
          normalmode
            .navigateToWithData(
              OtherAssetsDisposalPage,
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
              OtherAssetsDisposalPage,
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
              WhatYouWillNeedOtherAssetsDisposalPage,
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
            .navigateToWithDoubleIndex(
              assetIndex,
              disposalIndex,
              (srn, index: Max5000, _: Max50) => OtherAssetsDisposalListPage(srn, index, disposalIndex),
              controllers.nonsipp.otherassetsdisposal.routes.HowWasAssetDisposedOfController.onPageLoad
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
              assetIndex,
              disposalIndex,
              HowWasAssetDisposedOfPage.apply,
              Gen.const(HowDisposed.Sold),
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.WhenWasAssetSoldController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from HowWasAssetDisposedOfPage to WhenWasAssetSold page")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndex,
              disposalIndex,
              HowWasAssetDisposedOfPage.apply,
              Gen.const(HowDisposed.Transferred),
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from HowWasAssetDisposedOfPage to AnyPartAssetStillHeld page (Transferred)")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndex,
              disposalIndex,
              HowWasAssetDisposedOfPage.apply,
              Gen.const(HowDisposed.Other("test details")),
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              WhenWasAssetSoldPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              TotalConsiderationSaleAssetPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              TypeOfAssetBuyerPage,
              Gen.const(IdentityType.Individual),
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.IndividualNameOfAssetBuyerController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from type of asset buyer page to IndividualNameOfAssetBuyer page when Individual")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndex,
              disposalIndex,
              TypeOfAssetBuyerPage,
              Gen.const(IdentityType.UKCompany),
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.CompanyNameOfAssetBuyerController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from type of asset buyer page to IndividualNameOfAssetBuyer page when UKCompany")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndex,
              disposalIndex,
              TypeOfAssetBuyerPage,
              Gen.const(IdentityType.UKPartnership),
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.PartnershipBuyerNameController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from type of asset buyer page to PartnershipBuyerNamePage page when UKPartnership")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              assetIndex,
              disposalIndex,
              TypeOfAssetBuyerPage,
              Gen.const(IdentityType.Other),
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.OtherBuyerDetailsController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from type of asset buyer page to OtherBuyerDetails page when other")
        )
      }

      "IndividualNameOfAssetBuyerPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndex,
              disposalIndex,
              IndividualNameOfAssetBuyerPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              AssetIndividualBuyerNiNumberPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              PartnershipBuyerNamePage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              PartnershipBuyerUtrPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              OtherBuyerDetailsPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              CompanyNameOfAssetBuyerPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              AssetCompanyBuyerCrnPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              IsBuyerConnectedPartyPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              AssetSaleIndependentValuationPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
              assetIndex,
              disposalIndex,
              AnyPartAssetStillHeldPage,
              (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                  .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
            )
            .withName("go from AnyPartAssetStillHeldPage to AssetDisposalCYA")
        )
      }

      "OtherAssetsDisposalCompletedPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndex,
              disposalIndex,
              OtherAssetsDisposalCompletedPage,
              (srn, _: Max5000, _: Max50, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.ReportedOtherAssetsDisposalListController
                  .onPageLoad(srn, page = 1)
            )
            .withName("go from Other Assets Disposal CYA page to Reported Other Assets Disposals page")
        )
      }

      "OtherAssetsDisposalCompletedPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndex,
              disposalIndex,
              (srn, _: Max5000, _: Max50) => ReportedOtherAssetsDisposalListPage(srn = srn, addDisposal = true),
              (srn, _: Max5000, _: Max50, _) =>
                controllers.nonsipp.otherassetsdisposal.routes.StartReportingAssetsDisposalController
                  .onPageLoad(srn, page = 1)
            )
            .withName(
              "go from Reported Other Assets Disposal List page to Start Reporting Assets Disposal page when addDisposal is true"
            )
        )
      }

      "OtherAssetsDisposalCompletedPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              assetIndex,
              disposalIndex,
              (srn, _: Max5000, _: Max50) => ReportedOtherAssetsDisposalListPage(srn = srn, addDisposal = false),
              (srn, _: Max5000, _: Max50, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
            )
            .withName(
              "go from Reported Other Assets Disposal List page to Start Reporting Assets Disposal page when addDisposal is false"
            )
        )
      }
    }

    "In CheckMode" - {

      "OtherAssetsDisposalCompletedPage" - {
        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              assetIndex,
              disposalIndex,
              OtherAssetsDisposalCompletedPage,
              (srn, _: Max5000, _: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            HowWasAssetDisposedOfPage.apply,
            Gen.const(HowDisposed.Sold),
            (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.WhenWasAssetSoldController
                .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
          )
          .withName("go from HowWasAssetDisposedOfPage to WhenWasAssetSold")
      )

      act.like(
        checkmode
          .navigateToWithDoubleIndexAndData(
            assetIndex,
            disposalIndex,
            HowWasAssetDisposedOfPage.apply,
            Gen.const(HowDisposed.Transferred),
            (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
                .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
          )
          .withName("go from HowWasAssetDisposedOfPage to AnyPartAssetStillHeld (Transferred)")
      )

      act.like(
        checkmode
          .navigateToWithDoubleIndexAndData(
            assetIndex,
            disposalIndex,
            HowWasAssetDisposedOfPage.apply,
            Gen.const(HowDisposed.Other("test details")),
            (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AnyPartAssetStillHeldController
                .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
          )
          .withName("ggo from HowWasAssetDisposedOfPage to AnyPartAssetStillHeld (Other)")
      )
    }

    "WhenWasAssetSoldPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndex,
            disposalIndex,
            WhenWasAssetSoldPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            AnyPartAssetStillHeldPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            AssetSaleIndependentValuationPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            TotalConsiderationSaleAssetPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            TypeOfAssetBuyerPage,
            Gen.const(IdentityType.Individual),
            (srn, index: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.IndividualNameOfAssetBuyerController
                .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
          )
          .withName("go from type of asset buyer page to IndividualNameOfAssetBuyer page when Individual")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            assetIndex,
            disposalIndex,
            TypeOfAssetBuyerPage,
            Gen.const(IdentityType.UKCompany),
            (srn, index: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.CompanyNameOfAssetBuyerController
                .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
          )
          .withName("go from type of asset buyer page to IndividualNameOfAssetBuyer page when UKCompany")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            assetIndex,
            disposalIndex,
            TypeOfAssetBuyerPage,
            Gen.const(IdentityType.UKPartnership),
            (srn, index: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.PartnershipBuyerNameController
                .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
          )
          .withName("go from type of asset buyer page to PartnershipBuyerNamePage page when UKPartnership")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            assetIndex,
            disposalIndex,
            TypeOfAssetBuyerPage,
            Gen.const(IdentityType.Other),
            (srn, index: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.OtherBuyerDetailsController
                .onPageLoad(srn, assetIndex, disposalIndex, NormalMode)
          )
          .withName("go from type of asset buyer page to OtherBuyerDetails page when other")
      )
    }

    "IndividualNameOfAssetBuyerPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            assetIndex,
            disposalIndex,
            IndividualNameOfAssetBuyerPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            AssetIndividualBuyerNiNumberPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            PartnershipBuyerNamePage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            PartnershipBuyerUtrPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            OtherBuyerDetailsPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            CompanyNameOfAssetBuyerPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            AssetCompanyBuyerCrnPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
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
            assetIndex,
            disposalIndex,
            IsBuyerConnectedPartyPage,
            (srn, assetIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.otherassetsdisposal.routes.AssetDisposalCYAController
                .onPageLoad(srn, assetIndex, disposalIndex, CheckMode)
          )
          .withName("go from IsBuyerConnectedPartyPage to AssetDisposalCYA")
      )
    }

  }

}
