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
import pages.nonsipp.loansmadeoroutstanding.{DatePeriodLoanPage, LoansMadeOrOutstandingPage}
import utils.UserAnswersUtils.UserAnswersOps

import java.time.LocalDate

class IdentityTypePageSpec extends PageBehaviours {
  private val indexOne = refineMV[OneTo5000](1)
  private val indexTwo = refineMV[OneTo5000](2)
  private val indexThree = refineMV[OneTo5000](3)

  private val srn = srnGen.sample.value

  "IdentityTypePage" - {
    IdentitySubject.values.foreach { identitySubject =>
      s"for $identitySubject" - {

        beRetrievable[IdentityType](IdentityTypePage(srn, indexOne, identitySubject))

        beSettable[IdentityType](IdentityTypePage(srn, indexOne, identitySubject))

        beRemovable[IdentityType](IdentityTypePage(srn, indexOne, identitySubject))

        "must be able to set and retrieve multiple" in {
          val ua = UserAnswers("test")
            .unsafeSet(IdentityTypePage(srn, indexOne, identitySubject), IdentityType.Individual)
            .unsafeSet(IdentityTypePage(srn, indexTwo, identitySubject), IdentityType.UKCompany)
            .unsafeSet(IdentityTypePage(srn, indexThree, identitySubject), IdentityType.UKPartnership)

          ua.map(IdentityTypes(srn, identitySubject)).values.toList mustBe List(
            IdentityType.Individual,
            IdentityType.UKCompany,
            IdentityType.UKPartnership
          )
        }

        "cleanup with list size 1" - {
          val localDate: LocalDate = LocalDate.of(1989, 10, 6)
          val userAnswers =
            UserAnswers("id")
              .unsafeSet(DatePeriodLoanPage(srn, indexOne), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
              .unsafeSet(
                CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LoanRecipient),
                ConditionalYesNo.yes[String, Crn](crnGen.sample.value)
              )
              .unsafeSet(
                CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LandOrPropertySeller),
                ConditionalYesNo.yes[String, Crn](crnGen.sample.value)
              )
              .unsafeSet(IdentityTypePage(srn, indexOne, IdentitySubject.LoanRecipient), IdentityType.UKCompany) // part of loans journey
              .unsafeSet(LandPropertyInUKPage(srn, indexOne), true) // part of land or property journey
              .unsafeSet(LoansMadeOrOutstandingPage(srn), true) // part of loans journey

          s"remove dependant loan values when current answer is None (removal) and existing answers are present" in {

            val result = IdentityTypePage(srn, indexOne, IdentitySubject.LoanRecipient)
              .cleanup(None, userAnswers)
              .toOption
              .value

            result.get(DatePeriodLoanPage(srn, indexOne)) mustBe None
            result.get(LandPropertyInUKPage(srn, indexOne)) must not be None
            result.get(CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LoanRecipient)) mustBe None
            result.get(CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LandOrPropertySeller)) must not be None
            result.get(LoansMadeOrOutstandingPage(srn)) mustBe None
          }

          s"remove dependant loan values when current answer is Partnership and existing answer is UKCompany (update)" in {

            val result = IdentityTypePage(srn, indexOne, IdentitySubject.LoanRecipient)
              .cleanup(Some(IdentityType.UKPartnership), userAnswers)
              .toOption
              .value

            result.get(DatePeriodLoanPage(srn, indexOne)) must not be None
            result.get(LandPropertyInUKPage(srn, indexOne)) must not be None
            result.get(CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LoanRecipient)) mustBe None
            result.get(CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LandOrPropertySeller)) must not be None
            result.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(true)
          }
        }

        "cleanup with list size more than 1" - {
          val localDate: LocalDate = LocalDate.of(1989, 10, 6)
          val userAnswers = UserAnswers("id-1")
            .unsafeSet(LandPropertyInUKPage(srn, indexOne), true) // part of land or property journey
            .unsafeSet(LandPropertyInUKPage(srn, indexTwo), true) // part of land or property journey
            .unsafeSet(DatePeriodLoanPage(srn, indexOne), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
            .unsafeSet(DatePeriodLoanPage(srn, indexTwo), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
            .unsafeSet(IdentityTypePage(srn, indexOne, IdentitySubject.LoanRecipient), IdentityType.UKCompany) // part of loans journey
            .unsafeSet(IdentityTypePage(srn, indexTwo, IdentitySubject.LoanRecipient), IdentityType.UKCompany) // part of loans journey
            .unsafeSet(
              CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LoanRecipient),
              ConditionalYesNo.yes[String, Crn](crnGen.sample.value)
            )
            .unsafeSet(
              CompanyRecipientCrnPage(srn, indexTwo, IdentitySubject.LoanRecipient),
              ConditionalYesNo.yes[String, Crn](crnGen.sample.value)
            )
            .unsafeSet(
              CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LandOrPropertySeller),
              ConditionalYesNo.yes[String, Crn](crnGen.sample.value)
            )
            .unsafeSet(
              CompanyRecipientCrnPage(srn, indexTwo, IdentitySubject.LandOrPropertySeller),
              ConditionalYesNo.yes[String, Crn](crnGen.sample.value)
            )
            .unsafeSet(LoansMadeOrOutstandingPage(srn), true) // part of loans journey

          s"remove dependant loan values when current answer is None (removal) and existing answers are present" in {

            val result = IdentityTypePage(srn, indexOne, IdentitySubject.LoanRecipient)
              .cleanup(None, userAnswers)
              .toOption
              .value

            result.get(DatePeriodLoanPage(srn, indexOne)) mustBe None
            result.get(LandPropertyInUKPage(srn, indexOne)) must not be None
            result.get(CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LoanRecipient)) mustBe None
            result.get(CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LandOrPropertySeller)) must not be None

            result.get(DatePeriodLoanPage(srn, indexTwo)) must not be None
            result.get(LandPropertyInUKPage(srn, indexTwo)) must not be None
            result.get(CompanyRecipientCrnPage(srn, indexTwo, IdentitySubject.LoanRecipient)) must not be None
            result.get(CompanyRecipientCrnPage(srn, indexTwo, IdentitySubject.LandOrPropertySeller)) must not be None
            result.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(true)
          }

          s"remove dependant loan values when current answer is Partnership and existing answer is UKCompany (update)" in {

            val result = IdentityTypePage(srn, indexOne, IdentitySubject.LoanRecipient)
              .cleanup(Some(IdentityType.UKPartnership), userAnswers)
              .toOption
              .value

            result.get(DatePeriodLoanPage(srn, indexOne)) must not be None
            result.get(LandPropertyInUKPage(srn, indexOne)) must not be None
            result.get(CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LoanRecipient)) mustBe None
            result.get(CompanyRecipientCrnPage(srn, indexOne, IdentitySubject.LandOrPropertySeller)) must not be None
            result.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(true)
          }
        }
      }
    }
  }
}
