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
import models.ConditionalYesNo._
import models.SponsoringOrConnectedParty.{ConnectedParty, Neither, Sponsoring}
import models.requests.psr._
import models.requests.{AllowedAccessRequest, DataRequest}
import models.{ConditionalYesNo, Crn, IdentitySubject, IdentityType, Money, RecipientDetails, Security, Utr}
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.nonsipp.common.{
  CompanyRecipientCrnPage,
  IdentityTypes,
  OtherRecipientDetailsPage,
  PartnershipRecipientUtrPage
}
import pages.nonsipp.loansmadeoroutstanding._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.domain.Nino
import utils.UserAnswersUtils.UserAnswersOps

class LoanTransactionsTransformerSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new LoanTransactionsTransformer()

  "LoanTransactionsTransformer" - {
    "should return empty List when userAnswer is empty" in {

      val result = transformer.transform(srn)
      result mustBe List.empty
    }

    "should return empty List when index as string not a valid number" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("InvalidIntValue" -> IdentityType.Individual))

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transform(srn)(request)
      result mustBe List.empty
    }

    "should return transformed object when IdentityType is Individual with Nino" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.Individual))
        .unsafeSet(IndividualRecipientNamePage(srn, refineMV(1)), "IndividualRecipientName")
        .unsafeSet(IndividualRecipientNinoPage(srn, refineMV(1)), ConditionalYesNo.yes[String, Nino](nino))
        .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, refineMV(1)), true)
        .unsafeSet(AreRepaymentsInstalmentsPage(srn, refineMV(1)), false)
        .unsafeSet(DatePeriodLoanPage(srn, refineMV(1)), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
        .unsafeSet(AmountOfTheLoanPage(srn, refineMV(1)), (money, money, money))
        .unsafeSet(SecurityGivenForLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Security](security))
        .unsafeSet(InterestOnLoanPage(srn, refineMV(1)), (money, percentage, money))
        .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transform(srn)(request)
      result mustBe List(
        LoanTransactions(
          RecipientIdentityType(IdentityType.Individual, Some(nino.value), None, None),
          "IndividualRecipientName",
          true,
          None,
          LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
          LoanAmountDetails(money.value, money.value, money.value),
          false,
          LoanInterestDetails(money.value, percentage.value, money.value),
          Some(security.value),
          Some(money.value)
        )
      )
    }

    "should return transformed object when IdentityType is Individual without Nino" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.Individual))
        .unsafeSet(IndividualRecipientNamePage(srn, refineMV(1)), "IndividualRecipientName")
        .unsafeSet(IndividualRecipientNinoPage(srn, refineMV(1)), ConditionalYesNo.no[String, Nino]("noNinoReason"))
        .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, refineMV(1)), false)
        .unsafeSet(AreRepaymentsInstalmentsPage(srn, refineMV(1)), true)
        .unsafeSet(DatePeriodLoanPage(srn, refineMV(1)), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
        .unsafeSet(AmountOfTheLoanPage(srn, refineMV(1)), (money, money, money))
        .unsafeSet(SecurityGivenForLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Security](security))
        .unsafeSet(InterestOnLoanPage(srn, refineMV(1)), (money, percentage, money))
        .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transform(srn)(request)
      result mustBe List(
        LoanTransactions(
          RecipientIdentityType(IdentityType.Individual, None, Some("noNinoReason"), None),
          "IndividualRecipientName",
          false,
          None,
          LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
          LoanAmountDetails(money.value, money.value, money.value),
          true,
          LoanInterestDetails(money.value, percentage.value, money.value),
          Some(security.value),
          Some(money.value)
        )
      )
    }

    "should return transformed object when IdentityType is UKCompany with Crn" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.UKCompany))
        .unsafeSet(CompanyRecipientNamePage(srn, refineMV(1)), "CompanyRecipientName")
        .unsafeSet(
          CompanyRecipientCrnPage(srn, refineMV(1), IdentitySubject.LoanRecipient),
          ConditionalYesNo.yes[String, Crn](crn)
        )
        .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, refineMV(1)), Sponsoring)
        .unsafeSet(AreRepaymentsInstalmentsPage(srn, refineMV(1)), false)
        .unsafeSet(DatePeriodLoanPage(srn, refineMV(1)), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
        .unsafeSet(AmountOfTheLoanPage(srn, refineMV(1)), (money, money, money))
        .unsafeSet(SecurityGivenForLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Security](security))
        .unsafeSet(InterestOnLoanPage(srn, refineMV(1)), (money, percentage, money))
        .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transform(srn)(request)
      result mustBe List(
        LoanTransactions(
          RecipientIdentityType(IdentityType.UKCompany, Some(crn.value), None, None),
          "CompanyRecipientName",
          false,
          Some(Sponsoring.name),
          LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
          LoanAmountDetails(money.value, money.value, money.value),
          false,
          LoanInterestDetails(money.value, percentage.value, money.value),
          Some(security.value),
          Some(money.value)
        )
      )
    }

    "should return transformed object when IdentityType is UKCompany without Crn" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.UKCompany))
        .unsafeSet(CompanyRecipientNamePage(srn, refineMV(1)), "CompanyRecipientName")
        .unsafeSet(
          CompanyRecipientCrnPage(srn, refineMV(1), IdentitySubject.LoanRecipient),
          ConditionalYesNo.no[String, Crn]("noCrnReason")
        )
        .unsafeSet(AreRepaymentsInstalmentsPage(srn, refineMV(1)), true)
        .unsafeSet(DatePeriodLoanPage(srn, refineMV(1)), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
        .unsafeSet(AmountOfTheLoanPage(srn, refineMV(1)), (money, money, money))
        .unsafeSet(SecurityGivenForLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Security](security))
        .unsafeSet(InterestOnLoanPage(srn, refineMV(1)), (money, percentage, money))
        .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transform(srn)(request)
      result mustBe List(
        LoanTransactions(
          RecipientIdentityType(IdentityType.UKCompany, None, Some("noCrnReason"), None),
          "CompanyRecipientName",
          false,
          None,
          LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
          LoanAmountDetails(money.value, money.value, money.value),
          true,
          LoanInterestDetails(money.value, percentage.value, money.value),
          Some(security.value),
          Some(money.value)
        )
      )
    }

    "should return transformed object when IdentityType is UKPartnership with Utr" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.UKPartnership))
        .unsafeSet(PartnershipRecipientNamePage(srn, refineMV(1)), "PartnershipRecipientName")
        .unsafeSet(
          PartnershipRecipientUtrPage(srn, refineMV(1), IdentitySubject.LoanRecipient),
          ConditionalYesNo.yes[String, Utr](utr)
        )
        .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, refineMV(1)), ConnectedParty)
        .unsafeSet(AreRepaymentsInstalmentsPage(srn, refineMV(1)), false)
        .unsafeSet(DatePeriodLoanPage(srn, refineMV(1)), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
        .unsafeSet(AmountOfTheLoanPage(srn, refineMV(1)), (money, money, money))
        .unsafeSet(SecurityGivenForLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Security](security))
        .unsafeSet(InterestOnLoanPage(srn, refineMV(1)), (money, percentage, money))
        .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transform(srn)(request)
      result mustBe List(
        LoanTransactions(
          RecipientIdentityType(IdentityType.UKPartnership, Some(utr.value), None, None),
          "PartnershipRecipientName",
          true,
          Some(ConnectedParty.name),
          LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
          LoanAmountDetails(money.value, money.value, money.value),
          false,
          LoanInterestDetails(money.value, percentage.value, money.value),
          Some(security.value),
          Some(money.value)
        )
      )
    }

    "should return transformed object when IdentityType is UKPartnership without Utr" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.UKPartnership))
        .unsafeSet(PartnershipRecipientNamePage(srn, refineMV(1)), "PartnershipRecipientName")
        .unsafeSet(
          PartnershipRecipientUtrPage(srn, refineMV(1), IdentitySubject.LoanRecipient),
          ConditionalYesNo.no[String, Utr]("noUtrReason")
        )
        .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, refineMV(1)), Neither)
        .unsafeSet(AreRepaymentsInstalmentsPage(srn, refineMV(1)), true)
        .unsafeSet(DatePeriodLoanPage(srn, refineMV(1)), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
        .unsafeSet(AmountOfTheLoanPage(srn, refineMV(1)), (money, money, money))
        .unsafeSet(SecurityGivenForLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Security](security))
        .unsafeSet(InterestOnLoanPage(srn, refineMV(1)), (money, percentage, money))
        .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transform(srn)(request)
      result mustBe List(
        LoanTransactions(
          RecipientIdentityType(IdentityType.UKPartnership, None, Some("noUtrReason"), None),
          "PartnershipRecipientName",
          false,
          Some(Neither.name),
          LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
          LoanAmountDetails(money.value, money.value, money.value),
          true,
          LoanInterestDetails(money.value, percentage.value, money.value),
          Some(security.value),
          Some(money.value)
        )
      )
    }

    "should return transformed object when IdentityType is Other" in {
      val userAnswers = defaultUserAnswers
        .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.Other))
        .unsafeSet(
          OtherRecipientDetailsPage(srn, refineMV(1), IdentitySubject.LoanRecipient),
          RecipientDetails("OtherRecipientDetailsName", "otherDescription")
        )
        .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, refineMV(1)), Neither)
        .unsafeSet(AreRepaymentsInstalmentsPage(srn, refineMV(1)), true)
        .unsafeSet(DatePeriodLoanPage(srn, refineMV(1)), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
        .unsafeSet(AmountOfTheLoanPage(srn, refineMV(1)), (money, money, money))
        .unsafeSet(SecurityGivenForLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Security](security))
        .unsafeSet(InterestOnLoanPage(srn, refineMV(1)), (money, percentage, money))
        .unsafeSet(OutstandingArrearsOnLoanPage(srn, refineMV(1)), ConditionalYesNo.yes[Unit, Money](money))

      val request = DataRequest(allowedAccessRequest, userAnswers)

      val result = transformer.transform(srn)(request)
      result mustBe List(
        LoanTransactions(
          RecipientIdentityType(IdentityType.Other, None, None, Some("otherDescription")),
          "OtherRecipientDetailsName",
          false,
          Some(Neither.name),
          LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
          LoanAmountDetails(money.value, money.value, money.value),
          true,
          LoanInterestDetails(money.value, percentage.value, money.value),
          Some(security.value),
          Some(money.value)
        )
      )
    }
  }
}
