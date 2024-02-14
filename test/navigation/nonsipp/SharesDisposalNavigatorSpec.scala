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

import config.Refined.{Max50, Max5000}
import eu.timepit.refined.refineMV
import models.{HowSharesDisposed, IdentityType, NormalMode}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.sharesdisposal._
import utils.BaseSpec

class SharesDisposalNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val shareIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  "SharesDisposalNavigator" - {

    "in NormalMode" - {

      "SharesDisposalPage" - {

        act.like(
          normalmode
            .navigateToWithData(
              SharesDisposalPage,
              Gen.const(true),
              (srn, _) =>
                controllers.nonsipp.sharesdisposal.routes.WhatYouWillNeedSharesDisposalController.onPageLoad(srn)
            )
            .withName("go from Shares Disposal page to What You Will Need page when yes selected")
        )

        act.like(
          normalmode
            .navigateToWithData(
              SharesDisposalPage,
              Gen.const(false),
              (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
            )
            .withName("go from Shares Disposal page to Task List page when no selected")
        )
      }

      "WhatYouWillNeedSharesDisposalPage" - {

        act.like(
          normalmode
            .navigateTo(
              WhatYouWillNeedSharesDisposalPage,
              (srn, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalListController
                  .onPageLoad(srn, 1)
            )
            .withName("go from What You Will Need page to Shares Disposal List page")
        )
      }

      "SharesDisposalListPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              SharesDisposalListPage,
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowWereSharesDisposedController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from Shares Disposal List page to How Were Shares Disposed page")
        )
      }

      "HowWereSharesDisposedPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Sold),
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.WhenWereSharesSoldController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from How Were Shares Disposed page to When Were Shares Sold page")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Redeemed),
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.WhenWereSharesRedeemedController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from How Were Shares Disposed page to When Were Shares Redeemed page")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Transferred),
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowManySharesController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from How Were Shares Disposed page to How Many Shares Held page (Transferred)")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Other("test details")),
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowManySharesController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from How Were Shares Disposed page to How Many Shares Held page (Other)")
        )
      }

      "WhenWereSharesSoldPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              WhenWereSharesSoldPage,
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowManySharesSoldController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from When Were Shares Sold page to How Many Shares Sold page")
        )
      }

      "HowManySharesSoldPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              HowManySharesSoldPage,
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.TotalConsiderationSharesSoldController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from How Many Shares Sold page to Total Consideration Shares Sold page")
        )
      }

      "TotalConsiderationSharesSoldPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              TotalConsiderationSharesSoldPage,
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.WhoWereTheSharesSoldToController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from Total Consideration Shares Sold page to Who Shares Sold To page")
        )
      }
      "WhenWereSharesRedeemedPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              WhenWereSharesRedeemedPage,
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowManySharesRedeemedController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from When Were Shares Redeemed page to How Many Shares Redeemed page")
        )
      }

      "HowManySharesRedeemedPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              HowManySharesRedeemedPage,
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.TotalConsiderationSharesRedeemedController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from How Many Shares Redeemed page to Total Consideration Shares Redeemed page")
        )
      }

      "TotalConsiderationSharesRedeemedPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              TotalConsiderationSharesRedeemedPage,
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowManySharesController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from Total Consideration Shares Redeemed page to How Many Shares page")
        )
      }

      "WhoWereTheSharesSoldToPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              WhoWereTheSharesSoldToPage,
              Gen.const(IdentityType.Individual),
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesIndividualBuyerNameController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from who where the shares sold to individual buyer name page")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              WhoWereTheSharesSoldToPage,
              Gen.const(IdentityType.UKCompany),
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.CompanyNameOfSharesBuyerController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from who where the shares sold to company buyer name page")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              WhoWereTheSharesSoldToPage,
              Gen.const(IdentityType.UKPartnership),
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerNameController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from who where the shares sold to partnership buyer name page")
        )

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              WhoWereTheSharesSoldToPage,
              Gen.const(IdentityType.Other),
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.OtherBuyerDetailsController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from who where the shares sold to other buyer details page")
        )
      }

      "SharesIndividualBuyerNamePage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              SharesIndividualBuyerNamePage,
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.IndividualBuyerNinoNumberController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from individual buyer name page to individual buyer nino number page")
        )
      }

      "IndividualBuyerNinoNumberPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              IndividualBuyerNinoNumberPage,
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from individual buyer nino number page to Is Buyer Connected Party page")
        )
      }

      "CompanyBuyerNamePage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              CompanyBuyerNamePage,
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.CompanyBuyerCrnController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from CompanyNameOfSharesBuyerPage to company buyer crn page")
        )
      }

      "CompanyBuyerCrnPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              CompanyBuyerCrnPage,
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from company buyer crn page to Is Buyer Connected Party page")
        )
      }

      "PartnershipBuyerNamePage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              PartnershipBuyerNamePage,
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerUtrController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from partnership buyer name page to Partnership Buyer UTR page")
        )

      }

      "PartnershipBuyerUtrPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              PartnershipBuyerUtrPage,
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from Partnership Buyer UTR page to Is Buyer Connected Party page")
        )
      }

      "OtherBuyerDetailsPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              OtherBuyerDetailsPage,
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from Other Buyer Details page to Is Buyer Connected Party page")
        )
      }

      "IsBuyerConnectedPartyPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              IsBuyerConnectedPartyPage,
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.IndependentValuationController
                  .onPageLoad(srn, index, disposalIndex, NormalMode)
            )
            .withName("go from Is Buyer Connected Party page to Independent Valuation page")
        )
      }

      "IndependentValuationPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              IndependentValuationPage,
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowManySharesController
                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from IndependentValuationPage to HowManySharesPage")
        )
      }

      "HowManySharesPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              HowManySharesPage,
              (srn, index: Max5000, disposalIndex: Max50, _) =>
                controllers.routes.UnauthorisedController.onPageLoad()
            )
            .withName("go from HowManySharesPage to Unauthorised")
        )
      }
    }

    "in CheckMode" - {}
  }
}
