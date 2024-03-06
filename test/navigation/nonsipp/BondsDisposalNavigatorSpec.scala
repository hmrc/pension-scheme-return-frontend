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
import models.{HowDisposed, NormalMode}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.bondsdisposal._
import utils.BaseSpec

class BondsDisposalNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val bondIndex = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  "BondsDisposalNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          BondsDisposalPage,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.bondsdisposal.routes.WhatYouWillNeedBondsDisposalController.onPageLoad(srn)
        )
        .withName("go from bonds disposal page to what you will need bonds disposal page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          BondsDisposalPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from bonds disposal page to taskList when no selected")
    )

    "WhatYouWillNeedBondsDisposalPage" - {

      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedBondsDisposalPage,
            controllers.nonsipp.bondsdisposal.routes.BondsDisposalListController.onPageLoad(_, 1, _)
          )
          .withName(
            "go from what you will need bonds disposal page to bonds disposal list page"
          )
      )
    }

    "BondsDisposalListPage" - {

      act.like(
        normalmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            BondsDisposalListPage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.HowWereBondsDisposedOfController
                .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
          )
          .withName("go from bonds disposal list page to how were bonds disposed page")
      )
    }

    "HowWereBondsDisposedOfPage" - {

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            bondIndex,
            disposalIndex,
            HowWereBondsDisposedOfPage.apply,
            Gen.const(HowDisposed.Sold),
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.WhenWereBondsSoldController
                .onSubmit(srn, bondIndex, disposalIndex, NormalMode)
          )
          .withName("go from how were bonds disposed page to when were bonds sold page")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            bondIndex,
            disposalIndex,
            HowWereBondsDisposedOfPage.apply,
            Gen.const(HowDisposed.Transferred),
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from how were bonds disposed page to unauthorised page(Transferred)")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            bondIndex,
            disposalIndex,
            HowWereBondsDisposedOfPage.apply,
            Gen.const(HowDisposed.Other("test details")),
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from how were bonds disposed page to unauthorised page(Other)")
      )
    }

    "WhenWereBondsSoldPage" - {

      act.like(
        normalmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            WhenWereBondsSoldPage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.TotalConsiderationSaleBondsController
                .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
          )
          .withName("go from when were bonds sold page to total consideration sale bonds page")
      )

    }

    "TotalConsiderationSaleBondsPage" - {

      act.like(
        normalmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            TotalConsiderationSaleBondsPage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.BuyerNameController
                .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
          )
          .withName("go from total consideration sale bonds page to buyer name page")
      )

    }

    "BuyerNamePage" - {
      act.like(
        normalmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            BuyerNamePage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.IsBuyerConnectedPartyController
                .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
          )
          .withName("go from buyer name page to is buyer connected party page")
      )
    }

    "IsBuyerConnectedPartyPage" - {
      act.like(
        normalmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            IsBuyerConnectedPartyPage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName("go from buyer connected party page to unauthorised page")
      )
    }
  }

}
