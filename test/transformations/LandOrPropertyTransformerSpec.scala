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

package transformations

import play.api.test.FakeRequest
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.mvc.AnyContentAsEmpty
import models.HowDisposed.Sold
import models.SchemeHoldLandProperty.Acquisition
import controllers.TestValues
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import generators.ModelGenerators.allowedAccessRequestGen
import models._
import pages.nonsipp.common._
import viewmodels.models.SectionCompleted
import models.requests.{AllowedAccessRequest, DataRequest}
import models.requests.psr._
import pages.nonsipp.landorpropertydisposal.{Paths, _}
import org.scalatest.OptionValues
import play.api.libs.json.Json

class LandOrPropertyTransformerSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new LandOrPropertyTransformer()

  "LandOrPropertyTransformer - To Etmp" - {
    "should return None when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn, None, defaultUserAnswers)
      result mustBe None
    }

    "should omit Record Version when there is a change in userAnswers" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandOrPropertyHeldPage(srn), false)
        .unsafeSet(LandOrPropertyRecordVersionPage(srn), "001")

      val initialUserAnswer = emptyUserAnswers
        .unsafeSet(LandOrPropertyHeldPage(srn), true)
        .unsafeSet(LandOrPropertyRecordVersionPage(srn), "001")

      val result =
        transformer.transformToEtmp(srn = srn, Some(false), initialUserAnswer)(
          DataRequest(allowedAccessRequest, userAnswers)
        )
      result mustBe Some(
        LandOrProperty(
          recordVersion = None,
          landOrPropertyHeld = false,
          disposeAnyLandOrProperty = false,
          landOrPropertyTransactions = Seq.empty
        )
      )
    }

    "should return empty List when index as string not a valid number" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandOrPropertyHeldPage(srn), true)
        .unsafeSet(LandOrPropertyRecordVersionPage(srn), "001")
        .unsafeSet(
          Paths.landOrPropertyTransactions \ "propertyDetails" \ "landOrPropertyInUK",
          Json.obj("InvalidIntValue" -> true)
        )

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transformToEtmp(srn, Some(true), userAnswers)(request)
      result mustBe Some(
        LandOrProperty(
          recordVersion = Some("001"),
          landOrPropertyHeld = true,
          disposeAnyLandOrProperty = false,
          landOrPropertyTransactions = Seq.empty
        )
      )
    }
  }

  "LandOrPropertyTransformer - From Etmp" - {
    "when Individual has nino" in {

      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        buildLandOrProperty(
          individualRecipientName,
          PropertyAcquiredFrom(IdentityType.Individual, Some(nino.value), None, None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandOrPropertyRecordVersionPage(srn)) mustBe Some("001")
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
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, refineMV(1), refineMV(1))) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(LandOrPropertyIndividualBuyerNamePage(srn, refineMV(1), refineMV(1))) mustBe Some(
            individualRecipientName
          )
          userAnswers.get(IndividualBuyerNinoNumberPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            ConditionalYesNo.yes(nino)
          )
        }
      )
    }

    "when Individual has no nino" in {

      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        buildLandOrProperty(
          individualRecipientName,
          PropertyAcquiredFrom(IdentityType.Individual, None, Some(noninoReason), None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandOrPropertyRecordVersionPage(srn)) mustBe Some("001")
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

          userAnswers.get(LandPropertyDisposalCompletedPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, refineMV(1), refineMV(1))) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(LandOrPropertyIndividualBuyerNamePage(srn, refineMV(1), refineMV(1))) mustBe Some(
            individualRecipientName
          )
          userAnswers.get(IndividualBuyerNinoNumberPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            ConditionalYesNo.no(noninoReason)
          )
        }
      )
    }

    "when UKCompany has crn" in {

      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        buildLandOrProperty(
          companyRecipientName,
          PropertyAcquiredFrom(IdentityType.UKCompany, Some(crn.value), None, None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandOrPropertyRecordVersionPage(srn)) mustBe Some("001")
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
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, refineMV(1), refineMV(1))) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            IdentityType.UKCompany
          )
          userAnswers.get(CompanyBuyerNamePage(srn, refineMV(1), refineMV(1))) mustBe Some(
            companyRecipientName
          )
          userAnswers.get(CompanyBuyerCrnPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            ConditionalYesNo.yes(crn)
          )
        }
      )
    }

    "when UKCompany has no crn" in {

      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        buildLandOrProperty(
          companyRecipientName,
          PropertyAcquiredFrom(IdentityType.UKCompany, None, Some(noCrnReason), None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandOrPropertyRecordVersionPage(srn)) mustBe Some("001")
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
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, refineMV(1), refineMV(1))) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            IdentityType.UKCompany
          )
          userAnswers.get(CompanyBuyerNamePage(srn, refineMV(1), refineMV(1))) mustBe Some(
            companyRecipientName
          )
          userAnswers.get(CompanyBuyerCrnPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            ConditionalYesNo.no(noCrnReason)
          )
        }
      )
    }

    "when UKPartnership has utr" in {

      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        buildLandOrProperty(
          partnershipRecipientName,
          PropertyAcquiredFrom(IdentityType.UKPartnership, Some(utr.value), None, None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandOrPropertyRecordVersionPage(srn)) mustBe Some("001")
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
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, refineMV(1), refineMV(1))) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            IdentityType.UKPartnership
          )
          userAnswers.get(PartnershipBuyerNamePage(srn, refineMV(1), refineMV(1))) mustBe Some(
            partnershipRecipientName
          )
          userAnswers.get(PartnershipBuyerUtrPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            ConditionalYesNo.yes(utr)
          )
        }
      )
    }

    "when UKPartnership has no utr" in {

      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        buildLandOrProperty(
          partnershipRecipientName,
          PropertyAcquiredFrom(IdentityType.UKPartnership, None, Some(noUtrReason), None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandOrPropertyRecordVersionPage(srn)) mustBe Some("001")
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
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, refineMV(1), refineMV(1))) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            IdentityType.UKPartnership
          )
          userAnswers.get(PartnershipBuyerNamePage(srn, refineMV(1), refineMV(1))) mustBe Some(
            partnershipRecipientName
          )
          userAnswers.get(PartnershipBuyerUtrPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            ConditionalYesNo.no(noUtrReason)
          )
        }
      )
    }

    "when Other" in {

      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        buildLandOrProperty(
          otherRecipientName,
          PropertyAcquiredFrom(IdentityType.Other, None, None, Some(otherRecipientDescription))
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
          userAnswers.get(LandOrPropertyRecordVersionPage(srn)) mustBe Some("001")
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
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, refineMV(1), refineMV(1))) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            IdentityType.Other
          )
          userAnswers
            .get(OtherBuyerDetailsPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            RecipientDetails(otherRecipientName, otherRecipientDescription)
          )
        }
      )
    }
  }

  def buildLandOrProperty(name: String, propertyAcquiredFrom: PropertyAcquiredFrom): LandOrProperty =
    LandOrProperty(
      recordVersion = Some("001"),
      landOrPropertyHeld = true,
      disposeAnyLandOrProperty = true,
      landOrPropertyTransactions = Seq(
        LandOrPropertyTransactions(
          propertyDetails = PropertyDetails(
            landOrPropertyInUK = true,
            addressDetails = address,
            landRegistryTitleNumberKey = true,
            landRegistryTitleNumberValue = "landRegistryTitleNumberValue"
          ),
          heldPropertyTransaction = HeldPropertyTransaction(
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
          ),
          optDisposedPropertyTransaction = Some(
            Seq(
              DisposedPropertyTransaction(
                methodOfDisposal = Sold.name,
                optOtherMethod = None,
                optDateOfSale = Some(localDate),
                optNameOfPurchaser = Some(name),
                optPropertyAcquiredFrom = Some(propertyAcquiredFrom),
                optSaleProceeds = Some(money.value),
                optConnectedPartyStatus = Some(true),
                optIndepValuationSupport = Some(true),
                portionStillHeld = true
              )
            )
          )
        )
      )
    )
}
