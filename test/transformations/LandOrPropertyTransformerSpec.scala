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
import models.IdentityType.Individual
import models.SchemeHoldLandProperty.Acquisition
import controllers.TestValues
import utils.UserAnswersUtils.UserAnswersOps
import generators.ModelGenerators.allowedAccessRequestGen
import models._
import pages.nonsipp.common._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import models.requests.{AllowedAccessRequest, DataRequest}
import utils.IntUtils.given
import pages.nonsipp.landorproperty._
import models.requests.psr._
import config.Constants.PREPOPULATION_FLAG
import pages.nonsipp.landorpropertydisposal.{Paths, _}
import org.scalatest.OptionValues
import uk.gov.hmrc.domain.Nino
import play.api.libs.json.Json

class LandOrPropertyTransformerSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  private val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest()
  ).sample.value
  private val allowedAccessRequestPrePopulation: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest()
      .withSession((PREPOPULATION_FLAG, "true"))
  ).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)
  val userAnswers: UserAnswers = emptyUserAnswers
    .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
    .unsafeSet(LandOrPropertyPostcodeLookupPage(srn, index1of5000), postcodeLookup)
    .unsafeSet(AddressLookupResultsPage(srn, index1of5000), List(address, address, address))
    .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
    .unsafeSet(
      LandRegistryTitleNumberPage(srn, index1of5000),
      ConditionalYesNo.yes[String, String]("landRegistryTitleNumber")
    )
    .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Transfer)
    .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), Money(100000.0))
    .unsafeSet(IsLandOrPropertyResidentialPage(srn, index1of5000), false)
    .unsafeSet(LandOrPropertyTotalIncomePage(srn, index1of5000), Money(100000.0))
    .unsafeSet(LandOrPropertyProgress(srn, 1), SectionJourneyStatus.Completed)

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
        transformer.transformToEtmp(srn = srn, Some(false), initialUserAnswer)(using
          DataRequest(allowedAccessRequest, userAnswers)
        )
      result mustBe Some(
        LandOrProperty(
          recordVersion = None,
          optLandOrPropertyHeld = Some(false),
          optDisposeAnyLandOrProperty = Some(false),
          landOrPropertyTransactions = Seq.empty
        )
      )
    }

    "should return empty List when index as string not a valid number" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandOrPropertyHeldPage(srn), true)
        .unsafeSet(LandOrPropertyDisposalPage(srn), false)
        .unsafeSet(LandOrPropertyRecordVersionPage(srn), "001")
        .unsafeSet(
          Paths.landOrPropertyTransactions \ "propertyDetails" \ "landOrPropertyInUK",
          Json.obj("InvalidIntValue" -> true)
        )

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transformToEtmp(srn, Some(true), userAnswers)(using request)
      result mustBe Some(
        LandOrProperty(
          recordVersion = Some("001"),
          optLandOrPropertyHeld = Some(true),
          optDisposeAnyLandOrProperty = Some(false),
          landOrPropertyTransactions = Seq.empty
        )
      )
    }

    "should return disposals None when LandOrPropertyDisposalPage is None and it is pre-population " in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandOrPropertyHeldPage(srn), true)

      val request = DataRequest(allowedAccessRequestPrePopulation, userAnswers)

      val result = transformer.transformToEtmp(srn, Some(true), userAnswers)(using request)
      result mustBe Some(
        LandOrProperty(
          recordVersion = None,
          optLandOrPropertyHeld = Some(true),
          optDisposeAnyLandOrProperty = None,
          landOrPropertyTransactions = Seq.empty // tests don't need to check these transactions in detail
        )
      )
    }

    "should not include lease details when leased is false" in {

      val modifiedUserAnswers = userAnswers.unsafeSet(IsLandPropertyLeasedPage(srn, index1of5000), false)

      val request = DataRequest(allowedAccessRequest, modifiedUserAnswers)

      val result = transformer.transformToEtmp(srn, Some(true), modifiedUserAnswers)(using request)

      result mustBe Some(
        LandOrProperty(
          recordVersion = None,
          optLandOrPropertyHeld = Some(true),
          optDisposeAnyLandOrProperty = Some(false),
          landOrPropertyTransactions = List(
            LandOrPropertyTransactions(
              prePopulated = None,
              propertyDetails = PropertyDetails(
                landOrPropertyInUK = true,
                addressDetails = address,
                landRegistryTitleNumberKey = true,
                landRegistryTitleNumberValue = "landRegistryTitleNumber"
              ),
              heldPropertyTransaction = HeldPropertyTransaction(
                methodOfHolding = SchemeHoldLandProperty.Transfer,
                dateOfAcquisitionOrContribution = None,
                optPropertyAcquiredFromName = None,
                optPropertyAcquiredFrom = None,
                optConnectedPartyStatus = None,
                totalCostOfLandOrProperty = 100000.0,
                optIndepValuationSupport = None,
                optIsLandOrPropertyResidential = Some(false),
                optLeaseDetails = None,
                optLandOrPropertyLeased = Some(false),
                optTotalIncomeOrReceipts = Some(100000.0)
              ),
              optDisposedPropertyTransaction = None
            )
          )
        )
      )
    }

    "should not include lease details when leased is not set" in {

      val modifiedUserAnswers = userAnswers.remove(IsLandPropertyLeasedPage(srn, index1of5000)).get
      val request = DataRequest(allowedAccessRequest, modifiedUserAnswers)

      val result = transformer.transformToEtmp(srn, Some(true), modifiedUserAnswers)(using request)

      result mustBe Some(
        LandOrProperty(
          recordVersion = None,
          optLandOrPropertyHeld = Some(true),
          optDisposeAnyLandOrProperty = Some(false),
          landOrPropertyTransactions = List(
            LandOrPropertyTransactions(
              prePopulated = None,
              propertyDetails = PropertyDetails(
                landOrPropertyInUK = true,
                addressDetails = address,
                landRegistryTitleNumberKey = true,
                landRegistryTitleNumberValue = "landRegistryTitleNumber"
              ),
              heldPropertyTransaction = HeldPropertyTransaction(
                methodOfHolding = SchemeHoldLandProperty.Transfer,
                dateOfAcquisitionOrContribution = None,
                optPropertyAcquiredFromName = None,
                optPropertyAcquiredFrom = None,
                optConnectedPartyStatus = None,
                totalCostOfLandOrProperty = 100000.0,
                optIndepValuationSupport = None,
                optIsLandOrPropertyResidential = Some(false),
                optLeaseDetails = None,
                optLandOrPropertyLeased = None,
                optTotalIncomeOrReceipts = Some(100000.0)
              ),
              optDisposedPropertyTransaction = None
            )
          )
        )
      )
    }

    "should not include incomplete record" in {

      val incompleteUserAnswers = emptyUserAnswers
        .unsafeSet(LandOrPropertyHeldPage(srn), true)
        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyPostcodeLookupPage(srn, index1of5000), postcodeLookup)
        .unsafeSet(AddressLookupResultsPage(srn, index1of5000), List(address, address, address))
        .unsafeSet(
          LandOrPropertyProgress(srn, index1of5000),
          SectionJourneyStatus.InProgress(LandRegistryTitleNumberPage(srn, index1of5000))
        )

      val request = DataRequest(allowedAccessRequest, incompleteUserAnswers)

      val result = transformer.transformToEtmp(srn, Some(true), incompleteUserAnswers)(using request)

      result mustBe Some(LandOrProperty(None, Some(true), Some(false), List()))
    }

    "should include complete disposal" in {

      val incompleteUserAnswers = userAnswers
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, index1of5000, index1of50), HowDisposed.Sold)
        .unsafeSet(
          LandOrPropertyDisposalProgress(srn, index1of5000, index1of50),
          SectionJourneyStatus.InProgress("some-url")
        )

      val request = DataRequest(allowedAccessRequest, incompleteUserAnswers)

      val result = transformer.transformToEtmp(srn, Some(true), incompleteUserAnswers)(using request)

      result.get.landOrPropertyTransactions.head.optDisposedPropertyTransaction mustBe None
    }

    "should not include incomplete disposal" in {

      val incompleteUserAnswers = userAnswers
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, index1of5000, index1of50), HowDisposed.Sold)
        .unsafeSet(LandOrPropertyDisposalPage(srn), true)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, 1, 1), Sold)
        .unsafeSet(LandOrPropertyStillHeldPage(srn, 1, 1), true)
        .unsafeSet(WhenWasPropertySoldPage(srn, 1, 1), localDate)
        .unsafeSet(
          LandOrPropertyDisposalBuyerConnectedPartyPage(srn, 1, 1),
          true
        )
        .unsafeSet(TotalProceedsSaleLandPropertyPage(srn, 1, 1), money)
        .unsafeSet(DisposalIndependentValuationPage(srn, 1, 1), true)
        .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, 1, 1), IdentityType.Individual)
        .unsafeSet(LandOrPropertyIndividualBuyerNamePage(srn, 1, 1), individualRecipientName)
        .unsafeSet(
          IndividualBuyerNinoNumberPage(srn, 1, 1),
          ConditionalYesNo.no[String, Nino](noninoReason)
        )
        .unsafeSet(LandPropertyDisposalCompletedPage(srn, 1, 1), SectionCompleted)
        .unsafeSet(LandOrPropertyDisposalProgress(srn, 1, 1), SectionJourneyStatus.Completed)

      val request = DataRequest(allowedAccessRequest, incompleteUserAnswers)

      val result = transformer.transformToEtmp(srn, Some(true), incompleteUserAnswers)(using request)

      result.get.landOrPropertyTransactions.head.optDisposedPropertyTransaction.map(_.size) mustBe Some(1)
    }

    "should transform acquisition from a UK Company seller with CRN" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
        .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.yes("landRegistryTitleNumber"))
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), Acquisition)
        .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
        .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
        .unsafeSet(IdentityTypePage(srn, index1of5000, IdentitySubject.LandOrPropertySeller), IdentityType.UKCompany)
        .unsafeSet(CompanySellerNamePage(srn, index1of5000), "companyName")
        .unsafeSet(
          CompanyRecipientCrnPage(srn, index1of5000, IdentitySubject.LandOrPropertySeller),
          ConditionalYesNo.yes(crn)
        )
        .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), false)
        .unsafeSet(LandOrPropertyProgress(srn, index1of5000), SectionJourneyStatus.Completed)

      val result =
        transformer.transformToEtmp(srn, Some(true), userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))

      val heldTransaction = result.value.landOrPropertyTransactions.head.heldPropertyTransaction
      heldTransaction.optPropertyAcquiredFromName mustBe Some("companyName")
      heldTransaction.optPropertyAcquiredFrom mustBe Some(
        PropertyAcquiredFrom(
          identityType = IdentityType.UKCompany,
          idNumber = Some(crn.value),
          reasonNoIdNumber = None,
          otherDescription = None
        )
      )
      heldTransaction.optConnectedPartyStatus mustBe Some(false)
    }

    "should transform acquisition from an Other seller" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
        .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.yes("landRegistryTitleNumber"))
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), Acquisition)
        .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
        .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
        .unsafeSet(IdentityTypePage(srn, index1of5000, IdentitySubject.LandOrPropertySeller), IdentityType.Other)
        .unsafeSet(
          OtherRecipientDetailsPage(srn, index1of5000, IdentitySubject.LandOrPropertySeller),
          RecipientDetails("otherSeller", "some description")
        )
        .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyProgress(srn, index1of5000), SectionJourneyStatus.Completed)

      val result =
        transformer.transformToEtmp(srn, Some(true), userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))

      val heldTransaction = result.value.landOrPropertyTransactions.head.heldPropertyTransaction
      heldTransaction.optPropertyAcquiredFromName mustBe Some("otherSeller")
      heldTransaction.optPropertyAcquiredFrom mustBe Some(
        PropertyAcquiredFrom(
          identityType = IdentityType.Other,
          idNumber = None,
          reasonNoIdNumber = None,
          otherDescription = Some("some description")
        )
      )
      heldTransaction.optConnectedPartyStatus mustBe Some(true)
    }

    "should transform disposal with 'Transferred' method" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
        .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.yes("landRegistryTitleNumber"))
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Transfer)
        .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
        .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
        .unsafeSet(LandOrPropertyProgress(srn, index1of5000), SectionJourneyStatus.Completed)
        .unsafeSet(LandOrPropertyDisposalPage(srn), true)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, index1of5000, index1of50), HowDisposed.Transferred)
        .unsafeSet(LandOrPropertyStillHeldPage(srn, index1of5000, index1of50), true)
        .unsafeSet(LandOrPropertyDisposalProgress(srn, index1of5000, index1of50), SectionJourneyStatus.Completed)

      val result =
        transformer.transformToEtmp(srn, Some(true), userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))

      val disposedTransaction = result.value.landOrPropertyTransactions.head.optDisposedPropertyTransaction.value.head
      disposedTransaction.methodOfDisposal mustBe HowDisposed.Transferred.name
      disposedTransaction.optOtherMethod mustBe None
      disposedTransaction.optDateOfSale mustBe None
      disposedTransaction.optNameOfPurchaser mustBe None
      disposedTransaction.optPropertyAcquiredFrom mustBe None
      disposedTransaction.optSaleProceeds mustBe None
      disposedTransaction.optConnectedPartyStatus mustBe None
      disposedTransaction.optIndepValuationSupport mustBe None
    }

    "should transform disposal with 'Other' method" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
        .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.yes("landRegistryTitleNumber"))
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Transfer)
        .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
        .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
        .unsafeSet(LandOrPropertyProgress(srn, index1of5000), SectionJourneyStatus.Completed)
        .unsafeSet(LandOrPropertyDisposalPage(srn), true)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, index1of5000, index1of50), HowDisposed.Other("some other reason"))
        .unsafeSet(LandOrPropertyStillHeldPage(srn, index1of5000, index1of50), false)
        .unsafeSet(LandOrPropertyDisposalProgress(srn, index1of5000, index1of50), SectionJourneyStatus.Completed)

      val result =
        transformer.transformToEtmp(srn, Some(true), userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))

      val disposedTransaction = result.value.landOrPropertyTransactions.head.optDisposedPropertyTransaction.value.head
      disposedTransaction.methodOfDisposal mustBe HowDisposed.Other("some other reason").name
      disposedTransaction.optOtherMethod mustBe Some("some other reason")
    }

    "should transform disposal to a UK Partnership buyer with UTR" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
        .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.yes("landRegistryTitleNumber"))
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Transfer)
        .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
        .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
        .unsafeSet(LandOrPropertyProgress(srn, index1of5000), SectionJourneyStatus.Completed)
        .unsafeSet(LandOrPropertyDisposalPage(srn), true)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, index1of5000, index1of50), Sold)
        .unsafeSet(LandOrPropertyStillHeldPage(srn, index1of5000, index1of50), true)
        .unsafeSet(WhenWasPropertySoldPage(srn, index1of5000, index1of50), localDate)
        .unsafeSet(TotalProceedsSaleLandPropertyPage(srn, index1of5000, index1of50), money)
        .unsafeSet(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index1of5000, index1of50), false)
        .unsafeSet(DisposalIndependentValuationPage(srn, index1of5000, index1of50), true)
        .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, index1of5000, index1of50), IdentityType.UKPartnership)
        .unsafeSet(PartnershipBuyerNamePage(srn, index1of5000, index1of50), "partnershipBuyer")
        .unsafeSet(PartnershipBuyerUtrPage(srn, index1of5000, index1of50), ConditionalYesNo.yes(utr))
        .unsafeSet(LandOrPropertyDisposalProgress(srn, index1of5000, index1of50), SectionJourneyStatus.Completed)

      val result =
        transformer.transformToEtmp(srn, Some(true), userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))

      val disposedTransaction = result.value.landOrPropertyTransactions.head.optDisposedPropertyTransaction.value.head
      disposedTransaction.optNameOfPurchaser mustBe Some("partnershipBuyer")
      disposedTransaction.optPropertyAcquiredFrom mustBe Some(
        PropertyAcquiredFrom(
          identityType = IdentityType.UKPartnership,
          idNumber = Some(utr.value),
          reasonNoIdNumber = None,
          otherDescription = None
        )
      )
    }

    "should transform disposal to an Other buyer" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
        .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.yes("landRegistryTitleNumber"))
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Transfer)
        .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
        .unsafeSet(LandPropertyIndependentValuationPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyWhenDidSchemeAcquirePage(srn, index1of5000), localDate)
        .unsafeSet(LandOrPropertyProgress(srn, index1of5000), SectionJourneyStatus.Completed)
        .unsafeSet(LandOrPropertyDisposalPage(srn), true)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, index1of5000, index1of50), Sold)
        .unsafeSet(LandOrPropertyStillHeldPage(srn, index1of5000, index1of50), true)
        .unsafeSet(WhenWasPropertySoldPage(srn, index1of5000, index1of50), localDate)
        .unsafeSet(TotalProceedsSaleLandPropertyPage(srn, index1of5000, index1of50), money)
        .unsafeSet(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index1of5000, index1of50), true)
        .unsafeSet(DisposalIndependentValuationPage(srn, index1of5000, index1of50), false)
        .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, index1of5000, index1of50), IdentityType.Other)
        .unsafeSet(
          OtherBuyerDetailsPage(srn, index1of5000, index1of50),
          RecipientDetails("otherBuyer", "another description")
        )
        .unsafeSet(LandOrPropertyDisposalProgress(srn, index1of5000, index1of50), SectionJourneyStatus.Completed)

      val result =
        transformer.transformToEtmp(srn, Some(true), userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))

      val disposedTransaction = result.value.landOrPropertyTransactions.head.optDisposedPropertyTransaction.value.head
      disposedTransaction.optNameOfPurchaser mustBe Some("otherBuyer")
      disposedTransaction.optPropertyAcquiredFrom mustBe Some(
        PropertyAcquiredFrom(
          identityType = IdentityType.Other,
          idNumber = None,
          reasonNoIdNumber = None,
          otherDescription = Some("another description")
        )
      )
    }

    "should transform correctly when method of holding is Transfer" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LandPropertyInUKPage(srn, index1of5000), true)
        .unsafeSet(LandOrPropertyChosenAddressPage(srn, index1of5000), address)
        .unsafeSet(LandRegistryTitleNumberPage(srn, index1of5000), ConditionalYesNo.yes("landRegistryTitleNumber"))
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index1of5000), SchemeHoldLandProperty.Transfer)
        .unsafeSet(LandOrPropertyTotalCostPage(srn, index1of5000), money)
        .unsafeSet(LandOrPropertyProgress(srn, index1of5000), SectionJourneyStatus.Completed)

      val result =
        transformer.transformToEtmp(srn, Some(true), userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))

      val heldTransaction = result.value.landOrPropertyTransactions.head.heldPropertyTransaction
      heldTransaction.methodOfHolding mustBe SchemeHoldLandProperty.Transfer
      heldTransaction.dateOfAcquisitionOrContribution mustBe None
      heldTransaction.optIndepValuationSupport mustBe None
      heldTransaction.optPropertyAcquiredFromName mustBe None
      heldTransaction.optPropertyAcquiredFrom mustBe None
      heldTransaction.optConnectedPartyStatus mustBe None
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
          userAnswers.get(LandPropertyInUKPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, 1)) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, 1)) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, 1)) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, 1)) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, 1)) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, 1)) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, 1)) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(LandPropertyIndividualSellersNamePage(srn, 1)) mustBe Some(individualRecipientName)
          userAnswers.get(IndividualSellerNiPage(srn, 1)) mustBe Some(ConditionalYesNo.yes(nino))
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, 1)) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, 1)) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, 1, 1)) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalProgress(srn, 1, 1)) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, 1, 1)) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, 1, 1)) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, 1, 1)) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, 1, 1)) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, 1, 1)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(LandOrPropertyIndividualBuyerNamePage(srn, 1, 1)) mustBe Some(
            individualRecipientName
          )
          userAnswers.get(IndividualBuyerNinoNumberPage(srn, 1, 1)) mustBe Some(
            ConditionalYesNo.yes(nino)
          )
          userAnswers.get(LandOrPropertyProgress(srn, 1)) mustBe Some(SectionJourneyStatus.Completed)
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
          userAnswers.get(LandPropertyInUKPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, 1)) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, 1)) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, 1)) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, 1)) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, 1)) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, 1)) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, 1)) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(LandPropertyIndividualSellersNamePage(srn, 1)) mustBe Some(individualRecipientName)
          userAnswers.get(IndividualSellerNiPage(srn, 1)) mustBe Some(ConditionalYesNo.no(noninoReason))
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, 1)) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, 1)) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, 1)) mustBe Some(true)

          userAnswers.get(LandPropertyDisposalCompletedPage(srn, 1, 1)) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalProgress(srn, 1, 1)) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, 1, 1)) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, 1, 1)) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, 1, 1)) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, 1, 1)) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, 1, 1)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(LandOrPropertyIndividualBuyerNamePage(srn, 1, 1)) mustBe Some(
            individualRecipientName
          )
          userAnswers.get(IndividualBuyerNinoNumberPage(srn, 1, 1)) mustBe Some(
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
          userAnswers.get(LandPropertyInUKPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, 1)) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, 1)) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, 1)) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, 1)) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, 1)) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, 1)) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, 1)) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.UKCompany
          )
          userAnswers.get(CompanySellerNamePage(srn, 1)) mustBe Some(companyRecipientName)
          userAnswers.get(CompanyRecipientCrnPage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            ConditionalYesNo.yes(crn)
          )
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, 1)) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, 1)) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, 1, 1)) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalProgress(srn, 1, 1)) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, 1, 1)) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, 1, 1)) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, 1, 1)) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, 1, 1)) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, 1, 1)) mustBe Some(
            IdentityType.UKCompany
          )
          userAnswers.get(CompanyBuyerNamePage(srn, 1, 1)) mustBe Some(
            companyRecipientName
          )
          userAnswers.get(CompanyBuyerCrnPage(srn, 1, 1)) mustBe Some(
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
          userAnswers.get(LandPropertyInUKPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, 1)) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, 1)) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, 1)) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, 1)) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, 1)) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, 1)) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, 1)) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.UKCompany
          )
          userAnswers.get(CompanySellerNamePage(srn, 1)) mustBe Some(companyRecipientName)
          userAnswers.get(CompanyRecipientCrnPage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            ConditionalYesNo.no(noCrnReason)
          )
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, 1)) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, 1)) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, 1, 1)) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalProgress(srn, 1, 1)) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, 1, 1)) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, 1, 1)) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, 1, 1)) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, 1, 1)) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, 1, 1)) mustBe Some(
            IdentityType.UKCompany
          )
          userAnswers.get(CompanyBuyerNamePage(srn, 1, 1)) mustBe Some(
            companyRecipientName
          )
          userAnswers.get(CompanyBuyerCrnPage(srn, 1, 1)) mustBe Some(
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
          userAnswers.get(LandPropertyInUKPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, 1)) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, 1)) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, 1)) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, 1)) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, 1)) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, 1)) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, 1)) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.UKPartnership
          )
          userAnswers.get(PartnershipSellerNamePage(srn, 1)) mustBe Some(partnershipRecipientName)
          userAnswers
            .get(PartnershipRecipientUtrPage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            ConditionalYesNo.yes(utr)
          )
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, 1)) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, 1)) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, 1, 1)) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalProgress(srn, 1, 1)) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, 1, 1)) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, 1, 1)) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, 1, 1)) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, 1, 1)) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, 1, 1)) mustBe Some(
            IdentityType.UKPartnership
          )
          userAnswers.get(PartnershipBuyerNamePage(srn, 1, 1)) mustBe Some(
            partnershipRecipientName
          )
          userAnswers.get(PartnershipBuyerUtrPage(srn, 1, 1)) mustBe Some(
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
          userAnswers.get(LandPropertyInUKPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, 1)) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, 1)) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, 1)) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, 1)) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, 1)) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, 1)) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, 1)) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.UKPartnership
          )
          userAnswers.get(PartnershipSellerNamePage(srn, 1)) mustBe Some(partnershipRecipientName)
          userAnswers
            .get(PartnershipRecipientUtrPage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            ConditionalYesNo.no(noUtrReason)
          )
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, 1)) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, 1)) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, 1, 1)) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, 1, 1)) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, 1, 1)) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, 1, 1)) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, 1, 1)) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, 1, 1)) mustBe Some(
            IdentityType.UKPartnership
          )
          userAnswers.get(PartnershipBuyerNamePage(srn, 1, 1)) mustBe Some(
            partnershipRecipientName
          )
          userAnswers.get(PartnershipBuyerUtrPage(srn, 1, 1)) mustBe Some(
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
          userAnswers.get(LandPropertyInUKPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyChosenAddressPage(srn, 1)) mustBe Some(address)
          userAnswers.get(LandRegistryTitleNumberPage(srn, 1)) mustBe Some(
            ConditionalYesNo.yes("landRegistryTitleNumberValue")
          )
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, 1)) mustBe Some(
            SchemeHoldLandProperty.Acquisition
          )
          userAnswers.get(LandOrPropertyTotalCostPage(srn, 1)) mustBe Some(money)
          userAnswers.get(IsLandPropertyLeasedPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, 1)) mustBe Some(money)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, 1)) mustBe Some(localDate)
          userAnswers.get(LandPropertyIndependentValuationPage(srn, 1)) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            IdentityType.Other
          )
          userAnswers
            .get(OtherRecipientDetailsPage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe Some(
            RecipientDetails(otherRecipientName, otherRecipientDescription)
          )
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, 1)) mustBe Some(
            ("lesseeName", money, localDate)
          )
          userAnswers.get(IsLesseeConnectedPartyPage(srn, 1)) mustBe Some(false)
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, 1)) mustBe Some(true)
          userAnswers.get(LandPropertyDisposalCompletedPage(srn, 1, 1)) mustBe Some(
            SectionCompleted
          )
          userAnswers.get(LandOrPropertyDisposalProgress(srn, 1, 1)) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, 1, 1)) mustBe Some(Sold)
          userAnswers.get(LandOrPropertyStillHeldPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhenWasPropertySoldPage(srn, 1, 1)) mustBe Some(localDate)
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, 1, 1)) mustBe Some(
            true
          )
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, 1, 1)) mustBe Some(money)
          userAnswers.get(DisposalIndependentValuationPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, 1, 1)) mustBe Some(
            IdentityType.Other
          )
          userAnswers
            .get(OtherBuyerDetailsPage(srn, 1, 1)) mustBe Some(
            RecipientDetails(otherRecipientName, otherRecipientDescription)
          )
        }
      )
    }

    "should not default total income to zero when prePopulated entity is not yet checked" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        landOrPropertyWithEmptyOptionalValues(prePopulated = Some(false), recordVersion = Some("001"), Some(1))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => userAnswers.get(LandOrPropertyTotalIncomePage(srn, 1)) mustBe None
      )
    }

    "should default total income to zero when prePopulated entity is checked" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        landOrPropertyWithEmptyOptionalValues(prePopulated = Some(true), recordVersion = Some("001"))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, 1)) mustBe Some(Money(0))
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, 1)).map(_._2) mustBe Some(Money(0))
        }
      )
    }

    "should default total income to zero when the version of the return is more than 1" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        landOrPropertyWithEmptyOptionalValues(prePopulated = None, recordVersion = Some("002"))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyTotalIncomePage(srn, 1)) mustBe Some(Money(0))
          userAnswers.get(LandOrPropertyLeaseDetailsPage(srn, 1)).map(_._2) mustBe Some(Money(0))
        }
      )
    }

    "when optLandOrPropertyHeld is false" in {
      val landOrProperty = LandOrProperty(
        recordVersion = Some("001"),
        optLandOrPropertyHeld = Some(false),
        optDisposeAnyLandOrProperty = Some(false),
        landOrPropertyTransactions = Seq.empty
      )

      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, landOrProperty)

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyHeldPage(srn)) mustBe Some(false)
          userAnswers.get(LandOrPropertyRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe None
          userAnswers.get(LandPropertyInUKPage(srn, index1of5000)) mustBe None // No transactions should be set
        }
      )
    }

    "when optDisposeAnyLandOrProperty is false" in {
      val landOrProperty = LandOrProperty(
        recordVersion = Some("001"),
        optLandOrPropertyHeld = Some(true),
        optDisposeAnyLandOrProperty = Some(false), // Explicitly false
        landOrPropertyTransactions = Seq(
          LandOrPropertyTransactions(
            prePopulated = None,
            propertyDetails = PropertyDetails(
              landOrPropertyInUK = true,
              addressDetails = address,
              landRegistryTitleNumberKey = true,
              landRegistryTitleNumberValue = "landRegistryTitleNumberValue"
            ),
            heldPropertyTransaction = HeldPropertyTransaction(
              methodOfHolding = Acquisition,
              dateOfAcquisitionOrContribution = Some(localDate),
              optPropertyAcquiredFromName = Some(individualRecipientName),
              optPropertyAcquiredFrom =
                Some(PropertyAcquiredFrom(IdentityType.Individual, Some(nino.value), None, None)),
              optConnectedPartyStatus = Some(true),
              totalCostOfLandOrProperty = money.value,
              optIndepValuationSupport = Some(false),
              optIsLandOrPropertyResidential = Some(true),
              optLeaseDetails = None,
              optLandOrPropertyLeased = Some(false),
              optTotalIncomeOrReceipts = Some(money.value)
            ),
            optDisposedPropertyTransaction = None // No disposals in ETMP model
          )
        )
      )

      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, landOrProperty)

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LandOrPropertyDisposalPage(srn)) mustBe Some(false)
          userAnswers.get(
            HowWasPropertyDisposedOfPage(srn, index1of5000, index1of50)
          ) mustBe None // No disposal details should be set
        }
      )
    }

    "when method of holding is Transfer" in {
      val landOrProperty = LandOrProperty(
        recordVersion = Some("001"),
        optLandOrPropertyHeld = Some(true),
        optDisposeAnyLandOrProperty = Some(false),
        landOrPropertyTransactions = Seq(
          LandOrPropertyTransactions(
            prePopulated = None,
            propertyDetails = PropertyDetails(
              landOrPropertyInUK = true,
              addressDetails = address,
              landRegistryTitleNumberKey = true,
              landRegistryTitleNumberValue = "landRegistryTitleNumberValue"
            ),
            heldPropertyTransaction = HeldPropertyTransaction(
              methodOfHolding = SchemeHoldLandProperty.Transfer, // Transfer
              dateOfAcquisitionOrContribution = None,
              optPropertyAcquiredFromName = None,
              optPropertyAcquiredFrom = None,
              optConnectedPartyStatus = None,
              totalCostOfLandOrProperty = money.value,
              optIndepValuationSupport = None,
              optIsLandOrPropertyResidential = Some(false),
              optLeaseDetails = None,
              optLandOrPropertyLeased = Some(false),
              optTotalIncomeOrReceipts = Some(money.value)
            ),
            optDisposedPropertyTransaction = None
          )
        )
      )

      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, landOrProperty)

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, 1)) mustBe Some(SchemeHoldLandProperty.Transfer)
          userAnswers.get(LandOrPropertyWhenDidSchemeAcquirePage(srn, 1)) mustBe None
          userAnswers.get(LandPropertyIndependentValuationPage(srn, 1)) mustBe None
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LandOrPropertySeller)) mustBe None
          userAnswers.get(LandOrPropertySellerConnectedPartyPage(srn, 1)) mustBe None
        }
      )
    }

    "when method of disposal is Transferred" in {
      val landOrProperty = LandOrProperty(
        recordVersion = Some("001"),
        optLandOrPropertyHeld = Some(true),
        optDisposeAnyLandOrProperty = Some(true),
        landOrPropertyTransactions = Seq(
          LandOrPropertyTransactions(
            prePopulated = None,
            propertyDetails = PropertyDetails(
              landOrPropertyInUK = true,
              addressDetails = address,
              landRegistryTitleNumberKey = true,
              landRegistryTitleNumberValue = "landRegistryTitleNumberValue"
            ),
            heldPropertyTransaction = HeldPropertyTransaction(
              methodOfHolding = Acquisition,
              dateOfAcquisitionOrContribution = Some(localDate),
              optPropertyAcquiredFromName = Some(individualRecipientName),
              optPropertyAcquiredFrom =
                Some(PropertyAcquiredFrom(IdentityType.Individual, Some(nino.value), None, None)),
              optConnectedPartyStatus = Some(true),
              totalCostOfLandOrProperty = money.value,
              optIndepValuationSupport = Some(false),
              optIsLandOrPropertyResidential = Some(true),
              optLeaseDetails = None,
              optLandOrPropertyLeased = Some(false),
              optTotalIncomeOrReceipts = Some(money.value)
            ),
            optDisposedPropertyTransaction = Some(
              Seq(
                DisposedPropertyTransaction(
                  methodOfDisposal = HowDisposed.Transferred.name, // Transferred
                  optOtherMethod = None,
                  optDateOfSale = None,
                  optNameOfPurchaser = None,
                  optPropertyAcquiredFrom = None,
                  optSaleProceeds = None,
                  optConnectedPartyStatus = None,
                  optIndepValuationSupport = None,
                  portionStillHeld = true
                )
              )
            )
          )
        )
      )

      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, landOrProperty)

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, 1, 1)) mustBe Some(HowDisposed.Transferred)
          userAnswers.get(WhenWasPropertySoldPage(srn, 1, 1)) mustBe None
          userAnswers.get(TotalProceedsSaleLandPropertyPage(srn, 1, 1)) mustBe None
          userAnswers.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, 1, 1)) mustBe None
          userAnswers.get(DisposalIndependentValuationPage(srn, 1, 1)) mustBe None
          userAnswers.get(WhoPurchasedLandOrPropertyPage(srn, 1, 1)) mustBe None
        }
      )
    }

    "when method of disposal is Other" in {
      val landOrProperty = LandOrProperty(
        recordVersion = Some("001"),
        optLandOrPropertyHeld = Some(true),
        optDisposeAnyLandOrProperty = Some(true),
        landOrPropertyTransactions = Seq(
          LandOrPropertyTransactions(
            prePopulated = None,
            propertyDetails = PropertyDetails(
              landOrPropertyInUK = true,
              addressDetails = address,
              landRegistryTitleNumberKey = true,
              landRegistryTitleNumberValue = "landRegistryTitleNumberValue"
            ),
            heldPropertyTransaction = HeldPropertyTransaction(
              methodOfHolding = Acquisition,
              dateOfAcquisitionOrContribution = Some(localDate),
              optPropertyAcquiredFromName = Some(individualRecipientName),
              optPropertyAcquiredFrom =
                Some(PropertyAcquiredFrom(IdentityType.Individual, Some(nino.value), None, None)),
              optConnectedPartyStatus = Some(true),
              totalCostOfLandOrProperty = money.value,
              optIndepValuationSupport = Some(false),
              optIsLandOrPropertyResidential = Some(true),
              optLeaseDetails = None,
              optLandOrPropertyLeased = Some(false),
              optTotalIncomeOrReceipts = Some(money.value)
            ),
            optDisposedPropertyTransaction = Some(
              Seq(
                DisposedPropertyTransaction(
                  methodOfDisposal = HowDisposed.Other("some reason").name, // Other
                  optOtherMethod = Some("some reason"),
                  optDateOfSale = None,
                  optNameOfPurchaser = None,
                  optPropertyAcquiredFrom = None,
                  optSaleProceeds = None,
                  optConnectedPartyStatus = None,
                  optIndepValuationSupport = None,
                  portionStillHeld = false
                )
              )
            )
          )
        )
      )

      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, landOrProperty)

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(HowWasPropertyDisposedOfPage(srn, 1, 1)) mustBe Some(HowDisposed.Other("some reason"))
          userAnswers.get(LandOrPropertyStillHeldPage(srn, 1, 1)) mustBe Some(false)
        }
      )
    }
  }

  def landOrPropertyWithEmptyOptionalValues(
    prePopulated: Option[Boolean],
    recordVersion: Option[String],
    leaseAmount: Option[Double] = None
  ): LandOrProperty =
    LandOrProperty(
      recordVersion = recordVersion,
      optLandOrPropertyHeld = Some(true),
      optDisposeAnyLandOrProperty = Some(false),
      landOrPropertyTransactions = Seq(
        LandOrPropertyTransactions(
          prePopulated,
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
            optPropertyAcquiredFrom = Some(PropertyAcquiredFrom(Individual, None, Some("sdf"), Some("sdf"))),
            optConnectedPartyStatus = Some(true),
            totalCostOfLandOrProperty = money.value,
            optIndepValuationSupport = Some(false),
            optIsLandOrPropertyResidential = Some(true),
            optLeaseDetails = Some(
              LeaseDetails(
                optLesseeName = Some("lesseeName"),
                optLeaseGrantDate = Some(localDate),
                optAnnualLeaseAmount = leaseAmount,
                optConnectedPartyStatus = Some(false)
              )
            ),
            optLandOrPropertyLeased = Some(true),
            optTotalIncomeOrReceipts = None
          ),
          optDisposedPropertyTransaction = None
        )
      )
    )

  def buildLandOrProperty(name: String, propertyAcquiredFrom: PropertyAcquiredFrom): LandOrProperty =
    LandOrProperty(
      recordVersion = Some("001"),
      optLandOrPropertyHeld = Some(true),
      optDisposeAnyLandOrProperty = Some(true),
      landOrPropertyTransactions = Seq(
        LandOrPropertyTransactions(
          prePopulated = None,
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
            optIsLandOrPropertyResidential = Some(true),
            optLeaseDetails = Some(
              LeaseDetails(
                optLesseeName = Some("lesseeName"),
                optLeaseGrantDate = Some(localDate),
                optAnnualLeaseAmount = Some(money.value),
                optConnectedPartyStatus = Some(false)
              )
            ),
            optLandOrPropertyLeased = Some(true),
            optTotalIncomeOrReceipts = Some(money.value)
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
