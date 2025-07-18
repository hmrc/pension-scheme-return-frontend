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

package pages.nonsipp.landorproperty

import config.RefinedTypes.{OneTo50, OneTo5000}
import utils.IntUtils.given
import pages.nonsipp.landorpropertydisposal.{HowWasPropertyDisposedOfPage, LandOrPropertyDisposalPage}
import utils.UserAnswersUtils.UserAnswersOps
import models._
import viewmodels.models.SectionJourneyStatus
import eu.timepit.refined.api.Refined
import pages.behaviours.PageBehaviours

import java.time.LocalDate

class LandPropertyInUKPageSpec extends PageBehaviours {

  private val srn: SchemeId.Srn = srnGen.sample.value
  val indexOne: Refined[Int, OneTo5000] = 1
  val indexTwo: Refined[Int, OneTo5000] = 2
  val disposalIndexOne: Refined[Int, OneTo50] = 1
  val disposalIndexTwo: Refined[Int, OneTo50] = 2

  "LandPropertyInUKPage" - {

    val index = 1

    beRetrievable[Boolean](LandPropertyInUKPage(srn, index))

    beSettable[Boolean](LandPropertyInUKPage(srn, index))

    beRemovable[Boolean](LandPropertyInUKPage(srn, index))
  }

  "cleanup with list size 1" - {
    val leaseName = "testLeaseName"
    val money: Money = Money(123456)
    val localDate: LocalDate = LocalDate.of(1989, 10, 6)

    val userAnswers =
      UserAnswers("id")
        .unsafeSet(LandPropertyInUKPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertyChosenAddressPage(srn, indexOne), addressGen.sample.value)
        .unsafeSet(LandRegistryTitleNumberPage(srn, indexOne), ConditionalYesNo.yes[String, String]("some-number"))
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, indexOne), SchemeHoldLandProperty.Transfer)
        .unsafeSet(LandOrPropertyTotalCostPage(srn, indexOne), money)
        .unsafeSet(IsLandOrPropertyResidentialPage(srn, indexOne), true)
        .unsafeSet(IsLandPropertyLeasedPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, indexOne), (leaseName, money, localDate))
        .unsafeSet(IsLesseeConnectedPartyPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertyTotalIncomePage(srn, indexOne), money)
        .unsafeSet(RemovePropertyPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertyProgress(srn, indexOne), SectionJourneyStatus.Completed)
        .unsafeSet(LandOrPropertyHeldPage(srn), true)

    s"remove dependant values when current answer is None" in {

      val result = LandPropertyInUKPage(srn, indexOne).cleanup(None, userAnswers).toOption.value

      result.get(LandOrPropertyChosenAddressPage(srn, indexOne)) mustBe None
      result.get(LandRegistryTitleNumberPage(srn, indexOne)) mustBe None
      result.get(WhyDoesSchemeHoldLandPropertyPage(srn, indexOne)) mustBe None
      result.get(LandOrPropertyTotalCostPage(srn, indexOne)) mustBe None
      result.get(IsLandOrPropertyResidentialPage(srn, indexOne)) mustBe None
      result.get(IsLandPropertyLeasedPage(srn, indexOne)) mustBe None
      result.get(LandOrPropertySellerConnectedPartyPage(srn, indexOne)) mustBe None
      result.get(LandOrPropertyLeaseDetailsPage(srn, indexOne)) mustBe None
      result.get(IsLesseeConnectedPartyPage(srn, indexOne)) mustBe None
      result.get(LandOrPropertyTotalIncomePage(srn, indexOne)) mustBe None
      result.get(RemovePropertyPage(srn, indexOne)) mustBe None
      result.get(LandOrPropertyProgress(srn, indexOne)) mustBe None
      result.get(LandOrPropertyHeldPage(srn)) mustBe None
    }

  }

  "cleanup with list size more than 1" - {
    val leaseName = "testLeaseName"
    val money: Money = Money(123456)
    val localDate: LocalDate = LocalDate.of(1989, 10, 6)

    val userAnswers =
      UserAnswers("id")
        .unsafeSet(LandPropertyInUKPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertyChosenAddressPage(srn, indexOne), addressGen.sample.value)
        .unsafeSet(LandRegistryTitleNumberPage(srn, indexOne), ConditionalYesNo.yes[String, String]("some-number"))
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, indexOne), SchemeHoldLandProperty.Transfer)
        .unsafeSet(LandOrPropertyTotalCostPage(srn, indexOne), money)
        .unsafeSet(IsLandOrPropertyResidentialPage(srn, indexOne), true)
        .unsafeSet(IsLandPropertyLeasedPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, indexOne), (leaseName, money, localDate))
        .unsafeSet(IsLesseeConnectedPartyPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertyTotalIncomePage(srn, indexOne), money)
        .unsafeSet(LandOrPropertyProgress(srn, indexOne), SectionJourneyStatus.Completed)
        .unsafeSet(RemovePropertyPage(srn, indexOne), true)
        .unsafeSet(LandPropertyInUKPage(srn, indexTwo), true)
        .unsafeSet(LandOrPropertyChosenAddressPage(srn, indexTwo), addressGen.sample.value)
        .unsafeSet(LandRegistryTitleNumberPage(srn, indexTwo), ConditionalYesNo.yes[String, String]("some-number"))
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, indexTwo), SchemeHoldLandProperty.Transfer)
        .unsafeSet(LandOrPropertyTotalCostPage(srn, indexTwo), money)
        .unsafeSet(IsLandOrPropertyResidentialPage(srn, indexTwo), true)
        .unsafeSet(IsLandPropertyLeasedPage(srn, indexTwo), true)
        .unsafeSet(LandOrPropertySellerConnectedPartyPage(srn, indexTwo), true)
        .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, indexTwo), (leaseName, money, localDate))
        .unsafeSet(IsLesseeConnectedPartyPage(srn, indexTwo), true)
        .unsafeSet(LandOrPropertyTotalIncomePage(srn, indexTwo), money)
        .unsafeSet(LandOrPropertyProgress(srn, indexTwo), SectionJourneyStatus.Completed)
        .unsafeSet(RemovePropertyPage(srn, indexTwo), true)
        .unsafeSet(LandOrPropertyHeldPage(srn), true)
        // dependant disposal data:
        .unsafeSet(LandOrPropertyDisposalPage(srn), true)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, indexOne, disposalIndexOne), HowDisposed.Sold)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, indexOne, disposalIndexTwo), HowDisposed.Sold)
        .unsafeSet(HowWasPropertyDisposedOfPage(srn, indexTwo, disposalIndexOne), HowDisposed.Sold)

    s"remove dependant values when current answer is None" in {

      val result = LandPropertyInUKPage(srn, indexOne).cleanup(None, userAnswers).toOption.value

      result.get(LandOrPropertyChosenAddressPage(srn, indexOne)) mustBe None
      result.get(LandRegistryTitleNumberPage(srn, indexOne)) mustBe None
      result.get(WhyDoesSchemeHoldLandPropertyPage(srn, indexOne)) mustBe None
      result.get(LandOrPropertyTotalCostPage(srn, indexOne)) mustBe None
      result.get(IsLandOrPropertyResidentialPage(srn, indexOne)) mustBe None
      result.get(IsLandPropertyLeasedPage(srn, indexOne)) mustBe None
      result.get(LandOrPropertySellerConnectedPartyPage(srn, indexOne)) mustBe None
      result.get(LandOrPropertyLeaseDetailsPage(srn, indexOne)) mustBe None
      result.get(IsLesseeConnectedPartyPage(srn, indexOne)) mustBe None
      result.get(LandOrPropertyTotalIncomePage(srn, indexOne)) mustBe None
      result.get(LandOrPropertyProgress(srn, indexOne)) mustBe None
      result.get(RemovePropertyPage(srn, indexOne)) mustBe None

      result.get(LandOrPropertyChosenAddressPage(srn, indexTwo)) must not be None
      result.get(LandRegistryTitleNumberPage(srn, indexTwo)) must not be None
      result.get(WhyDoesSchemeHoldLandPropertyPage(srn, indexTwo)) must not be None
      result.get(LandOrPropertyTotalCostPage(srn, indexTwo)) must not be None
      result.get(IsLandOrPropertyResidentialPage(srn, indexTwo)) must not be None
      result.get(IsLandPropertyLeasedPage(srn, indexTwo)) must not be None
      result.get(LandOrPropertySellerConnectedPartyPage(srn, indexTwo)) must not be None
      result.get(LandOrPropertyLeaseDetailsPage(srn, indexTwo)) must not be None
      result.get(IsLesseeConnectedPartyPage(srn, indexTwo)) must not be None
      result.get(LandOrPropertyTotalIncomePage(srn, indexTwo)) must not be None
      result.get(LandOrPropertyProgress(srn, indexTwo)) must not be None
      result.get(RemovePropertyPage(srn, indexTwo)) must not be None

      result.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
    }

    s"remove dependant disposal values when current answer is None" in {

      val result = LandPropertyInUKPage(srn, indexOne).cleanup(None, userAnswers).toOption.value

      result.get(LandOrPropertyDisposalPage(srn)) mustBe Some(true)
      result.get(HowWasPropertyDisposedOfPage(srn, indexOne, disposalIndexOne)) mustBe None
      result.get(HowWasPropertyDisposedOfPage(srn, indexOne, disposalIndexTwo)) mustBe None
      result.get(HowWasPropertyDisposedOfPage(srn, indexTwo, disposalIndexOne)) must not be None
      result.get(LandOrPropertyDisposalPage(srn)) must not be None

    }
  }
}
