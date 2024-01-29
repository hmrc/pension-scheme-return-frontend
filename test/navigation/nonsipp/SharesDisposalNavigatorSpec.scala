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
import models.HowSharesDisposed
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
              (srn, _) => controllers.routes.UnauthorisedController.onPageLoad()
              //                controllers.nonsipp.sharesdisposal.routes.SharesDisposalListController
              //                  .onPageLoad(srn, 1, NormalMode)
            )
            .withName("go from What You Will Need page to Shares Disposal List page")
        )
      }

      "SharesDisposalListPage" - {

        //TODO: dependent on SharesDisposalList implementation; use navigateFromListPage method

      }

      "HowWereSharesDisposedPage" - {

        act.like(
          normalmode
            .navigateToWithDoubleDataAndIndex(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Sold),
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.routes.UnauthorisedController.onPageLoad()
              //              controllers.nonsipp.sharesdisposal.routes.WhenWereSharesSoldController
              //                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from How Were Shares Disposed page to When Were Shares Sold page")
        )

        act.like(
          normalmode
            .navigateToWithDoubleDataAndIndex(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Redeemed),
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.routes.UnauthorisedController.onPageLoad()
              //              controllers.nonsipp.sharesdisposal.routes.WhenWereSharesRedeemedController
              //                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from How Were Shares Disposed page to When Were Shares Redeemed page")
        )

        act.like(
          normalmode
            .navigateToWithDoubleDataAndIndex(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Transferred),
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.routes.UnauthorisedController.onPageLoad()
              //              controllers.nonsipp.sharesdisposal.routes.HowManySharesHeldController
              //                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from How Were Shares Disposed page to How Many Shares Held page (Transferred)")
        )

        act.like(
          normalmode
            .navigateToWithDoubleDataAndIndex(
              shareIndex,
              disposalIndex,
              HowWereSharesDisposedPage.apply,
              Gen.const(HowSharesDisposed.Other("test details")),
              (srn, shareIndex: Max5000, disposalIndex: Max50, _) =>
                controllers.routes.UnauthorisedController.onPageLoad()
              //              controllers.nonsipp.sharesdisposal.routes.HowManySharesHeldController
              //                .onPageLoad(srn, shareIndex, disposalIndex, NormalMode)
            )
            .withName("go from How Were Shares Disposed page to How Many Shares Held page (Other)")
        )
      }
    }

    "in CheckMode" - {}
  }
}
