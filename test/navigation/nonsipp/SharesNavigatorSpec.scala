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
import config.RefinedTypes.Max5000
import models.SchemeId.Srn
import utils.IntUtils.given
import models._
import pages.nonsipp.common._
import models.IdentitySubject.SharesSeller
import pages.nonsipp.shares._
import play.api.mvc.Call
import utils.UserAnswersUtils.UserAnswersOps
import org.scalacheck.Gen
import navigation.{Navigator, NavigatorBehaviours}
import uk.gov.hmrc.domain.Nino

class SharesNavigatorSpec extends BaseSpec with NavigatorBehaviours {

  val navigator: Navigator = new NonSippNavigator
  private val index: Max5000 = 1
  private val subject = IdentitySubject.SharesSeller

  "SharesNavigator" - {

    act.like(
      normalmode
        .navigateToWithData(
          DidSchemeHoldAnySharesPage.apply,
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
          DidSchemeHoldAnySharesPage.apply,
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
          WhatYouWillNeedSharesPage.apply,
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
          TypeOfSharesHeldPage.apply,
          (srn, _: Int, _) =>
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
          WhyDoesSchemeHoldSharesPage.apply,
          (srn, _: Int, _) =>
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
          WhyDoesSchemeHoldSharesPage.apply,
          (srn, _: Int, _) =>
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
          WhyDoesSchemeHoldSharesPage.apply,
          (srn, _: Int, _) =>
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
              IdentityTypePage.apply,
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
              IdentityTypePage.apply,
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
              IdentityTypePage.apply,
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
              IdentityTypePage.apply,
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
            WhenDidSchemeAcquireSharesPage.apply,
            (srn, _: Int, _) =>
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
            CompanyNameRelatedSharesPage.apply,
            (srn, _: Int, _) =>
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
            SharesCompanyCrnPage.apply,
            (srn, _: Int, _) =>
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
            ClassOfSharesPage.apply,
            (srn, _: Int, _) =>
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
            IndividualNameOfSharesSellerPage.apply,
            (srn, _: Int, _) =>
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
            SharesIndividualSellerNINumberPage.apply,
            (srn, _: Int, _) =>
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
            SharesIndividualSellerNINumberPage.apply,
            (srn, _: Int, _) =>
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
            CompanyNameOfSharesSellerPage.apply,
            (srn, _: Int, _) =>
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
            CostOfSharesPage.apply,
            (srn, _: Int, _) =>
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
            PartnershipShareSellerNamePage.apply,
            (srn, _: Int, _) =>
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
            PartnershipRecipientUtrPage.apply,
            (srn, _: Int, _) =>
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
            PartnershipRecipientUtrPage.apply,
            (srn, _: Int, _) =>
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
            CompanyRecipientCrnPage.apply,
            (srn, _: Int, _) =>
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
            CompanyRecipientCrnPage.apply,
            (srn, _: Int, _) =>
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
            OtherRecipientDetailsPage.apply,
            Gen.const(shareDetails),
            (srn, _: Int, _) =>
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
            HowManySharesPage.apply,
            (srn, _: Int, _) =>
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
            HowManySharesPage.apply,
            (srn, _: Int, _) =>
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
            HowManySharesPage.apply,
            (srn, _: Int, _) =>
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
            HowManySharesPage.apply,
            (srn, _: Int, _) =>
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
            SharesFromConnectedPartyPage.apply,
            (srn, _: Int, _) =>
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
            SharesIndependentValuationPage.apply,
            (srn, _: Int, _) =>
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
            SharesIndependentValuationPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, NormalMode),
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
            "go from shares independent valuation page to total asset value page when holding is acquisition"
          )
      )
    }

    "SharesIndependentValuationPage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            SharesIndependentValuationPage.apply,
            (srn, _: Int, _) =>
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
            SharesIndependentValuationPage.apply,
            (srn, _: Int, _) =>
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

    "TotalAssetValuePage" - {
      act.like(
        normalmode
          .navigateToWithIndex(
            index,
            TotalAssetValuePage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesTotalIncomeController
                .onPageLoad(srn, index, NormalMode)
          )
          .withName(
            "go from TotalAssetValuePage to SharesTotalIncomePage"
          )
      )
    }

    "SharesCYAPage" - {
      act.like(
        normalmode
          .navigateTo(
            srn => SharesCYAPage(srn),
            (srn, _) => controllers.nonsipp.shares.routes.SharesListController.onPageLoad(srn, 1, NormalMode)
          )
          .withName("go from shares CYA page to shares list page")
      )
    }

    "RemoveSharesPage" - {
      act.like(
        normalmode
          .navigateTo(
            srn => RemoveSharesPage(srn, index),
            controllers.nonsipp.shares.routes.DidSchemeHoldAnySharesController.onPageLoad
          )
          .withName("go from remove shares page to did scheme hold any shares page")
      )
    }

  }

  "CheckMode" - {

    "TypeOfSharesHeldPage" - {

      def test(
        testName: String,
        nextPage: (Srn, Int, Mode) => Call,
        oldUserAnswers: Srn => UserAnswers => UserAnswers,
        newUserAnswers: Srn => UserAnswers => UserAnswers
      ): Unit =
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              TypeOfSharesHeldPage.apply,
              nextPage,
              oldUserAnswers(_)(defaultUserAnswers),
              newUserAnswers(_)(defaultUserAnswers)
            )
            .withName(testName)
        )

      test(
        "go to CYA when unchanged answer is Unquoted and SharesFromConnectedPartyPage is complete",
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.Unquoted),
        newUserAnswers = srn =>
          _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.Unquoted)
            .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)
      )

      test(
        "go to SharesFromConnectedParty when unchanged answer is Unquoted and SharesFromConnectedPartyPage is incomplete",
        controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.Unquoted),
        newUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.Unquoted)
      )

      test(
        "go to CYA when unchanged answer is SponsoringEmployer and TotalAssetValuePage is complete",
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer),
        newUserAnswers = srn =>
          _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer)
            .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition)
            .unsafeSet(TotalAssetValuePage(srn, index), money)
      )

      test(
        "go to TotalAssetValue when unchanged answer is SponsoringEmployer and TotalAssetValuePage is incomplete",
        controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer),
        newUserAnswers = srn =>
          _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer)
            .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition)
      )

      test(
        "go to CYA when unchanged answer is ConnectedParty",
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty),
        newUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty)
      )

      test(
        "go to CYA when answer is changed to SponsoringEmployer",
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty),
        newUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer)
      )

      test(
        "go to TotalAssetValue when answer is changed to SponsoringEmployer and WhyDoesSchemeHoldShares is Acquisition",
        controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty),
        newUserAnswers = srn =>
          _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer)
            .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition)
      )

      test(
        "go to SharesFromConnectedPartyPage when answer is changed to Unquoted",
        controllers.nonsipp.shares.routes.SharesFromConnectedPartyController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty),
        newUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.Unquoted)
      )

      test(
        "go to CYA when answer is changed to ConnectedParty",
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer),
        newUserAnswers = srn => _.unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty)
      )
    }

    "WhyDoesSchemeHoldSharesPage" - {

      def test(
        testName: String,
        nextPage: (Srn, Int, Mode) => Call,
        oldUserAnswers: Srn => UserAnswers => UserAnswers,
        newUserAnswers: Srn => UserAnswers => UserAnswers
      ): Unit =
        act.like(
          checkmode
            .navigateToWithIndex(
              index,
              WhyDoesSchemeHoldSharesPage.apply,
              nextPage,
              oldUserAnswers(_)(defaultUserAnswers),
              newUserAnswers(_)(defaultUserAnswers)
            )
            .withName(testName)
        )

      test(
        "go to CYA when unchanged answer is Contribution and WhenDidSchemeAcquireSharesPage is complete",
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Contribution),
        newUserAnswers = srn =>
          _.unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Contribution)
            .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)
      )

      test(
        "go to WhenDidSchemeAcquireShares when unchanged answer is Contribution and WhenDidSchemeAcquireSharesPage is incomplete",
        controllers.nonsipp.shares.routes.WhenDidSchemeAcquireSharesController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Contribution),
        newUserAnswers = srn => _.unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Contribution)
      )

      test(
        "go to CYA when unchanged answer is Acquisition and WhenDidSchemeAcquireSharesPage, at least one share seller" +
          "and TotalAssetValuePage are all complete when TypeOfSharesHeld is SponsoringEmployer",
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition),
        newUserAnswers = srn =>
          _.unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition)
            .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)
            .unsafeSet(CompanyNameOfSharesSellerPage(srn, index), companyName)
            .unsafeSet(
              CompanyRecipientCrnPage(srn, index, IdentitySubject.SharesSeller),
              ConditionalYesNo.yes[String, Crn](crn)
            )
            .unsafeSet(TotalAssetValuePage(srn, index), money)
      )

      test(
        "go to CYA when unchanged answer is Transfer",
        controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad,
        oldUserAnswers = srn => _.unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Transfer),
        newUserAnswers = srn => _.unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Transfer)
      )
    }

    "CompanyNameRelatedSharesPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            CompanyNameRelatedSharesPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
          )
          .withName(
            "go from company name related shares page to sharesCYA page"
          )
      )
    }

    "SharesCompanyCrnPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            SharesCompanyCrnPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
          )
          .withName(
            "go from shares company Crn page to class of sharesCYA page"
          )
      )
    }

    "ClassOfSharesPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            ClassOfSharesPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
          )
          .withName("go from class of shares to identity subject sharesCYA page")
      )
    }

    "HowManySharesPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            HowManySharesPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                WhyDoesSchemeHoldSharesPage(srn, index),
                SchemeHoldShare.Acquisition
              )
          )
          .withName(
            "go from HowManySharesPage to SharesCYA page when holding is acquisition"
          )
      )
    }

    "HowManySharesPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            HowManySharesPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.Unquoted
              )
          )
          .withName(
            "go from HowManySharesPage to sharesCYA page when unquoted shares selected "
          )
      )
    }

    "HowManySharesPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            HowManySharesPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.SponsoringEmployer
              )
          )
          .withName(
            "go from how many shares page to sharesCYA page when SponsoringEmployer is selected"
          )
      )
    }

    "HowManySharesPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            HowManySharesPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.ConnectedParty
              )
          )
          .withName(
            "go from how many shares page to sharesCYA page when ConnectedParty is selected"
          )
      )
    }

    "IdentityType navigation" - {
      act.like(
        checkmode
          .navigateToWithDataIndexAndSubjectBoth(
            index,
            subject,
            IdentityTypePage.apply,
            Gen.const(IdentityType.Other),
            controllers.nonsipp.common.routes.OtherRecipientDetailsController.onPageLoad
          )
          .withName("go from identity type page to other seller details page")
      )

      act.like(
        checkmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            IdentityTypePage.apply,
            Gen.const(IdentityType.Individual),
            controllers.nonsipp.shares.routes.IndividualNameOfSharesSellerController.onPageLoad
          )
          .withName("go from identity type page to individual seller name page")
      )

      act.like(
        checkmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            IdentityTypePage.apply,
            Gen.const(IdentityType.UKCompany),
            controllers.nonsipp.shares.routes.CompanyNameOfSharesSellerController.onPageLoad
          )
          .withName("go from identity type page to company seller name page")
      )

      act.like(
        checkmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            IdentityTypePage.apply,
            Gen.const(IdentityType.UKPartnership),
            controllers.nonsipp.shares.routes.PartnershipNameOfSharesSellerController.onPageLoad
          )
          .withName("go from identity type page to UKPartnership to partnership name of shares seller name page")
      )
    }

    "IndividualNameOfSharesSellerPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            IndividualNameOfSharesSellerPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesIndividualSellerNINumberController
                .onPageLoad(srn, index, CheckMode)
          )
          .withName("go from individual name of shares seller page to shares individual seller NI number page")
      )

      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            IndividualNameOfSharesSellerPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesCYAController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(SharesIndividualSellerNINumberPage(srn, index), ConditionalYesNo.no[String, Nino]("Reason"))
                .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)
          )
          .withName("go from individual name of shares seller page to Shares CYA page")
      )
    }

    "WhenDidSchemeAcquireSharesPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            WhenDidSchemeAcquireSharesPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.common.routes.IdentityTypeController
                .onPageLoad(srn, index, CheckMode, IdentitySubject.SharesSeller),
            srn =>
              defaultUserAnswers.unsafeSet(
                WhyDoesSchemeHoldSharesPage(srn, index),
                SchemeHoldShare.Acquisition
              )
          )
          .withName(
            "go from When Did Scheme Acquire Shares Page to Identity Type page when WhyDoesSchemeHoldShares is Acquisition"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            WhenDidSchemeAcquireSharesPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.ConnectedParty
              )
          )
          .withName("go from When Did Scheme Acquire Shares Page to Shares CYA page when Type Of Shares not Unquoted")
      )

      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            WhenDidSchemeAcquireSharesPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.Unquoted
              )
          )
          .withName(
            "go from When Did Scheme Acquire Shares Page to Shares From Connected Party page when Type Of Shares Unquoted but SharesFromConnectedParty is empty"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            WhenDidSchemeAcquireSharesPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesCYAController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  TypeOfSharesHeldPage(srn, index),
                  TypeOfShares.Unquoted
                )
                .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)
          )
          .withName(
            "go from When Did Scheme Acquire Shares Page to Shares CYA page when Type Of Shares Unquoted and SharesFromConnectedParty is not empty"
          )
      )
    }

    "CompanyNameOfSharesSellerPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            CompanyNameOfSharesSellerPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.common.routes.CompanyRecipientCrnController
                .onPageLoad(srn, index, CheckMode, SharesSeller)
          )
          .withName("go from company name of shares seller page to company recipient crn page")
      )

      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            CompanyNameOfSharesSellerPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesCYAController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  CompanyRecipientCrnPage(srn, index, SharesSeller),
                  ConditionalYesNo.no[String, Crn]("reason")
                )
                .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)
          )
          .withName("go from company name of shares seller page to Shares CYA page")
      )
    }

    "PartnershipShareSellerNamePage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            PartnershipShareSellerNamePage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.common.routes.PartnershipRecipientUtrController
                .onPageLoad(srn, index, CheckMode, SharesSeller)
          )
          .withName("go from Partnership Share Seller Name Page to partnership recipient utr page")
      )

      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            PartnershipShareSellerNamePage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesCYAController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  PartnershipRecipientUtrPage(srn, index, SharesSeller),
                  ConditionalYesNo.no[String, Utr]("reason")
                )
                .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)
          )
          .withName("go from Partnership Share Seller Name Page to Shares CYA page")
      )
    }

    "OtherRecipientDetailsPage" - {

      val shareDetails = RecipientDetails(
        "testName",
        "testDescription"
      )

      act.like(
        checkmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            OtherRecipientDetailsPage.apply,
            Gen.const(shareDetails),
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.Unquoted
              )
          )
          .withName(
            "go from other recipient details page to Shares From Connected Party page when Type Of Shares Unquoted"
          )
      )

      act.like(
        checkmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            OtherRecipientDetailsPage.apply,
            Gen.const(shareDetails),
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesCYAController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  TypeOfSharesHeldPage(srn, index),
                  TypeOfShares.Unquoted
                )
                .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)
          )
          .withName(
            "go from other recipient details page to Shares CYA page when Type Of Shares Unquoted and SharesFromConnectedParty is not empty"
          )
      )

      act.like(
        checkmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            OtherRecipientDetailsPage.apply,
            Gen.const(shareDetails),
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.SponsoringEmployer
              )
          )
          .withName(
            "go from other recipient details page to Total asset value page when Type Of Shares SponsoringEmployer"
          )
      )

      act.like(
        checkmode
          .navigateToWithDataIndexAndSubject(
            index,
            subject,
            OtherRecipientDetailsPage.apply,
            Gen.const(shareDetails),
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.ConnectedParty
              )
          )
          .withName("go from other recipient details page to Shares CYA page when Type Of Shares not Unquoted")
      )

    }

    "SharesIndividualSellerNINumberPage" - {

      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            SharesIndividualSellerNINumberPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.Unquoted
              )
          )
          .withName(
            "go from SharesIndividualSellerNINumberPage to Shares From Connected Party page when Type Of Shares Unquoted but SharesFromConnectedParty is empty"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            SharesIndividualSellerNINumberPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesCYAController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  TypeOfSharesHeldPage(srn, index),
                  TypeOfShares.Unquoted
                )
                .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)
          )
          .withName(
            "go from SharesIndividualSellerNINumberPage to Shares CYA page when Type Of Shares Unquoted and SharesFromConnectedParty is not empty"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            SharesIndividualSellerNINumberPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.SponsoringEmployer
              )
          )
          .withName(
            "go from SharesIndividualSellerNINumberPage to Total asset value page when Type Of Shares SponsoringEmployer"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            SharesIndividualSellerNINumberPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.ConnectedParty
              )
          )
          .withName("go from SharesIndividualSellerNINumberPage to Shares CYA page when Type Of Shares not Unquoted")
      )
    }

    "CompanyRecipientCrnPage" - {
      act.like(
        checkmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            CompanyRecipientCrnPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.Unquoted
              )
          )
          .withName(
            "go from CompanyRecipientCrnPage to Shares From Connected Party page when Type Of Shares Unquoted but SharesFromConnectedParty is empty"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            CompanyRecipientCrnPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesCYAController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  TypeOfSharesHeldPage(srn, index),
                  TypeOfShares.Unquoted
                )
                .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)
          )
          .withName(
            "go from CompanyRecipientCrnPage to Shares CYA page when Type Of Shares Unquoted and SharesFromConnectedParty is not empty"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            CompanyRecipientCrnPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.SponsoringEmployer
              )
          )
          .withName(
            "go from CompanyRecipientCrnPage to Total asset value page when Type Of Shares SponsoringEmployer"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            CompanyRecipientCrnPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.ConnectedParty
              )
          )
          .withName("go from CompanyRecipientCrnPage to Shares CYA page when Type Of Shares not Unquoted")
      )
    }

    "PartnershipRecipientUtrPage" - {
      act.like(
        checkmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            PartnershipRecipientUtrPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesFromConnectedPartyController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.Unquoted
              )
          )
          .withName(
            "go from PartnershipRecipientUtrPage to Shares From Connected Party page when Type Of Shares Unquoted but SharesFromConnectedParty is empty"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            PartnershipRecipientUtrPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.SharesCYAController
                .onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers
                .unsafeSet(
                  TypeOfSharesHeldPage(srn, index),
                  TypeOfShares.Unquoted
                )
                .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)
          )
          .withName(
            "go from PartnershipRecipientUtrPage to Shares CYA page when Type Of Shares Unquoted and SharesFromConnectedParty is not empty"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            PartnershipRecipientUtrPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.SponsoringEmployer
              )
          )
          .withName(
            "go from PartnershipRecipientUtrPage to Total asset value page when Type Of Shares SponsoringEmployer"
          )
      )

      act.like(
        checkmode
          .navigateToWithIndexAndSubject(
            index,
            subject,
            PartnershipRecipientUtrPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
            srn =>
              defaultUserAnswers.unsafeSet(
                TypeOfSharesHeldPage(srn, index),
                TypeOfShares.ConnectedParty
              )
          )
          .withName("go from PartnershipRecipientUtrPage to Shares CYA page when Type Of Shares not Unquoted")
      )
    }

    "SharesFromConnectedPartyPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            SharesFromConnectedPartyPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
          )
          .withName("go from  shares from connected party page to  sharesCYA page")
      )
    }

    "SharesFromConnectedPartyPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            SharesFromConnectedPartyPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode),
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
          .withName("go from  shares from connected party page to TotalAssetValue page")
      )
    }

    "CostOfSharesPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            CostOfSharesPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
          )
          .withName("go from cost of shares page to SharesCYA page")
      )
    }

    "SharesIndependentValuationPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            SharesIndependentValuationPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
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
        checkmode
          .navigateToWithIndex(
            index,
            SharesIndependentValuationPage.apply,
            (srn, _: Int, _) =>
              controllers.nonsipp.shares.routes.TotalAssetValueController.onPageLoad(srn, index, CheckMode),
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
            "go from shares independent valuation page to total asset value page when holding is acquisition"
          )
      )
    }

    "SharesIndependentValuationPage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            SharesIndependentValuationPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
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
        checkmode
          .navigateToWithIndex(
            index,
            SharesIndependentValuationPage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode),
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

    "TotalAssetValuePage" - {
      act.like(
        checkmode
          .navigateToWithIndex(
            index,
            TotalAssetValuePage.apply,
            (srn, _: Int, _) => controllers.nonsipp.shares.routes.SharesCYAController.onPageLoad(srn, index, CheckMode)
          )
          .withName(
            "go from TotalAssetValuePage to SharesCYA Page"
          )
      )
    }

  }
}
