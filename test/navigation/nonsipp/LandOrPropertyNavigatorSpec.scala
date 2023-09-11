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
import models.SchemeHoldLandProperty.{Acquisition, Contribution, Transfer}
import models._
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.landorproperty._
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class LandOrPropertyNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator

  private val index = refineMV[OneTo5000](1)
  private val subject = IdentitySubject.LandOrPropertySeller

  "LandOrPropertyNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          LandOrPropertyHeldPage,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.landorproperty.routes.WhatYouWillNeedLandOrPropertyController.onPageLoad(srn)
        )
        .withName("go from land or property held page to what you will need Land or Property page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithData(
          LandOrPropertyHeldPage,
          Gen.const(false),
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad
        )
        .withName("go from land or property held page to money borrowed page when no selected")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          LandPropertyInUKPage,
          Gen.const(true),
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertyAddressLookupController.onPageLoad(srn, index)
        )
        .withName("go from land or property in uk page to land or property address lookup when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          LandPropertyInUKPage,
          Gen.const(false),
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertyAddressLookupController.onPageLoad(srn, index)
        )
        .withName("go from land or property in uk page to land or property address lookup when no selected")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          LandOrPropertyAddressLookupPage,
          Gen.const(address),
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.LandRegistryTitleNumberController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName("go from land or property address lookup page to land registry title page")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          LandRegistryTitleNumberPage,
          Gen.const(ConditionalYesNo.yes[String, String]("test")),
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.WhyDoesSchemeHoldLandPropertyController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName("go from land registry title page to why does scheme hold land or property page")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          LandOrPropertyTotalCostPage,
          Gen.const(money),
          controllers.nonsipp.landorproperty.routes.IsLandOrPropertyResidentialController.onPageLoad
        )
        .withName("go from land or property total cost page to is land or property residential page")
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          (srn, _: Max5000) => WhatYouWillNeedLandOrPropertyPage(srn),
          controllers.nonsipp.landorproperty.routes.LandPropertyInUKController.onPageLoad
        )
        .withName("go from what you will need Land or Property page to LandProperty In UK page")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          LandOrPropertyWhenDidSchemeAcquirePage,
          Gen.const(localDate),
          controllers.nonsipp.landorproperty.routes.LandPropertyIndependentValuationController.onPageLoad,
          srn =>
            defaultUserAnswers.unsafeSet(
              WhyDoesSchemeHoldLandPropertyPage(srn, index),
              SchemeHoldLandProperty.Contribution
            ) //Needed to mock the user input from 2 pages "ago"
        )
        .withName("go from land or property when did scheme acquire page to land property independent valuation page")
    )

    act.like(
      normalmode
        .navigateToWithDataIndexAndSubjects(
          index,
          subject,
          CompanySellerNamePage,
          Gen.const(""),
          controllers.nonsipp.common.routes.CompanyRecipientCrnController.onPageLoad
        )
        .withName("go from company land or property company seller  page to company crn page")
    )

    val recipientDetails = RecipientDetails(
      "testName",
      "testDescription"
    )

    List(OtherRecipientDetailsPage, CompanyRecipientCrnPage, PartnershipRecipientUtrPage).foreach { page =>
      act.like(
        normalmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            OtherRecipientDetailsPage,
            Gen.const(recipientDetails),
            controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController.onPageLoad
          )
          .withName(s"go from ${page} page to recipient connected party page")
      )
    }

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          IndividualSellerNiPage,
          controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController.onPageLoad
        )
        .withName("go from idividual seller NI page to recipient connected party page")
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          LandPropertyIndividualSellersNamePage,
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.IndividualSellerNiController.onPageLoad(srn, index, NormalMode)
        )
        .withName("go from land or property individual seller name page to ? page")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          LandOrPropertySellerConnectedPartyPage,
          Gen.const(true),
          controllers.nonsipp.landorproperty.routes.LandPropertyIndependentValuationController.onPageLoad
        )
        .withName(
          "go from land or property is seller a connected party page to is land or property independent valuation page"
        )
    )
  }

  "WhyDoesSchemeHoldLandPropertyNavigator" - {

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          (srn, _: Max5000) => WhyDoesSchemeHoldLandPropertyPage(srn, index),
          Gen.const(Acquisition),
          (srn, index: Max5000, mode) =>
            controllers.nonsipp.landorproperty.routes.WhenDidSchemeAcquireController.onPageLoad(srn, index, mode)
        )
        .withName("why does scheme hold land property page to WhenDidSchemeAcquireController page on Acquisition")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          (srn, _: Max5000) => WhyDoesSchemeHoldLandPropertyPage(srn, index),
          Gen.const(Contribution),
          (srn, index: Max5000, mode) =>
            controllers.nonsipp.landorproperty.routes.WhenDidSchemeAcquireController.onPageLoad(srn, index, mode)
        )
        .withName("why does scheme hold land property page to WhenDidSchemeAcquireController page on Contribution")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          (srn, _: Max5000) => WhyDoesSchemeHoldLandPropertyPage(srn, index),
          Gen.const(Transfer),
          (srn, index: Max5000, mode) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalCostController.onPageLoad(srn, index, mode)
        )
        .withName("why does scheme hold land property page to WhenDidSchemeAcquireController page on Transfer")
    )

  }

  "LandPropertyIndependentValuationNavigator" - {

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          LandPropertyIndependentValuationPage,
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalCostController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName(
          "go from land property independent valuation page to land Or property total cost page when yes selected"
        )
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          LandPropertyIndependentValuationPage,
          Gen.const(false),
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalCostController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName(
          "go from land property independent valuation page to land Or property total cost page when no selected"
        )
    )
  }

  "IsLandOrPropertyResidential" - {

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          IsLandOrPropertyResidentialPage,
          Gen.const(true),
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.IsLandPropertyLeasedController.onPageLoad(srn, index, NormalMode)
        )
        .withName("go from land or property in uk page to Is land property leased when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          IsLandOrPropertyResidentialPage,
          Gen.const(false),
          (srn, index: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from land or property in uk page to unauthorised when no selected")
    )
  }

  "IsLandPropertyLeased" - {

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          IsLandPropertyLeasedPage,
          Gen.const(true),
          (srn, index: Max5000, _) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertyLeaseDetailsController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName("go from is land property leased page to land or property lease details page when yes selected")
    )

    act.like(
      normalmode
        .navigateToWithDataAndIndex(
          index,
          IsLandPropertyLeasedPage,
          Gen.const(false),
          controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalIncomeController.onPageLoad
        )
        .withName("go from is land property leased page to land property total income when no selected")
    )
  }

  "IndividualSellerNi" - {

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          IndividualSellerNiPage,
          (srn, index: Max5000, mode) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName("go from individual sellerNi page to land Or property seller connected party page when yes selected")
    )
  }

  "LandOrPropertyLeaseDetailsPage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          LandOrPropertyLeaseDetailsPage,
          (srn, index: Max5000, mode) =>
            controllers.nonsipp.landorproperty.routes.IsLesseeConnectedPartyController
              .onPageLoad(srn, index, NormalMode)
        )
        .withName("go from land Or property lease details page to is lessee connected party page")
    )
  }

  "IsLesseeConnectedPartyPage" - {

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          IsLesseeConnectedPartyPage,
          controllers.nonsipp.landorproperty.routes.LandOrPropertyTotalIncomeController.onPageLoad
        )
        .withName("go from is lessee connected party page to unauthorised")
    )
  }

  "LandOrPropertyTotalIncomePage" - {
    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          LandOrPropertyTotalIncomePage,
          (srn, index: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad()
        )
        .withName("go from LandOrPropertyTotalIncome page to unauthorised page")
    )
  }

  "IdentityType navigation" - {
    "NormalMode" - {
      act.like(
        normalmode
          .navigateToWithDataIndexAndSubjectBoth(
            index,
            subject,
            IdentityTypePage,
            Gen.const(IdentityType.Other),
            controllers.nonsipp.common.routes.OtherRecipientDetailsController.onPageLoad
          )
          .withName("go from identity type page to other recipient details page")
      )

      act.like(
        normalmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            IdentityTypePage,
            Gen.const(IdentityType.Individual),
            controllers.nonsipp.landorproperty.routes.LandPropertyIndividualSellersNameController.onPageLoad
          )
          .withName("go from identity type page to individual recipient name page")
      )

      act.like(
        normalmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            IdentityTypePage,
            Gen.const(IdentityType.UKCompany),
            controllers.nonsipp.landorproperty.routes.CompanySellerNameController.onPageLoad
          )
          .withName("go from identity type page to company recipient name page")
      )

      act.like(
        normalmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            IdentityTypePage,
            Gen.const(IdentityType.UKPartnership),
            controllers.nonsipp.landorproperty.routes.PartnershipSellerNameController.onPageLoad
          )
          .withName("go fromidentity type page to UKPartnership to partnership recipient name page")
      )
    }
  }
  "Remove Land or Property" - {
    act.like(
      normalmode
        .navigateTo(
          srn => RemovePropertyPage(srn, refineMV(1)),
          (srn, _) =>
            controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController
              .onPageLoad(srn, NormalMode)
        )
        .withName("go from remove page to LandOrPropertyHeldPage page")
    )
  }
}
