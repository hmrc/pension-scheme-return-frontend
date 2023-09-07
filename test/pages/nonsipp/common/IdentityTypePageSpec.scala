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

package pages.nonsipp.common

import config.Refined.OneTo5000
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, Crn, IdentitySubject, IdentityType, Money, UserAnswers}
import pages.behaviours.PageBehaviours
import pages.nonsipp.landorproperty.LandPropertyInUKPage
import pages.nonsipp.loansmadeoroutstanding.DatePeriodLoanPage
import utils.UserAnswersUtils.UserAnswersOps

import java.time.LocalDate

class IdentityTypePageSpec extends PageBehaviours {
  "WhoReceivedLoanPage" - {
    val index = refineMV[OneTo5000](1)
    val identitySubject = IdentitySubject.LoanRecipient

    val srn = srnGen.sample.value

    beRetrievable[IdentityType](IdentityTypePage(srn, index, identitySubject))

    beSettable[IdentityType](IdentityTypePage(srn, index, identitySubject))

    beRemovable[IdentityType](IdentityTypePage(srn, index, identitySubject))

    "must be able to set and retrieve multiple" in {
      val ua = UserAnswers("test")
        .unsafeSet(IdentityTypePage(srn, refineMV[OneTo5000](1), identitySubject), IdentityType.Individual)
        .unsafeSet(IdentityTypePage(srn, refineMV[OneTo5000](2), identitySubject), IdentityType.UKCompany)
        .unsafeSet(IdentityTypePage(srn, refineMV[OneTo5000](3), identitySubject), IdentityType.UKPartnership)

      ua.map(IdentityTypes(srn, IdentitySubject.LoanRecipient)).values.toList mustBe List(
        IdentityType.Individual,
        IdentityType.UKCompany,
        IdentityType.UKPartnership
      )
    }

    "cleanup" - {
      val localDate: LocalDate = LocalDate.of(1989, 10, 6)
      val userAnswers =
        UserAnswers("id")
          .unsafeSet(DatePeriodLoanPage(srn, index), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
          .unsafeSet(CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient), ConditionalYesNo.yes[String, Crn](crnGen.sample.value))
          .unsafeSet(CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller), ConditionalYesNo.yes[String, Crn](crnGen.sample.value))
          .unsafeSet(IdentityTypePage(srn, index, IdentitySubject.LoanRecipient), IdentityType.UKCompany) // part of loans journey
          .unsafeSet(LandPropertyInUKPage(srn, index), true) // part of land or property journey

      s"remove dependant loan values when current answer is None (removal) and existing answers are present" in {

        val result = IdentityTypePage(srn, index, IdentitySubject.LoanRecipient)
          .cleanup(None, userAnswers)
          .toOption
          .value

        result.get(DatePeriodLoanPage(srn, index)) mustBe None
        result.get(LandPropertyInUKPage(srn, index)) must not be None
        result.get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient)) mustBe None
        result.get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller)) must not be None
      }

      s"remove dependant loan values when current answer is Partnership and existing answer is UKCompany (update)" in {

        val result = IdentityTypePage(srn, index, IdentitySubject.LoanRecipient)
          .cleanup(Some(IdentityType.UKPartnership), userAnswers)
          .toOption
          .value

        result.get(DatePeriodLoanPage(srn, index)) must not be None
        result.get(LandPropertyInUKPage(srn, index)) must not be None
        result.get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LoanRecipient)) mustBe None
        result.get(CompanyRecipientCrnPage(srn, index, IdentitySubject.LandOrPropertySeller)) must not be None
      }
    }
  }
}
