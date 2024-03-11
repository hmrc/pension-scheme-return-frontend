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
import models.{CheckMode, HowDisposed, NormalMode, PointOfEntry}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.bondsdisposal._
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

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
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.BondsStillHeldController
                .onSubmit(srn, bondIndex, disposalIndex, NormalMode)
          )
          .withName("go from how were bonds disposed page to BondsStillHeld (Transferred)")
      )

      act.like(
        normalmode
          .navigateToWithDoubleIndexAndData(
            bondIndex,
            disposalIndex,
            HowWereBondsDisposedOfPage.apply,
            Gen.const(HowDisposed.Other("test details")),
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.BondsStillHeldController
                .onSubmit(srn, bondIndex, disposalIndex, NormalMode)
          )
          .withName("go from how were bonds disposed page to BondsStillHeld (Other)")
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
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.BondsStillHeldController
                .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
          )
          .withName("go from buyer connected party page to BondsStillHeld")
      )
    }

    "BondsStillHeldPage" - {
      act.like(
        normalmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            BondsStillHeldPage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                .onPageLoad(srn, bondIndex, disposalIndex, NormalMode)
          )
          .withName("go from BondsStillHeld to BondsDisposalCYA")
      )
    }
  }

  "CheckMode" - {

    "HowWereBondsDisposedOfPage" - {

      act.like(
        checkmode
          .navigateToWithDoubleIndexAndData(
            bondIndex,
            disposalIndex,
            HowWereBondsDisposedOfPage.apply,
            Gen.const(HowDisposed.Sold),
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.WhenWereBondsSoldController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
          )
          .withName("go from how were bonds disposed page to when were bonds sold page")
      )


    }

    "WhenWereBondsSoldPage" - {

      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            WhenWereBondsSoldPage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.TotalConsiderationSaleBondsController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex),
                PointOfEntry.HowWereBondsDisposedPointOfEntry
              )
          )
          .withName("go from WhenWereBondsSoldPage to TotalConsiderationSaleBonds (HowWereSharesDisposedPOE)")
      )
    }

    "TotalConsiderationSaleBondsPage" - {

      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            TotalConsiderationSaleBondsPage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex),
                PointOfEntry.NoPointOfEntry
              )
          )
          .withName("go from TotalConsiderationSaleBondsPage to CYA")
      )

      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            TotalConsiderationSaleBondsPage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.BuyerNameController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex),
                PointOfEntry.HowWereBondsDisposedPointOfEntry
              )
          )
          .withName("go from TotalConsiderationSaleBondsPage to BuyerName (HowWereBondsDisposedPOE)")
      )
    }

    "BuyerNamePage" - {

      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            BuyerNamePage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex),
                PointOfEntry.NoPointOfEntry
              )
          )
          .withName("go from BuyerNamePage to CYA")
      )

      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            BuyerNamePage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.IsBuyerConnectedPartyController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                BondsDisposalCYAPointOfEntry(srn, bondIndex, disposalIndex),
                PointOfEntry.HowWereBondsDisposedPointOfEntry
              )
          )
          .withName("go from BuyerNamePage to IsBuyerConnectedParty (HowWereBondsDisposedPOE)")
      )
    }

    "IsBuyerConnectedPartyPage" - {

      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            IsBuyerConnectedPartyPage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
          )
          .withName("go from buyer connected party page to BondsDisposalCYA")
      )
    }

    "BondsStillHeldPage" - {
      act.like(
        checkmode
          .navigateToWithDoubleIndex(
            bondIndex,
            disposalIndex,
            BondsStillHeldPage,
            (srn, bondIndex: Max5000, disposalIndex: Max50, _) =>
              controllers.nonsipp.bondsdisposal.routes.BondsDisposalCYAController
                .onPageLoad(srn, bondIndex, disposalIndex, CheckMode)
          )
          .withName("go from BondsStillHeld to BondsDisposalCYA")
      )
    }

  }

}
