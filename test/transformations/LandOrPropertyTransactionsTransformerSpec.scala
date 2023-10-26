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

package transformations

import controllers.TestValues
import eu.timepit.refined.refineMV
import generators.ModelGenerators.allowedAccessRequestGen
import models.SchemeHoldLandProperty.Acquisition
import models.requests.psr._
import models.requests.{AllowedAccessRequest, DataRequest}
import models.{ConditionalYesNo, IdentitySubject, IdentityType, RecipientDetails, SchemeHoldLandProperty}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypePage,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.landorproperty._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import utils.UserAnswersUtils.UserAnswersOps

class LandOrPropertyTransactionsTransformerSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new LandOrPropertyTransactionsTransformer()

  "LandOrPropertyTransactionsTransformer - To Etmp" - {
    "should return empty List when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn)
      result mustBe List.empty
    }

    "should return empty List when index as string not a valid number" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(
          Paths.landOrPropertyTransactions \ "propertyDetails" \ "landOrPropertyInUK",
          Json.obj("InvalidIntValue" -> true)
        )

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transformToEtmp(srn)(request)
      result mustBe List.empty
    }
  }

  "LandOrPropertyTransactionsTransformer - From Etmp" - {
    "when Individual has nino" in {

      val result = transformer.transformFromEtmp(
        defaultUserAnswers,
        srn,
        buildLandOrProperty(
          individualRecipientName,
          PropertyAcquiredFrom(IdentityType.Individual, Some(nino.value), None, None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage()),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandPropertyInUKPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, refineMV(1))) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, refineMV(1))) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1))) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(LandPropertyIndividualSellersNamePage(srn, refineMV(1))) mustBe Some(individualRecipientName)
          userAnswers.get(IndividualSellerNiPage(srn, refineMV(1))) mustBe Some(ConditionalYesNo.yes(nino))
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, refineMV(1))) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, refineMV(1))) mustBe Some(true)
        }
      )
    }

    "when Individual has no nino" in {

      val result = transformer.transformFromEtmp(
        defaultUserAnswers,
        srn,
        buildLandOrProperty(
          individualRecipientName,
          PropertyAcquiredFrom(IdentityType.Individual, None, Some(noninoReason), None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage()),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandPropertyInUKPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, refineMV(1))) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, refineMV(1))) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1))) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(LandPropertyIndividualSellersNamePage(srn, refineMV(1))) mustBe Some(individualRecipientName)
          userAnswers.get(IndividualSellerNiPage(srn, refineMV(1))) mustBe Some(ConditionalYesNo.no(noninoReason))
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, refineMV(1))) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, refineMV(1))) mustBe Some(true)
        }
      )
    }
    "when UKCompany has crn" in {

      val result = transformer.transformFromEtmp(
        defaultUserAnswers,
        srn,
        buildLandOrProperty(
          companyRecipientName,
          PropertyAcquiredFrom(IdentityType.UKCompany, Some(crn.value), None, None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage()),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandPropertyInUKPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, refineMV(1))) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, refineMV(1))) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1))) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.UKCompany
          )
          userAnswers.get(CompanySellerNamePage(srn, refineMV(1))) mustBe Some(companyRecipientName)
          userAnswers.get(CompanyRecipientCrnPage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            ConditionalYesNo.yes(crn)
          )
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, refineMV(1))) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, refineMV(1))) mustBe Some(true)
        }
      )
    }

    "when UKCompany has no crn" in {

      val result = transformer.transformFromEtmp(
        defaultUserAnswers,
        srn,
        buildLandOrProperty(
          companyRecipientName,
          PropertyAcquiredFrom(IdentityType.UKCompany, None, Some(noCrnReason), None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage()),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandPropertyInUKPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, refineMV(1))) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, refineMV(1))) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1))) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.UKCompany
          )
          userAnswers.get(CompanySellerNamePage(srn, refineMV(1))) mustBe Some(companyRecipientName)
          userAnswers.get(CompanyRecipientCrnPage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            ConditionalYesNo.no(noCrnReason)
          )
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, refineMV(1))) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, refineMV(1))) mustBe Some(true)
        }
      )
    }

    "when UKPartnership has utr" in {

      val result = transformer.transformFromEtmp(
        defaultUserAnswers,
        srn,
        buildLandOrProperty(
          partnershipRecipientName,
          PropertyAcquiredFrom(IdentityType.UKPartnership, Some(utr.value), None, None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage()),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandPropertyInUKPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, refineMV(1))) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, refineMV(1))) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1))) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.UKPartnership
          )
          userAnswers.get(PartnershipSellerNamePage(srn, refineMV(1))) mustBe Some(partnershipRecipientName)
          userAnswers
            .get(PartnershipRecipientUtrPage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            ConditionalYesNo.yes(utr)
          )
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, refineMV(1))) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, refineMV(1))) mustBe Some(true)
        }
      )
    }

    "when UKPartnership has no utr" in {

      val result = transformer.transformFromEtmp(
        defaultUserAnswers,
        srn,
        buildLandOrProperty(
          partnershipRecipientName,
          PropertyAcquiredFrom(IdentityType.UKPartnership, None, Some(noUtrReason), None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage()),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandPropertyInUKPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, refineMV(1))) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, refineMV(1))) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1))) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.UKPartnership
          )
          userAnswers.get(PartnershipSellerNamePage(srn, refineMV(1))) mustBe Some(partnershipRecipientName)
          userAnswers
            .get(PartnershipRecipientUtrPage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            ConditionalYesNo.no(noUtrReason)
          )
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, refineMV(1))) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, refineMV(1))) mustBe Some(true)
        }
      )
    }

    "when Other" in {

      val result = transformer.transformFromEtmp(
        defaultUserAnswers,
        srn,
        buildLandOrProperty(
          otherRecipientName,
          PropertyAcquiredFrom(IdentityType.Other, None, None, Some(otherRecipientDescription))
        )
      )
      result.fold(
        ex => fail(ex.getMessage()),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandPropertyInUKPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, refineMV(1))) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, refineMV(1))) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, refineMV(1))) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.Other
          )
          userAnswers
            .get(OtherRecipientDetailsPage(srn, refineMV(1), IdentitySubject.LandOrPropertySeller)) mustBe Some(
            RecipientDetails(otherRecipientName, otherRecipientDescription)
          )
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, refineMV(1))) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, refineMV(1))) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, refineMV(1))) mustBe Some(true)
        }
      )
    }
  }

  def buildLandOrProperty(name: String, propertyAcquiredFrom: PropertyAcquiredFrom): LandOrProperty =
    LandOrProperty(
      landOrPropertyHeld = true,
      landOrPropertyTransactions = Seq(
        LandOrPropertyTransactions(
          PropertyDetails(
            landOrPropertyInUK = true,
            addressDetails = address,
            landRegistryTitleNumberKey = true,
            landRegistryTitleNumberValue = "landRegistryTitleNumberValue"
          ),
          HeldPropertyTransaction(
            methodOfHolding = Acquisition,
            dateOfAcquisitionOrContribution = Some(localDate),
            optPropertyAcquiredFromName = Some(name),
            optPropertyAcquiredFrom = Some(propertyAcquiredFrom),
            optConnectedPartyStatus = Some(true),
            totalCostOfLandOrProperty = money.value,
            optIndepValuationSupport = Some(false),
            isLandOrPropertyResidential = true,
            optLeaseDetails = Some(
              LeaseDetails(
                lesseeName = "lesseeName",
                leaseGrantDate = localDate,
                annualLeaseAmount = money.value,
                connectedPartyStatus = false
              )
            ),
            landOrPropertyLeased = true,
            totalIncomeOrReceipts = money.value
          )
        )
      )
    )
}
