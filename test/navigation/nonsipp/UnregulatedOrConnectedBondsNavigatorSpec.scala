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
import models.{NormalMode, SchemeHoldBond}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.unregulatedorconnectedbonds._
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class UnregulatedOrConnectedBondsNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index = refineMV[OneTo5000](1)

  "UnregulatedOrConnectedBondsNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          UnregulatedOrConnectedBondsHeldPage,
          Gen.const(true),
          (srn, _) =>
            controllers.nonsipp.unregulatedorconnectedbonds.routes.WhatYouWillNeedBondsController.onPageLoad(srn)
        )
        .withName("go from unregulated Or connected bonds held page to WhatYouWillNeedBonds when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          UnregulatedOrConnectedBondsHeldPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from unregulated Or connected bonds held page to other assets held page when no selected")
    )

    "WhatYouWillNeedBondsPage" - {
      act.like(
        normalmode
          .navigateTo(
            WhatYouWillNeedBondsPage,
            (srn, _) =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.NameOfBondsController
                .onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from WhatYouWillNeedBondsPage to NameOfBondsPage"
          )
      )
    }

    "NameOfBondsPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            NameOfBondsPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.WhyDoesSchemeHoldBondsController
                .onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from NameOfBondsPage to WhyDoesSchemeHoldBonds page"
          )
      )
    }

    "WhyDoesSchemeHoldBondsPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            WhyDoesSchemeHoldBondsPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.WhenDidSchemeAcquireBondsController
                .onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                WhyDoesSchemeHoldBondsPage(srn, index),
                SchemeHoldBond.Acquisition
              )
          )
          .withName(
            "go from WhyDoesSchemeHoldBondsPage to WhenDidSchemeAcquireBondsPage when holding is acquisition"
          )
      )

      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            WhyDoesSchemeHoldBondsPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.WhenDidSchemeAcquireBondsController
                .onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                WhyDoesSchemeHoldBondsPage(srn, index),
                SchemeHoldBond.Contribution
              )
          )
          .withName(
            "go from WhyDoesSchemeHoldBondsPage to WhenDidSchemeAcquireBondsPage when holding is contribution"
          )
      )

      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            WhyDoesSchemeHoldBondsPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.CostOfBondsController
                .onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                WhyDoesSchemeHoldBondsPage(srn, index),
                SchemeHoldBond.Transfer
              )
          )
          .withName(
            "go from WhyDoesSchemeHoldBondsPage to CostOfBondsPage when holding is transfer"
          )
      )
    }

    "WhenDidSchemeAcquireBondsPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            WhenDidSchemeAcquireBondsPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.unregulatedorconnectedbonds.routes.CostOfBondsController
              .onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from WhenDidSchemeAcquireBondsPage to CostOfBondsPage"
          )
      )
    }

    "CostOfBondsPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            CostOfBondsPage,
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
          )
          .withName(
            "go from CostOfBondsPage to Unauthorised"
          )
      )
    }
  }
}
