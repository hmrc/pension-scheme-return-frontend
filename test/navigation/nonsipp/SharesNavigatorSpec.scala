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
import controllers.nonsipp.shares.CompanyNameOfSharesSellerPage
import eu.timepit.refined.refineMV
import models.{IdentitySubject, IdentityType, NormalMode, RecipientDetails, SchemeHoldShare, TypeOfShares}
import navigation.{Navigator, NavigatorBehaviours}
import org.scalacheck.Gen
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.shares._
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

class SharesNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index = refineMV[OneTo5000](1)
  private val subject = IdentitySubject.SharesSeller

  "SharesNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeHoldAnySharesPage,
          Gen.const(false),
          (srn, _) => controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
        )
        .withName(
          "go from scheme hold any shares to task list page when no is selected"
        )
    )

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeHoldAnySharesPage,
          Gen.const(true),
          (srn, _) => controllers.nonsipp.shares.routes.WhatYouWillNeedSharesController.onPageLoad(srn)
        )
        .withName(
          "go from scheme hold any shares to what you will need page when yes is selected"
        )
    )

    act.like(
      normalmode
        .navigateTo(
          WhatYouWillNeedSharesPage,
          (srn, _) => controllers.nonsipp.shares.routes.TypeOfSharesHeldController.onPageLoad(srn, index, NormalMode)
        )
        .withName(
          "go from WhatYouWillNeedSharesPage to type of shares page"
        )
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          TypeOfSharesHeldPage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.shares.routes.WhyDoesSchemeHoldSharesController.onPageLoad(srn, index, NormalMode)
        )
        .withName(
          "go from TypeOfSharesHeldPage to WhyDoesSchemeHoldShares page"
        )
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          WhyDoesSchemeHoldSharesPage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, NormalMode),
          srn =>
            defaultUserAnswers.unsafeSet(
              WhyDoesSchemeHoldSharesPage(srn, index),
              SchemeHoldShare.Acquisition
            )
        )
        .withName(
          "go from WhyDoesSchemeHoldSharesPage to WhenDidSchemeAcquireShares page when holding is acquisition"
        )
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          WhyDoesSchemeHoldSharesPage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad(srn, index, NormalMode),
          srn =>
            defaultUserAnswers.unsafeSet(
              WhyDoesSchemeHoldSharesPage(srn, index),
              SchemeHoldShare.Contribution
            )
        )
        .withName(
          "go from WhyDoesSchemeHoldSharesPage to WhenDidSchemeAcquireShares page when holding is contribution"
        )
    )

    act.like(
      normalmode
        .navigateToWithIndex(
          index,
          WhyDoesSchemeHoldSharesPage,
          (srn, _: Max5000, _) =>
            controllers.nonsipp.shares.routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, NormalMode),
          srn =>
            defaultUserAnswers.unsafeSet(
              WhyDoesSchemeHoldSharesPage(srn, index),
              SchemeHoldShare.Transfer
            )
        )
        .withName(
          "go from WhyDoesSchemeHoldSharesPage to CompanyNameRelatedSharesPage when holding is transfer"
        )
    )
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
            .withName("go from identity type page to other seller details page")
        )

        act.like(
          normalmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.Individual),
              controllers.nonsipp.shares.routes.IndividualNameOfSharesSellerController.onPageLoad
            )
            .withName("go from identity type page to individual seller name page")
        )

        act.like(
          normalmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.UKCompany),
              controllers.nonsipp.shares.routes.CompanyNameOfSharesSellerController.onPageLoad
            )
            .withName("go from identity type page to company seller name page")
        )

        act.like(
          normalmode
            .navigateToWithDataIndexAndSubject(
              index,
              subject,
              IdentityTypePage,
              Gen.const(IdentityType.UKPartnership),
              controllers.nonsipp.shares.routes.PartnershipNameOfSharesSellerController.onPageLoad
            )
            .withName("go from identity type page to UKPartnership to partnership name of shares seller name page")
        )
      }
    }

    "WhenDidSchemeAcquireSharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            WhenDidSchemeAcquireSharesPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.CompanyNameRelatedSharesController.onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from WhenDidSchemeAcquireShares to CompanyNameRelatedShares page"
          )
      )
    }

    "CompanyNameRelatedSharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            CompanyNameRelatedSharesPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesCompanyCrnController.onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from company name related shares page to shares company Crn page"
          )
      )
    }

    "SharesCompanyCrnPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesCompanyCrnPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.ClassOfSharesController.onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from shares company Crn page to class of shares page"
          )
      )
    }

    "ClassOfSharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            ClassOfSharesPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.HowManySharesController.onPageLoad(srn, index, NormalMode)
          )
          .withName("go from class of shares to identity subject shares seller page")
      )
    }

    "IndividualNameOfSharesSellerPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            IndividualNameOfSharesSellerPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesIndividualSellerNINumberController
                .onPageLoad(srn, index, NormalMode)
          )
          .withName("go from individual name of shares seller page to shares individual seller NI number page")
      )
    }

    "SharesIndividualSellerNINumberPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesIndividualSellerNINumberPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                .onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.Unquoted
              )
          )
          .withName(
            "go from SharesIndividualSellerNINumberPage to SharesFromConnectedParty page when Unquoted is selected"
          )
      )
    }

    "SharesIndividualSellerNINumberPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesIndividualSellerNINumberPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
          )
          .withName("go from SharesIndividualSellerNINumber page to CostOfShares page")
      )
    }

    "CompanyNameOfSharesSellerPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            CompanyNameOfSharesSellerPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.common.routes.CompanyRecipientCrnController
                .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller)
          )
          .withName("go from  company name of shares seller page to company recipient Crn page")
      )
    }

    "CostOfSharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            CostOfSharesPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesIndependentValuationController
                .onPageLoad(srn, index, NormalMode)
          )
          .withName("go from cost of shares page to SharesIndependentValuation page")
      )
    }

    "PartnershipShareSellerNamePage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            PartnershipShareSellerNamePage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.common.routes.PartnershipRecipientUtrController
                .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller)
          )
          .withName("go from  company name of shares seller page to partnership recipient Utr page")
      )
    }

    "PartnershipRecipientUtrPage" - {
      act.like(
        normalmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            PartnershipRecipientUtrPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                .onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.Unquoted
              )
          )
          .withName(
            "go from PartnershipRecipientUtrPage to SharesFromConnectedParty page when Unquoted is selected"
          )
      )
    }

    "PartnershipRecipientUtrPage" - {
      act.like(
        normalmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            PartnershipRecipientUtrPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
          )
          .withName("go from PartnershipRecipientUtrPage page to CostOfShares page")
      )
    }

    "CompanyRecipientCrnPage" - {
      act.like(
        normalmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            CompanyRecipientCrnPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                .onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.Unquoted
              )
          )
          .withName(
            "go from CompanyRecipientCrnPage to SharesFromConnectedParty page when Unquoted is selected"
          )
      )
    }

    "CompanyRecipientCrnPage" - {
      act.like(
        normalmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            CompanyRecipientCrnPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
          )
          .withName("go from CompanyRecipientCrnPage page to CostOfShares page")
      )
    }

    "OtherRecipientDetailsPage" - {

      val shareDetails = RecipientDetails(
        "testName",
        "testDescription"
      )

      act.like(
        normalmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            OtherRecipientDetailsPage,
            Gen.const(shareDetails),
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode)
          )
          .withName("go from other recipient details page to CostOfShares page")
      )
    }

    "HowManySharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            HowManySharesPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, index, NormalMode, IdentitySubject.SharesSeller),
            srn =>
              defaultUserAnswers.unsafeSet(
                WhyDoesSchemeHoldSharesPage(srn, index),
                SchemeHoldShare.Acquisition
              )
          )
          .withName(
            "go from HowManySharesPage to IdentityType page when holding is acquisition"
          )
      )
    }

    "HowManySharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            HowManySharesPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                .onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.Unquoted
              )
          )
          .withName(
            "go from HowManySharesPage to shares from connected party when unquoted shares selected "
          )
      )
    }

    "HowManySharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            HowManySharesPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.SponsoringEmployer
              )
          )
          .withName(
            "go from how many shares page to CostOfShares page when SponsoringEmployer is selected"
          )
      )
    }

    "HowManySharesPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            HowManySharesPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.CostOfSharesController.onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.ConnectedParty
              )
          )
          .withName(
            "go from how many shares page to CostOfShares page when ConnectedParty is selected"
          )
      )
    }

    "SharesFromConnectedPartyPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesFromConnectedPartyPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.CostOfSharesController
                .onPageLoad(srn, index, NormalMode)
          )
          .withName("go from  shares from connected party page to cost of shares page")
      )
    }

    "SharesIndependentValuationPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesIndependentValuationPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesTotalIncomeController.onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  WhyDoesSchemeHoldSharesPage(srn, index),
                  SchemeHoldShare.Acquisition
                )
                .unsafeSet(
                  TypeOfSharesHeldPage(srn, index),
                  TypeOfShares.Unquoted
                )
          )
          .withName(
            "go from shares independent valuation page to SharesTotalIncome page when holding is acquisition"
          )
      )
    }

    "SharesIndependentValuationPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesIndependentValuationPage,
            (srn, _: Max5000, _) => controllers.routes.UnauthorisedController.onPageLoad(),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  WhyDoesSchemeHoldSharesPage(srn, index),
                  SchemeHoldShare.Acquisition
                )
                .unsafeSet(
                  TypeOfSharesHeldPage(srn, index),
                  TypeOfShares.SponsoringEmployer
                )
          )
          .withName(
            "go from shares independent valuation page to unauthorised page when holding is acquisition"
          )
      )
    }

    "SharesIndependentValuationPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesIndependentValuationPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesTotalIncomeController
                .onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  TypeOfSharesHeldPage(srn, index),
                  TypeOfShares.Unquoted
                )
                .unsafeSet(
                  WhyDoesSchemeHoldSharesPage(srn, index),
                  SchemeHoldShare.Contribution
                )
          )
          .withName(
            "go from shares independent valuation page to shares total income when unquoted shares selected "
          )
      )
    }

    "SharesIndependentValuationPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesIndependentValuationPage,
            (srn, _: Max5000, _) =>
              controllers.nonsipp.shares.routes.SharesTotalIncomeController
                .onPageLoad(srn, index, NormalMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  TypeOfSharesHeldPage(srn, index),
                  TypeOfShares.ConnectedParty
                )
                .unsafeSet(
                  WhyDoesSchemeHoldSharesPage(srn, index),
                  SchemeHoldShare.Transfer
                )
          )
          .withName(
            "go from how many shares page to shares total income page  page when ConnectedParty is selected"
          )
      )
    }

  }
}
