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
import config.RefinedTypes.{Max50, Max5000}
import utils.IntUtils.given
import pages.nonsipp.sharesdisposal._
import models._
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen
import navigation.{Navigator, NavigatorBehaviours}
import models.HowSharesDisposed._

class SharesDisposalNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val shareIndex: Max5000 = 1
  private val disposalIndex: Max50 = 1
  private val shareIndexTwo: Max5000 = 2

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
            .navigateToWithIndex(
              shareIndex,
              SharesDisposalListPage,
              (srn, shareIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowWereSharesDisposedController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from Shares Disposal List page to How Were Shares Disposed page")
        )
      }

      "RemoveShareDisposalPage" - {

        "When there are no other share disposals" - {
          act.like(
            normalmode
              .navigateTo(
                RemoveShareDisposalPage,
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalController.onPageLoad
              )
              .withName("go from RemoveShareDisposalPage to Shares Disposal page")
          )
        }

        "When there is a disposal for the same share" - {
          act.like(
            normalmode
              .navigateTo(
                RemoveShareDisposalPage,
                (srn, _) =>
                  controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
                    .onPageLoad(srn, page = 1),
                srn =>
                  defaultUserAnswers
                    .unsafeSet(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex), HowSharesDisposed.Transferred)
              )
              .withName("go from RemoveShareDisposalPage to ReportedSharesDisposalList")
          )
        }

        "When there is a disposal for other share" - {
          act.like(
            normalmode
              .navigateTo(
                RemoveShareDisposalPage,
                (srn, _) =>
                  controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
                    .onPageLoad(srn, page = 1),
                srn =>
                  defaultUserAnswers
                    .unsafeSet(
                      HowWereSharesDisposedPage(srn, shareIndexTwo, disposalIndex),
                      HowSharesDisposed.Transferred
                    )
              )
              .withName("go from RemoveShareDisposalPage to ReportedSharesDisposalList")
          )
        }
      }

      "HowWereSharesDisposedPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Sold),
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
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
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
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
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
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
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
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
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
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
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
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
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
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
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
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
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
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
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
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
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowManySharesController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from IndependentValuationPage to HowManyDisposalSharesPage")
        )
      }

      "HowManyDisposalSharesPage" - {
        act.like(
          normalmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              HowManyDisposalSharesPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from HowManyDisposalSharesPage to Shares Disposal CYA page")
        )
      }

      "SharesDisposalCYAPage" - {

        act.like(
          normalmode
            .navigateTo(
              SharesDisposalCYAPage,
              (srn, _) =>
                controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
                  .onPageLoad(srn, page = 1)
            )
            .withName("go from SharesDisposalCYA to ReportedSharesDisposalList")
        )
      }
    }

    "in CheckMode" - {

      "HowWereSharesDisposedPage" - {
        // No PointOfEntry tests required here, as the page that follows HowWereSharesDisposedPage in CheckMode depends
        // only on the selection made on this page

        act.like(
          checkmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Sold),
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.WhenWereSharesSoldController
                  .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
            )
            .withName("go from HowWereSharesDisposed to WhenWereSharesSold (POE N/A)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Redeemed),
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.WhenWereSharesRedeemedController
                  .onPageLoad(srn, shareIndex, disposalIndex, CheckMode)
            )
            .withName("go from HowWereSharesDisposed to WhenWereSharesRedeemed (POE N/A)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Transferred),
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from HowWereSharesDisposed to CYA (Transferred) (POE N/A)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Other("test details")),
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from HowWereSharesDisposed to CYA (Other) (POE N/A)")
        )
      }

      "WhenWereSharesSoldPage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              WhenWereSharesSoldPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from WhenWereSharesSold to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              WhenWereSharesSoldPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowManySharesSoldController
                  .onPageLoad(srn, shareIndex, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from WhenWereSharesSold to HowManySharesSold (HowWereSharesDisposedPOE)")
        )
      }

      "HowManySharesSoldPage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              HowManySharesSoldPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from HowManySharesSold to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              HowManySharesSoldPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.TotalConsiderationSharesSoldController
                  .onPageLoad(srn, shareIndex, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from WhenWereSharesSold to TotalConsiderationSharesSold (HowWereSharesDisposedPOE)")
        )
      }

      "TotalConsiderationSharesSoldPage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              TotalConsiderationSharesSoldPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from TotalConsiderationSharesSold to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              TotalConsiderationSharesSoldPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.WhoWereTheSharesSoldToController
                  .onPageLoad(srn, shareIndex, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from WhenWereSharesSold to WhoWereTheSharesSoldTo (HowWereSharesDisposedPOE)")
        )
      }

      "WhoWereTheSharesSoldToPage" - {
        // No PointOfEntry tests required here, as the page that follows WhoWereTheSharesSoldToPage in CheckMode is
        // always the 'BuyerNamePage' for that IdentityType

        act.like(
          checkmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              WhoWereTheSharesSoldToPage,
              Gen.const(IdentityType.Individual),
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesIndividualBuyerNameController
                  .onPageLoad(srn, index, disposalIndex, CheckMode)
            )
            .withName("go from WhoWereTheSharesSold to IndividualBuyerName (POE N/A)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              WhoWereTheSharesSoldToPage,
              Gen.const(IdentityType.UKCompany),
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.CompanyNameOfSharesBuyerController
                  .onPageLoad(srn, index, disposalIndex, CheckMode)
            )
            .withName("go from WhoWereTheSharesSold to CompanyBuyerName")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              WhoWereTheSharesSoldToPage,
              Gen.const(IdentityType.UKPartnership),
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerNameController
                  .onPageLoad(srn, index, disposalIndex, CheckMode)
            )
            .withName("go from WhoWereTheSharesSold to PartnershipBuyerName")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndexAndData(
              shareIndex,
              disposalIndex,
              WhoWereTheSharesSoldToPage,
              Gen.const(IdentityType.Other),
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.OtherBuyerDetailsController
                  .onPageLoad(srn, index, disposalIndex, CheckMode)
            )
            .withName("go from WhoWereTheSharesSold to OtherBuyerDetails")
        )
      }

      "SharesIndividualBuyerNamePage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              SharesIndividualBuyerNamePage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from IndividualBuyerName to CYA (noPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              SharesIndividualBuyerNamePage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.IndividualBuyerNinoNumberController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from IndividualBuyerName to IndividualBuyerDetails (HowWereSharesDisposedPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              SharesIndividualBuyerNamePage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.IndividualBuyerNinoNumberController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.WhoWereTheSharesSoldToPointOfEntry
                )
            )
            .withName("go from IndividualBuyerName to IndividualBuyerDetails (WhoWereTheSharesSoldToPOE)")
        )
      }

      "IndividualBuyerNinoNumberPage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              IndividualBuyerNinoNumberPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from IndividualBuyerDetails to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              IndividualBuyerNinoNumberPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from IndividualBuyerDetails to IsBuyerConnectedParty (HowWereSharesDisposedPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              IndividualBuyerNinoNumberPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.WhoWereTheSharesSoldToPointOfEntry
                )
            )
            .withName("go from IndividualBuyerDetails to CYA (WhoWereTheSharesSoldToPOE)")
        )
      }

      "CompanyBuyerNamePage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              CompanyBuyerNamePage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from CompanyBuyerName to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              CompanyBuyerNamePage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.CompanyBuyerCrnController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from CompanyBuyerName to CompanyBuyerDetails (HowWereSharesDisposedPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              CompanyBuyerNamePage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.CompanyBuyerCrnController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.WhoWereTheSharesSoldToPointOfEntry
                )
            )
            .withName("go from CompanyBuyerName to CompanyBuyerDetails (WhoWereTheSharesSoldToPOE)")
        )
      }

      "CompanyBuyerCrnPage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              CompanyBuyerCrnPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from CompanyBuyerDetails to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              CompanyBuyerCrnPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from CompanyBuyerDetails to IsBuyerConnectedParty (HowWereSharesDisposedPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              CompanyBuyerCrnPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.WhoWereTheSharesSoldToPointOfEntry
                )
            )
            .withName("go from CompanyBuyerDetails to IsBuyerConnectedParty (WhoWereTheSharesSoldToPOE)")
        )
      }

      "PartnershipBuyerNamePage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              PartnershipBuyerNamePage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from PartnershipBuyerName to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              PartnershipBuyerNamePage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerUtrController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from PartnershipBuyerName to PartnershipBuyerDetails (HowWereSharesDisposedPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              PartnershipBuyerNamePage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.PartnershipBuyerUtrController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.WhoWereTheSharesSoldToPointOfEntry
                )
            )
            .withName("go from PartnershipBuyerName to PartnershipBuyerDetails (WhoWereTheSharesSoldToPOE)")
        )
      }

      "PartnershipBuyerUtrPage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              PartnershipBuyerUtrPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from PartnershipBuyerDetails to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              PartnershipBuyerUtrPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from PartnershipBuyerDetails to IsBuyerConnectedParty (HowWereSharesDisposedPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              PartnershipBuyerUtrPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.WhoWereTheSharesSoldToPointOfEntry
                )
            )
            .withName("go from PartnershipBuyerDetails to CYA (WhoWereTheSharesSoldToPOE)")
        )
      }

      "OtherBuyerDetailsPage" - {
        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              OtherBuyerDetailsPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from OtherBuyerDetails to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              OtherBuyerDetailsPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from OtherBuyerDetails to IsBuyerConnectedParty (HowWereSharesDisposedPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              OtherBuyerDetailsPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.WhoWereTheSharesSoldToPointOfEntry
                )
            )
            .withName("go from OtherBuyerDetails to CYA (WhoWereTheSharesSoldToPOE)")
        )
      }

      "IsBuyerConnectedPartyPage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              IsBuyerConnectedPartyPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from IsBuyerConnectedParty to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              IsBuyerConnectedPartyPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.IndependentValuationController
                  .onPageLoad(srn, index, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from IsBuyerConnectedParty to IndependentValuation (HowWereSharesDisposedPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              IsBuyerConnectedPartyPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, index, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.WhoWereTheSharesSoldToPointOfEntry
                )
            )
            .withName("go from IsBuyerConnectedParty to CYA (WhoWereTheSharesRedeemedToPOE)")
        )
      }

      "IndependentValuationPage" - {
        // In CheckMode, CYA page always follows this page, so no PointOfEntry tests required

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              IndependentValuationPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from IndependentValuationPage to CYA page")
        )
      }

      "WhenWereSharesRedeemedPage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              WhenWereSharesRedeemedPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from WhenWereSharesRedeemed to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              WhenWereSharesRedeemedPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.HowManySharesRedeemedController
                  .onPageLoad(srn, shareIndex, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from WhenWereSharesRedeemed to HowManySharesRedeemed (HowWereSharesDisposedPOE)")
        )
      }

      "HowManySharesRedeemedPage" - {

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              HowManySharesRedeemedPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.NoPointOfEntry
                )
            )
            .withName("go from HowManySharesRedeemed to CYA (NoPOE)")
        )

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              HowManySharesRedeemedPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.TotalConsiderationSharesRedeemedController
                  .onPageLoad(srn, shareIndex, disposalIndex, CheckMode),
              srn =>
                defaultUserAnswers.unsafeSet(
                  SharesDisposalCYAPointOfEntry(srn, shareIndex, disposalIndex),
                  PointOfEntry.HowWereSharesDisposedPointOfEntry
                )
            )
            .withName("go from WhenWereSharesRedeemed to TotalConsiderationSharesRedeemed (HowWereSharesDisposedPOE)")
        )
      }

      "TotalConsiderationSharesRedeemedPage" - {
        // In CheckMode, CYA page always follows this page, so no PointOfEntry tests required

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              TotalConsiderationSharesRedeemedPage,
              (srn, shareIndex: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from Total Consideration Shares Redeemed page to CYA page")
        )
      }

      "HowManyDisposalSharesPage" - {
        // In CheckMode, CYA page always follows this page, so no PointOfEntry tests required

        act.like(
          checkmode
            .navigateToWithDoubleIndex(
              shareIndex,
              disposalIndex,
              HowManyDisposalSharesPage,
              (srn, index: Int, disposalIndex: Int, _) =>
                controllers.nonsipp.sharesdisposal.routes.SharesDisposalCYAController
                  .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from HowManyDisposalSharesPage to Shares Disposal CYA page")
        )
      }

      "SharesDisposalCYAPage" - {

        act.like(
          checkmode
            .navigateTo(
              SharesDisposalCYAPage,
              (srn, _) =>
                controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController
                  .onPageLoad(srn, page = 1)
            )
            .withName("go from SharesDisposalCYA to ReportedSharesDisposalList")
        )
      }
    }
  }
}
