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

package pages.nonsipp.landorproperty

import config.Refined.OneTo5000
import eu.timepit.refined.api.Refined
import eu.timepit.refined.refineMV
import models.{
  ConditionalYesNo,
  Crn,
  IdentitySubject,
  IdentityType,
  Money,
  SchemeHoldLandProperty,
  SchemeId,
  UserAnswers
}
import pages.behaviours.PageBehaviours
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage}
import utils.UserAnswersUtils.UserAnswersOps

import java.time.LocalDate

class LandPropertyInUKPageSpec extends PageBehaviours {

  private val srn: SchemeId.Srn = srnGen.sample.value
  val indexOne: Refined[Int, OneTo5000] = refineMV[OneTo5000](1)
  val indexTwo: Refined[Int, OneTo5000] = refineMV[OneTo5000](2)

  "LandPropertyInUKPage" - {

    val index = refineMV[OneTo5000](1)

    beRetrievable[Boolean](LandPropertyInUKPage(srnGen.sample.value, index))

    beSettable[Boolean](LandPropertyInUKPage(srnGen.sample.value, index))

    beRemovable[Boolean](LandPropertyInUKPage(srnGen.sample.value, index))
  }

  "cleanup with index-1" - {
    val leaseName = "testLeaseName"
    val money: Money = Money(123456)
    val localDate: LocalDate = LocalDate.of(1989, 10, 6)

    val userAnswers =
      UserAnswers("id")
        .unsafeSet(IdentityTypePage(srn, indexOne, IdentitySubject.LandOrPropertySeller), IdentityType.Individual)
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, indexOne), SchemeHoldLandProperty.Acquisition)
        .unsafeSet(
          CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LandOrPropertySeller),
          ConditionalYesNo.yes[String, Crn](crnGen.sample.value)
        )
        .unsafeSet(IsLandPropertyLeasedPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, indexOne), (leaseName, money, localDate))
        .unsafeSet(IsLesseeConnectedPartyPage(srn, indexOne), true)
        .unsafeSet(LandOrPropertyTotalIncomePage(srn, indexOne), money)
        .unsafeSet(LandOrPropertyHeldPage(srn), true)

    s"remove dependant values when current answer is None" in {

      val result = LandPropertyInUKPage(srn, indexOne).cleanup(None, userAnswers).toOption.value

      result.get(LandOrPropertyLeaseDetailsPage(srn, indexOne)) mustBe None
      result.get(IsLesseeConnectedPartyPage(srn, indexOne)) mustBe None
      result.get(IdentityTypePage(srn, indexOne, IdentitySubject.LandOrPropertySeller)) mustBe None
      result.get(CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LandOrPropertySeller)) mustBe None
      result.get(WhyDoesSchemeHoldLandPropertyPage(srn, indexOne)) mustBe None
      result.get(LandOrPropertyTotalIncomePage(srn, indexOne)) mustBe None
      result.get(LandOrPropertyTotalIncomePage(srn, indexOne)) mustBe None
    }

  }

  "cleanup with index bigger than 1" - {
    val leaseName = "testLeaseName"
    val money: Money = Money(123456)
    val localDate: LocalDate = LocalDate.of(1989, 10, 6)

    val userAnswers =
      UserAnswers("id")
        .unsafeSet(IdentityTypePage(srn, indexTwo, IdentitySubject.LandOrPropertySeller), IdentityType.Individual)
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, indexTwo), SchemeHoldLandProperty.Acquisition)
        .unsafeSet(
          CompanyRecipientCrnPage(srn, indexTwo, IdentitySubject.LandOrPropertySeller),
          ConditionalYesNo.yes[String, Crn](crnGen.sample.value)
        )
        .unsafeSet(IsLandPropertyLeasedPage(srn, indexTwo), true)
        .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, indexTwo), (leaseName, money, localDate))
        .unsafeSet(IsLesseeConnectedPartyPage(srn, indexTwo), true)
        .unsafeSet(LandOrPropertyTotalIncomePage(srn, indexTwo), money)
        .unsafeSet(LandOrPropertyHeldPage(srn), true)

    s"remove dependant values when current answer is None" in {

      val result = LandPropertyInUKPage(srn, indexTwo).cleanup(None, userAnswers).toOption.value

      result.get(LandOrPropertyLeaseDetailsPage(srn, indexTwo)) mustBe None
      result.get(IsLesseeConnectedPartyPage(srn, indexTwo)) mustBe None
      result.get(IdentityTypePage(srn, indexTwo, IdentitySubject.LandOrPropertySeller)) mustBe None
      result.get(CompanyRecipientCrnPage(srn, indexTwo, IdentitySubject.LandOrPropertySeller)) mustBe None
      result.get(WhyDoesSchemeHoldLandPropertyPage(srn, indexTwo)) mustBe None
      result.get(LandOrPropertyTotalIncomePage(srn, indexTwo)) mustBe None
      result.get(LandOrPropertyHeldPage(srn)) mustBe Some(true)
    }

  }
}
