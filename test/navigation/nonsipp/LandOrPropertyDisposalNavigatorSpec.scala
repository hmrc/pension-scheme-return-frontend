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
import pages.nonsipp.landorpropertydisposal._
import navigation.{Navigator, NavigatorBehaviours}
import models.{IdentityType, NormalMode}
import viewmodels.models.SectionCompleted
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen

class LandOrPropertyDisposalNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index: Max5000 = 1
  private val disposalIndex: Max50 = 1

  "LandOrPropertyDisposalPage" - {
    act.like(
      normalmode
        .navigateToWithData(
          LandOrPropertyDisposalPage.apply,
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
          LandOrPropertyDisposalPage.apply,
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
          LandOrPropertyStillHeldPage.apply,
          (srn, index: Int, disposalIndex: Int, mode) =>
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
          WhenWasPropertySoldPage.apply,
          (srn, index: Int, disposalIndex: Int, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.TotalProceedsSaleLandPropertyController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from when was property sold page to total proceeds sale page")
    )
  }

  "TotalProceedsSaleLandPropertyPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          TotalProceedsSaleLandPropertyPage.apply,
          (srn, index: Int, disposalIndex: Int, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.WhoPurchasedLandOrPropertyController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from total proceeds sale page to who purchased page")
    )
  }

  "IndividualBuyerNinoNumberPage" - {
    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          IndividualBuyerNinoNumberPage.apply,
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
          LandOrPropertyIndividualBuyerNamePage.apply,
          (srn, index: Int, disposalIndex: Int, _) =>
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
          PartnershipBuyerNamePage.apply,
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
          PartnershipBuyerUtrPage.apply,
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
          OtherBuyerDetailsPage.apply,
          (srn, index: Int, disposalIndex: Int, _) =>
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
          (srn, index: Int, disposalIndex: Int, _) =>
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
          WhoPurchasedLandOrPropertyPage.apply,
          Gen.const(IdentityType.UKCompany),
          (srn, index: Int, disposalIndex: Int, _) =>
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
          WhoPurchasedLandOrPropertyPage.apply,
          Gen.const(IdentityType.UKPartnership),
          (srn, index: Int, disposalIndex: Int, _) =>
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
          WhoPurchasedLandOrPropertyPage.apply,
          Gen.const(IdentityType.Other),
          (srn, index: Int, disposalIndex: Int, _) =>
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
          LandOrPropertyDisposalBuyerConnectedPartyPage.apply,
          (srn, index: Int, disposalIndex: Int, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.DisposalIndependentValuationController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName(
          "go from land or property disposal buyer connected party Page to independent valuation page"
        )
    )
  }

  "DisposalIndependentValuationPage" - {

    act.like(
      normalmode
        .navigateToWithDoubleIndex(
          index,
          disposalIndex,
          DisposalIndependentValuationPage.apply,
          (srn, index: Int, disposalIndex: Int, _) =>
            controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyStillHeldController
              .onPageLoad(srn, index, disposalIndex, NormalMode)
        )
        .withName("go from total proceeds sale land property page to land or property still held page")
    )
  }

  "Add Land or property disposal" - {
    "Record at index 1" - {
      act.like(
        normalmode
          .navigateTo(
            srn => LandOrPropertyDisposalAddressListPage(srn, 1),
            (srn, mode) =>
              controllers.nonsipp.landorpropertydisposal.routes.HowWasPropertyDisposedOfController
                .onPageLoad(srn, 1, 2, mode),
            srn =>
              defaultUserAnswers
                .unsafeSet(LandPropertyDisposalCompletedPage(srn, 1, 1), SectionCompleted)
          )
          .withName("Add Land or property disposal with a record at index 1")
      )
    }

    "Record at index 2" - {
      act.like(
        normalmode
          .navigateTo(
            srn => LandOrPropertyDisposalAddressListPage(srn, 1),
            (srn, mode) =>
              controllers.nonsipp.landorpropertydisposal.routes.HowWasPropertyDisposedOfController
                .onPageLoad(srn, 1, 1, mode),
            srn =>
              defaultUserAnswers
                .unsafeSet(LandPropertyDisposalCompletedPage(srn, 1, 2), SectionCompleted)
          )
          .withName("Add Land or property disposal with a record at index 2")
      )
    }

    "No records" - {
      act.like(
        normalmode
          .navigateTo(
            srn => LandOrPropertyDisposalAddressListPage(srn, 1),
            (srn, mode) =>
              controllers.nonsipp.landorpropertydisposal.routes.HowWasPropertyDisposedOfController
                .onPageLoad(srn, 1, 1, mode)
          )
          .withName("Add Land or property disposal with no records")
      )
    }
  }
}
