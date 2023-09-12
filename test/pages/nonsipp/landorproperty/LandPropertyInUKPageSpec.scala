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
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, Crn, IdentitySubject, IdentityType, Money, SchemeHoldLandProperty, UserAnswers}
import pages.behaviours.PageBehaviours
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage}

import java.time.LocalDate
import utils.UserAnswersUtils.UserAnswersOps

class LandPropertyInUKPageSpec extends PageBehaviours {
  private val srn = srnGen.sample.value
  val index = refineMV[OneTo5000](1)

  "LandPropertyInUKPage" - {

    val index = refineMV[OneTo5000](1)

    beRetrievable[Boolean](LandPropertyInUKPage(srnGen.sample.value, index))

    beSettable[Boolean](LandPropertyInUKPage(srnGen.sample.value, index))

    beRemovable[Boolean](LandPropertyInUKPage(srnGen.sample.value, index))
  }

  "cleanup" - {
    val leaseName = "testLeaseName"
    val money: Money = Money(123456)
    val localDate: LocalDate = LocalDate.of(1989, 10, 6)

    val userAnswers =
      UserAnswers("id")
        .unsafeSet(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller), IdentityType.Individual)
        .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Acquisition)
        .unsafeSet(
          CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller),
          ConditionalYesNo.yes[String, Crn](crnGen.sample.value)
        )
        .unsafeSet(IsLandPropertyLeasedPage(srn, index), true)
        .unsafeSet(LandOrPropertyLeaseDetailsPage(srn, index), (leaseName, money, localDate))
        .unsafeSet(IsLesseeConnectedPartyPage(srn, index), true)
        .unsafeSet(LandOrPropertyTotalIncomePage(srn, index), money)

    s"remove dependant values when current answer is None" in {

      val result = LandPropertyInUKPage(srn, index).cleanup(None, userAnswers).toOption.value

      result.get(LandOrPropertyLeaseDetailsPage(srn, index)) mustBe None
      result.get(IsLesseeConnectedPartyPage(srn, index)) mustBe None
      result.get(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller)) mustBe None
      result.get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller)) mustBe None
      result.get(WhyDoesSchemeHoldLandPropertyPage(srn, index)) mustBe None
      result.get(LandOrPropertyTotalIncomePage(srn, index)) mustBe None
    }

  }
}
