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
import models.{IdentityType, NormalMode}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.landorpropertydisposal._
import utils.BaseSpec

class LandOrPropertyDisposalNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[Max5000.Refined](1)
  private val disposalIndex = refineMV[Max50.Refined](1)

  "LandOrPropertyDisposalPage" - {
    act.like(
      normalmode
        .navigateToWithData(
          LandOrPropertyDisposalPage,
          Gen.const(true),
          (srn, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.WhatYouWillNeedLandPropertyDisposalController
              .onPageLoad(srn)
        )
        .withName("go from LandOrPropertyDisposalPage to ??? page on yes")
    )

    act.like(
      normalmode
        .navigateToWithData(
          LandOrPropertyDisposalPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName("go from LandOrPropertyDisposalPage to task list page on no")
    )
  }

  "LandOrPropertyStillHeldPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          LandOrPropertyStillHeldPage,
          (srn, index: Max5000, disposalIndex: Max50, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from LandOrPropertyStillHeldPage to ??? page")
    )
  }

  "WhenWasPropertySoldPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          WhenWasPropertySoldPage,
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.WhoPurchasedLandOrPropertyController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from when was property sold page to who purchased land or property page")
    )
  }

  "IndividualBuyerNinoNumberPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          IndividualBuyerNinoNumberPage,
          (srn, index: Max5000, disposalIndex: Max50, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from individual buyer nino page to unauthorised page")
    )
  }

  "LandOrPropertyIndividualBuyerNamePage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          LandOrPropertyIndividualBuyerNamePage,
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.IndividualBuyerNinoNumberController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from land or property individual buyer name page to individual buyer nino number page")
    )
  }

  "PartnershipBuyerNamePage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          PartnershipBuyerNamePage,
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.PartnershipBuyerUtrController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from partnership buyer UTR page")
    )
  }
}
