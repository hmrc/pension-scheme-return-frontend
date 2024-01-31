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
          (srn, index: Max5000, disposalIndex: Max50, mode) =>
            controllers.nonsipp.landorpropertydisposal.routes.LandPropertyDisposalCYAController
              .onPageLoad(srn, index, disposalIndex, mode)
        )
        .withName("go from LandOrPropertyStillHeldPage to land property disposal CYA page")
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
          controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalBuyerConnectedPartyController.onPageLoad
        )
        .withName("go from individual buyer nino page to land or property disposal buyer connected party page")
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
          controllers.nonsipp.landorpropertydisposal.routes.PartnershipBuyerUtrController.onPageLoad
        )
        .withName("go from partnership buyer name page to partnership buyer UTR page")
    )
  }

  "PartnershipBuyerUtrPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          PartnershipBuyerUtrPage,
          controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalBuyerConnectedPartyController.onPageLoad
        )
        .withName("go from partnership buyer UTR page to land or property disposal buyer connected party page")
    )
  }

  "OtherBuyerDetailsPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          OtherBuyerDetailsPage,
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalBuyerConnectedPartyController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from other buyer details page to land or property disposal buyer connected party page")
    )
  }

  "WhoPurchasedLandOrPropertyPage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndexAndData(
          index,
          disposalIndex,
          WhoPurchasedLandOrPropertyPage,
          Gen.const(IdentityType.Individual),
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyIndividualBuyerNameController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from who purchased land or property page to land or property individual buyer name page")
    )

    act.like(
      normalmode
        .navigateToWithDoubleIndexAndData(
          index,
          disposalIndex,
          WhoPurchasedLandOrPropertyPage,
          Gen.const(IdentityType.UKCompany),
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.CompanyBuyerNameController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from who purchased land or property page to company buyer name page")
    )

    act.like(
      normalmode
        .navigateToWithDoubleIndexAndData(
          index,
          disposalIndex,
          WhoPurchasedLandOrPropertyPage,
          Gen.const(IdentityType.UKPartnership),
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.PartnershipBuyerNameController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from who purchased land or property page to partnership buyer name page")
    )

    act.like(
      normalmode
        .navigateToWithDoubleIndexAndData(
          index,
          disposalIndex,
          WhoPurchasedLandOrPropertyPage,
          Gen.const(IdentityType.Other),
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.OtherBuyerDetailsController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from who purchased land or property page to unauthorised page")
    )
  }

  "LandOrPropertyDisposalBuyerConnectedPartyPage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          LandOrPropertyDisposalBuyerConnectedPartyPage,
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.TotalProceedsSaleLandPropertyController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName(
          "go from land or property disposal buyer connected party Page to total proceeds sale land property"
        )
    )
  }

  "TotalProceedsSaleLandPropertyPage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          TotalProceedsSaleLandPropertyPage,
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.DisposalIndependentValuationController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from total proceeds sale land property page to independent valuation page")
    )
  }

  "DisposalIndependentValuationPage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          DisposalIndependentValuationPage,
          (srn, index: Max5000, disposalIndex: Max50, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyStillHeldController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from total proceeds sale land property page to land or property still held page")
    )
  }
}
