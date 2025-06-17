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
import play.api.mvc.AnyContentAsEmpty
import models.IdentityType.Individual
import controllers.TestValues
import utils.IntUtils.given
import utils.UserAnswersUtils.UserAnswersOps
import generators.ModelGenerators.allowedAccessRequestGen
import models._
import pages.nonsipp.common._
import pages.nonsipp.loansmadeoroutstanding._
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import org.scalatest.matchers.must.Matchers
import models.ConditionalYesNo._
import models.requests.psr._
import config.Constants.PREPOPULATION_FLAG
import org.scalatest.OptionValues
import uk.gov.hmrc.domain.Nino
import models.SponsoringOrConnectedParty.{ConnectedParty, Neither, Sponsoring}
import models.requests.{AllowedAccessRequest, DataRequest}

class LoansTransformerSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] =
    allowedAccessRequestGen(FakeRequest()).sample.value

  val allowedAccessRequestPrePopulation: AllowedAccessRequest[AnyContentAsEmpty.type] =
    allowedAccessRequestGen(FakeRequest().withSession((PREPOPULATION_FLAG, "true"))).sample.value

  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new LoansTransformer()

  "Transform to ETMP" - {
    "should return None when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn, emptyUserAnswers)
      result mustBe None
    }

    "should omit Record Version when there is a change in userAnswers" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(LoansRecordVersionPage(srn), "001")
        .unsafeSet(LoansMadeOrOutstandingPage(srn), false)

      val initialUA = emptyUserAnswers
        .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
        .unsafeSet(LoansRecordVersionPage(srn), "001")

      val result = transformer.transformToEtmp(srn, initialUA)(DataRequest(allowedAccessRequest, userAnswers))
      result mustBe Some(Loans(recordVersion = None, optSchemeHadLoans = Some(false), loanTransactions = Seq.empty))
    }

    "should return recordVersion when there is no change among UAs" - {

      "should return empty loanTransactions when index as string not a valid number" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(LoansRecordVersionPage(srn), "001")
          .unsafeSet(
            IdentityTypes(srn, IdentitySubject.LoanRecipient),
            Map("InvalidIntValue" -> IdentityType.Individual)
          )

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, userAnswers)(request)
        result mustBe Some(
          Loans(recordVersion = Some("001"), optSchemeHadLoans = Some(true), loanTransactions = Seq.empty)
        )
      }

      "should return transformed object when IdentityType is Individual with Nino" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(LoansRecordVersionPage(srn), "001")
          .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.Individual))
          .unsafeSet(IndividualRecipientNamePage(srn, 1), "IndividualRecipientName")
          .unsafeSet(IndividualRecipientNinoPage(srn, 1), ConditionalYesNo.yes[String, Nino](nino))
          .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, 1), true)
          .unsafeSet(AreRepaymentsInstalmentsPage(srn, 1), false)
          .unsafeSet(DatePeriodLoanPage(srn, 1), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
          .unsafeSet(AmountOfTheLoanPage(srn, 1), amountOfTheLoan)
          .unsafeSet(SecurityGivenForLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Security](security))
          .unsafeSet(InterestOnLoanPage(srn, 1), interestOnLoan)
          .unsafeSet(ArrearsPrevYears(srn, 1), true)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Money](money))
          .unsafeSet(LoanCompleted(srn, 1), SectionCompleted)
          .unsafeSet(LoansProgress(srn, 1), SectionJourneyStatus.Completed)

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, userAnswers)(request)
        result mustBe Some(
          Loans(
            recordVersion = Some("001"),
            optSchemeHadLoans = Some(true),
            loanTransactions = List(
              LoanTransactions(
                prePopulated = None,
                recipientIdentityType = RecipientIdentityType(IdentityType.Individual, Some(nino.value), None, None),
                loanRecipientName = "IndividualRecipientName",
                connectedPartyStatus = true,
                optRecipientSponsoringEmployer = None,
                datePeriodLoanDetails = LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
                loanAmountDetails = LoanAmountDetails(
                  amountOfTheLoan.loanAmount.value,
                  amountOfTheLoan.optCapRepaymentCY.map(_.value),
                  amountOfTheLoan.optAmountOutstanding.map(_.value)
                ),
                equalInstallments = false,
                loanInterestDetails = LoanInterestDetails(
                  interestOnLoan.loanInterestAmount.value,
                  interestOnLoan.loanInterestRate.value,
                  interestOnLoan.optIntReceivedCY.map(_.value)
                ),
                optSecurityGivenDetails = Some(security.value),
                optArrearsPrevYears = Some(true),
                optOutstandingArrearsOnLoan = Some(money.value)
              )
            )
          )
        )
      }

      "should return transformed object when IdentityType is Individual without Nino" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(LoansRecordVersionPage(srn), "001")
          .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.Individual))
          .unsafeSet(IndividualRecipientNamePage(srn, 1), "IndividualRecipientName")
          .unsafeSet(IndividualRecipientNinoPage(srn, 1), ConditionalYesNo.no[String, Nino]("noNinoReason"))
          .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, 1), false)
          .unsafeSet(AreRepaymentsInstalmentsPage(srn, 1), true)
          .unsafeSet(DatePeriodLoanPage(srn, 1), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
          .unsafeSet(AmountOfTheLoanPage(srn, 1), amountOfTheLoan)
          .unsafeSet(SecurityGivenForLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Security](security))
          .unsafeSet(InterestOnLoanPage(srn, 1), interestOnLoan)
          .unsafeSet(ArrearsPrevYears(srn, 1), true)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Money](money))
          .unsafeSet(LoanCompleted(srn, 1), SectionCompleted)
          .unsafeSet(LoansProgress(srn, 1), SectionJourneyStatus.Completed)

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, userAnswers)(request)
        result mustBe Some(
          Loans(
            recordVersion = Some("001"),
            optSchemeHadLoans = Some(true),
            loanTransactions = List(
              LoanTransactions(
                prePopulated = None,
                recipientIdentityType =
                  RecipientIdentityType(IdentityType.Individual, None, Some("noNinoReason"), None),
                loanRecipientName = "IndividualRecipientName",
                connectedPartyStatus = false,
                optRecipientSponsoringEmployer = None,
                datePeriodLoanDetails = LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
                loanAmountDetails = LoanAmountDetails(
                  amountOfTheLoan.loanAmount.value,
                  amountOfTheLoan.optCapRepaymentCY.map(_.value),
                  amountOfTheLoan.optAmountOutstanding.map(_.value)
                ),
                equalInstallments = true,
                loanInterestDetails = LoanInterestDetails(
                  interestOnLoan.loanInterestAmount.value,
                  interestOnLoan.loanInterestRate.value,
                  interestOnLoan.optIntReceivedCY.map(_.value)
                ),
                optSecurityGivenDetails = Some(security.value),
                optArrearsPrevYears = Some(true),
                optOutstandingArrearsOnLoan = Some(money.value)
              )
            )
          )
        )
      }

      "should return transformed object when IdentityType is UKCompany with Crn" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(LoansRecordVersionPage(srn), "001")
          .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.UKCompany))
          .unsafeSet(CompanyRecipientNamePage(srn, 1), "CompanyRecipientName")
          .unsafeSet(
            CompanyRecipientCrnPage(srn, 1, IdentitySubject.LoanRecipient),
            ConditionalYesNo.yes[String, Crn](crn)
          )
          .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, 1), Sponsoring)
          .unsafeSet(AreRepaymentsInstalmentsPage(srn, 1), false)
          .unsafeSet(DatePeriodLoanPage(srn, 1), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
          .unsafeSet(AmountOfTheLoanPage(srn, 1), amountOfTheLoan)
          .unsafeSet(SecurityGivenForLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Security](security))
          .unsafeSet(InterestOnLoanPage(srn, 1), interestOnLoan)
          .unsafeSet(ArrearsPrevYears(srn, 1), true)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Money](money))
          .unsafeSet(LoanCompleted(srn, 1), SectionCompleted)
          .unsafeSet(LoansProgress(srn, 1), SectionJourneyStatus.Completed)

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, userAnswers)(request)
        result mustBe Some(
          Loans(
            recordVersion = Some("001"),
            optSchemeHadLoans = Some(true),
            loanTransactions = List(
              LoanTransactions(
                prePopulated = None,
                recipientIdentityType = RecipientIdentityType(IdentityType.UKCompany, Some(crn.value), None, None),
                loanRecipientName = "CompanyRecipientName",
                connectedPartyStatus = false,
                optRecipientSponsoringEmployer = Some(Sponsoring.name),
                datePeriodLoanDetails = LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
                loanAmountDetails = LoanAmountDetails(
                  amountOfTheLoan.loanAmount.value,
                  amountOfTheLoan.optCapRepaymentCY.map(_.value),
                  amountOfTheLoan.optAmountOutstanding.map(_.value)
                ),
                equalInstallments = false,
                loanInterestDetails = LoanInterestDetails(
                  interestOnLoan.loanInterestAmount.value,
                  interestOnLoan.loanInterestRate.value,
                  interestOnLoan.optIntReceivedCY.map(_.value)
                ),
                optSecurityGivenDetails = Some(security.value),
                optArrearsPrevYears = Some(true),
                optOutstandingArrearsOnLoan = Some(money.value)
              )
            )
          )
        )
      }

      "should return transformed object when IdentityType is UKCompany without Crn" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(LoansRecordVersionPage(srn), "001")
          .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.UKCompany))
          .unsafeSet(CompanyRecipientNamePage(srn, 1), "CompanyRecipientName")
          .unsafeSet(
            CompanyRecipientCrnPage(srn, 1, IdentitySubject.LoanRecipient),
            ConditionalYesNo.no[String, Crn]("noCrnReason")
          )
          .unsafeSet(AreRepaymentsInstalmentsPage(srn, 1), true)
          .unsafeSet(DatePeriodLoanPage(srn, 1), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
          .unsafeSet(AmountOfTheLoanPage(srn, 1), amountOfTheLoan)
          .unsafeSet(SecurityGivenForLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Security](security))
          .unsafeSet(InterestOnLoanPage(srn, 1), interestOnLoan)
          .unsafeSet(ArrearsPrevYears(srn, 1), true)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Money](money))
          .unsafeSet(LoanCompleted(srn, 1), SectionCompleted)
          .unsafeSet(LoansProgress(srn, 1), SectionJourneyStatus.Completed)

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, userAnswers)(request)
        result mustBe Some(
          Loans(
            recordVersion = Some("001"),
            optSchemeHadLoans = Some(true),
            loanTransactions = List(
              LoanTransactions(
                prePopulated = None,
                recipientIdentityType = RecipientIdentityType(IdentityType.UKCompany, None, Some("noCrnReason"), None),
                loanRecipientName = "CompanyRecipientName",
                connectedPartyStatus = false,
                optRecipientSponsoringEmployer = None,
                datePeriodLoanDetails = LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
                loanAmountDetails = LoanAmountDetails(
                  amountOfTheLoan.loanAmount.value,
                  amountOfTheLoan.optCapRepaymentCY.map(_.value),
                  amountOfTheLoan.optAmountOutstanding.map(_.value)
                ),
                equalInstallments = true,
                loanInterestDetails = LoanInterestDetails(
                  interestOnLoan.loanInterestAmount.value,
                  interestOnLoan.loanInterestRate.value,
                  interestOnLoan.optIntReceivedCY.map(_.value)
                ),
                optSecurityGivenDetails = Some(security.value),
                optArrearsPrevYears = Some(true),
                optOutstandingArrearsOnLoan = Some(money.value)
              )
            )
          )
        )
      }

      "should return transformed object when IdentityType is UKPartnership with Utr" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(LoansRecordVersionPage(srn), "001")
          .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.UKPartnership))
          .unsafeSet(PartnershipRecipientNamePage(srn, 1), "PartnershipRecipientName")
          .unsafeSet(
            PartnershipRecipientUtrPage(srn, 1, IdentitySubject.LoanRecipient),
            ConditionalYesNo.yes[String, Utr](utr)
          )
          .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, 1), ConnectedParty)
          .unsafeSet(AreRepaymentsInstalmentsPage(srn, 1), false)
          .unsafeSet(DatePeriodLoanPage(srn, 1), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
          .unsafeSet(AmountOfTheLoanPage(srn, 1), amountOfTheLoan)
          .unsafeSet(SecurityGivenForLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Security](security))
          .unsafeSet(InterestOnLoanPage(srn, 1), interestOnLoan)
          .unsafeSet(ArrearsPrevYears(srn, 1), true)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Money](money))
          .unsafeSet(LoanCompleted(srn, 1), SectionCompleted)
          .unsafeSet(LoansProgress(srn, 1), SectionJourneyStatus.Completed)

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, userAnswers)(request)
        result mustBe Some(
          Loans(
            recordVersion = Some("001"),
            optSchemeHadLoans = Some(true),
            loanTransactions = List(
              LoanTransactions(
                prePopulated = None,
                recipientIdentityType = RecipientIdentityType(IdentityType.UKPartnership, Some(utr.value), None, None),
                loanRecipientName = "PartnershipRecipientName",
                connectedPartyStatus = true,
                optRecipientSponsoringEmployer = Some(ConnectedParty.name),
                datePeriodLoanDetails = LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
                loanAmountDetails = LoanAmountDetails(
                  amountOfTheLoan.loanAmount.value,
                  amountOfTheLoan.optCapRepaymentCY.map(_.value),
                  amountOfTheLoan.optAmountOutstanding.map(_.value)
                ),
                equalInstallments = false,
                loanInterestDetails = LoanInterestDetails(
                  interestOnLoan.loanInterestAmount.value,
                  interestOnLoan.loanInterestRate.value,
                  interestOnLoan.optIntReceivedCY.map(_.value)
                ),
                optSecurityGivenDetails = Some(security.value),
                optArrearsPrevYears = Some(true),
                optOutstandingArrearsOnLoan = Some(money.value)
              )
            )
          )
        )
      }

      "should return transformed object when IdentityType is UKPartnership without Utr" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(LoansRecordVersionPage(srn), "001")
          .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.UKPartnership))
          .unsafeSet(PartnershipRecipientNamePage(srn, 1), "PartnershipRecipientName")
          .unsafeSet(
            PartnershipRecipientUtrPage(srn, 1, IdentitySubject.LoanRecipient),
            ConditionalYesNo.no[String, Utr]("noUtrReason")
          )
          .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, 1), Neither)
          .unsafeSet(AreRepaymentsInstalmentsPage(srn, 1), true)
          .unsafeSet(DatePeriodLoanPage(srn, 1), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
          .unsafeSet(AmountOfTheLoanPage(srn, 1), amountOfTheLoan)
          .unsafeSet(SecurityGivenForLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Security](security))
          .unsafeSet(InterestOnLoanPage(srn, 1), interestOnLoan)
          .unsafeSet(ArrearsPrevYears(srn, 1), true)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Money](money))
          .unsafeSet(LoanCompleted(srn, 1), SectionCompleted)
          .unsafeSet(LoansProgress(srn, 1), SectionJourneyStatus.Completed)

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, userAnswers)(request)
        result mustBe Some(
          Loans(
            recordVersion = Some("001"),
            optSchemeHadLoans = Some(true),
            loanTransactions = List(
              LoanTransactions(
                prePopulated = None,
                recipientIdentityType =
                  RecipientIdentityType(IdentityType.UKPartnership, None, Some("noUtrReason"), None),
                loanRecipientName = "PartnershipRecipientName",
                connectedPartyStatus = false,
                optRecipientSponsoringEmployer = Some(Neither.name),
                datePeriodLoanDetails = LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
                loanAmountDetails = LoanAmountDetails(
                  amountOfTheLoan.loanAmount.value,
                  amountOfTheLoan.optCapRepaymentCY.map(_.value),
                  amountOfTheLoan.optAmountOutstanding.map(_.value)
                ),
                equalInstallments = true,
                loanInterestDetails = LoanInterestDetails(
                  interestOnLoan.loanInterestAmount.value,
                  interestOnLoan.loanInterestRate.value,
                  interestOnLoan.optIntReceivedCY.map(_.value)
                ),
                optSecurityGivenDetails = Some(security.value),
                optArrearsPrevYears = Some(true),
                optOutstandingArrearsOnLoan = Some(money.value)
              )
            )
          )
        )
      }

      "should return transformed object when IdentityType is Other" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(LoansMadeOrOutstandingPage(srn), true)
          .unsafeSet(LoansRecordVersionPage(srn), "001")
          .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.Other))
          .unsafeSet(
            OtherRecipientDetailsPage(srn, 1, IdentitySubject.LoanRecipient),
            RecipientDetails("OtherRecipientDetailsName", "otherDescription")
          )
          .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, 1), Neither)
          .unsafeSet(AreRepaymentsInstalmentsPage(srn, 1), true)
          .unsafeSet(DatePeriodLoanPage(srn, 1), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
          .unsafeSet(AmountOfTheLoanPage(srn, 1), amountOfTheLoan)
          .unsafeSet(SecurityGivenForLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Security](security))
          .unsafeSet(InterestOnLoanPage(srn, 1), interestOnLoan)
          .unsafeSet(ArrearsPrevYears(srn, 1), false)
          .unsafeSet(OutstandingArrearsOnLoanPage(srn, 1), ConditionalYesNo.no[Unit, Money](()))
          .unsafeSet(LoanCompleted(srn, 1), SectionCompleted)
          .unsafeSet(LoansProgress(srn, 1), SectionJourneyStatus.Completed)

        val request = DataRequest(allowedAccessRequest, userAnswers)

        val result = transformer.transformToEtmp(srn, userAnswers)(request)
        result mustBe Some(
          Loans(
            recordVersion = Some("001"),
            optSchemeHadLoans = Some(true),
            loanTransactions = List(
              LoanTransactions(
                prePopulated = None,
                recipientIdentityType = RecipientIdentityType(IdentityType.Other, None, None, Some("otherDescription")),
                loanRecipientName = "OtherRecipientDetailsName",
                connectedPartyStatus = false,
                optRecipientSponsoringEmployer = Some(Neither.name),
                datePeriodLoanDetails = LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
                loanAmountDetails = LoanAmountDetails(
                  amountOfTheLoan.loanAmount.value,
                  amountOfTheLoan.optCapRepaymentCY.map(_.value),
                  amountOfTheLoan.optAmountOutstanding.map(_.value)
                ),
                equalInstallments = true,
                loanInterestDetails = LoanInterestDetails(
                  interestOnLoan.loanInterestAmount.value,
                  interestOnLoan.loanInterestRate.value,
                  interestOnLoan.optIntReceivedCY.map(_.value)
                ),
                optSecurityGivenDetails = Some(security.value),
                optArrearsPrevYears = Some(false),
                optOutstandingArrearsOnLoan = None
              )
            )
          )
        )
      }

      "should return transformed object when optional pre-pop fields are missing" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(LoansRecordVersionPage(srn), "001")
          .unsafeSet(IdentityTypes(srn, IdentitySubject.LoanRecipient), Map("0" -> IdentityType.Other))
          .unsafeSet(
            OtherRecipientDetailsPage(srn, 1, IdentitySubject.LoanRecipient),
            RecipientDetails("OtherRecipientDetailsName", "otherDescription")
          )
          .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, 1), Neither)
          .unsafeSet(AreRepaymentsInstalmentsPage(srn, 1), true)
          .unsafeSet(DatePeriodLoanPage(srn, 1), (localDate, Money(Double.MinPositiveValue), Int.MaxValue))
          .unsafeSet(AmountOfTheLoanPage(srn, 1), partialAmountOfTheLoan)
          .unsafeSet(SecurityGivenForLoanPage(srn, 1), ConditionalYesNo.yes[Unit, Security](security))
          .unsafeSet(InterestOnLoanPage(srn, 1), partialInterestOnLoan)
          .unsafeSet(LoansProgress(srn, 1), SectionJourneyStatus.Completed)

        val request = DataRequest(allowedAccessRequestPrePopulation, userAnswers)

        val result = transformer.transformToEtmp(srn, userAnswers)(request)
        result mustBe Some(
          Loans(
            recordVersion = Some("001"),
            optSchemeHadLoans = None,
            loanTransactions = List(
              LoanTransactions(
                prePopulated = None,
                recipientIdentityType = RecipientIdentityType(IdentityType.Other, None, None, Some("otherDescription")),
                loanRecipientName = "OtherRecipientDetailsName",
                connectedPartyStatus = false,
                optRecipientSponsoringEmployer = Some(Neither.name),
                datePeriodLoanDetails = LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
                loanAmountDetails = LoanAmountDetails(
                  partialAmountOfTheLoan.loanAmount.value,
                  None,
                  None
                ),
                equalInstallments = true,
                loanInterestDetails = LoanInterestDetails(
                  partialInterestOnLoan.loanInterestAmount.value,
                  partialInterestOnLoan.loanInterestRate.value,
                  None
                ),
                optSecurityGivenDetails = Some(security.value),
                optArrearsPrevYears = None,
                optOutstandingArrearsOnLoan = None
              )
            )
          )
        )
      }
    }
  }

  "Should transform loan details from ETMP" - {

    "when schemeHadLoans is false" in {

      val userAnswers = emptyUserAnswers

      val result = transformer.transformFromEtmp(
        userAnswers,
        allowedAccessRequest.srn,
        Loans(recordVersion = Some("001"), optSchemeHadLoans = Some(false), loanTransactions = List.empty)
      )

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LoansRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(false)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient)) mustBe None
          userAnswers.get(IndividualRecipientNamePage(srn, 1)) mustBe None
          userAnswers.get(IndividualRecipientNinoPage(srn, 1)) mustBe None
          userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, 1)) mustBe None
          userAnswers.get(DatePeriodLoanPage(srn, 1)) mustBe None
          userAnswers.get(AmountOfTheLoanPage(srn, 1)) mustBe None
          userAnswers.get(AreRepaymentsInstalmentsPage(srn, 1)) mustBe None
          userAnswers.get(InterestOnLoanPage(srn, 1)) mustBe None
          userAnswers.get(SecurityGivenForLoanPage(srn, 1)) mustBe None
          userAnswers.get(OutstandingArrearsOnLoanPage(srn, 1)) mustBe None
          userAnswers.get(LoanCompleted(srn, 1)) mustBe None
          userAnswers.get(LoansProgress(srn, 1)) mustBe None
        }
      )
    }

    "when individual nino" in {

      val userAnswers = emptyUserAnswers

      val result = transformer.transformFromEtmp(
        userAnswers,
        allowedAccessRequest.srn,
        loans(individualRecipientName, RecipientIdentityType(IdentityType.Individual, Some(nino.value), None, None))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LoansRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(true)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(IndividualRecipientNamePage(srn, 1)) mustBe Some(individualRecipientName)
          userAnswers.get(IndividualRecipientNinoPage(srn, 1)) mustBe Some(ConditionalYesNo.yes(nino))
          userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, 1)) mustBe Some(true)
          userAnswers.get(DatePeriodLoanPage(srn, 1)) mustBe Some(
            (localDate, Money(Double.MinPositiveValue), Int.MaxValue)
          )
          userAnswers.get(AmountOfTheLoanPage(srn, 1)) mustBe Some(amountOfTheLoan)
          userAnswers.get(AreRepaymentsInstalmentsPage(srn, 1)) mustBe Some(false)
          userAnswers.get(InterestOnLoanPage(srn, 1)) mustBe Some(interestOnLoan)
          userAnswers.get(SecurityGivenForLoanPage(srn, 1)) mustBe Some(ConditionalYesNo.yes(security))
          userAnswers.get(OutstandingArrearsOnLoanPage(srn, 1)) mustBe Some(ConditionalYesNo.yes(money))
          userAnswers.get(LoanCompleted(srn, 1)) mustBe Some(SectionCompleted)
          userAnswers.get(LoansProgress(srn, 1)) mustBe Some(SectionJourneyStatus.Completed)
        }
      )
    }

    "when individual no nino" in {

      val userAnswers = emptyUserAnswers

      val result = transformer.transformFromEtmp(
        userAnswers,
        allowedAccessRequest.srn,
        loans(individualRecipientName, RecipientIdentityType(IdentityType.Individual, None, Some(noninoReason), None))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LoansRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(true)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(IndividualRecipientNamePage(srn, 1)) mustBe Some(individualRecipientName)
          userAnswers.get(IndividualRecipientNinoPage(srn, 1)) mustBe Some(ConditionalYesNo.no(noninoReason))
          userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, 1)) mustBe Some(true)
        }
      )
    }

    "when company crn" in {

      val userAnswers = emptyUserAnswers

      val result = transformer.transformFromEtmp(
        userAnswers,
        allowedAccessRequest.srn,
        loans(
          companyRecipientName,
          RecipientIdentityType(IdentityType.UKCompany, Some(crn.value), None, None),
          sponsoringEmployer = true
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LoansRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(true)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            IdentityType.UKCompany
          )
          userAnswers.get(CompanyRecipientNamePage(srn, 1)) mustBe Some(companyRecipientName)
          userAnswers.get(CompanyRecipientCrnPage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            ConditionalYesNo.yes(crn)
          )
          userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, 1)) mustBe None
          userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, 1)) mustBe Some(
            SponsoringOrConnectedParty.Sponsoring
          )
        }
      )
    }

    "when company no crn" in {

      val userAnswers = emptyUserAnswers

      val result = transformer.transformFromEtmp(
        userAnswers,
        allowedAccessRequest.srn,
        loans(companyRecipientName, RecipientIdentityType(IdentityType.UKCompany, None, Some(noCrnReason), None))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LoansRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(true)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            IdentityType.UKCompany
          )
          userAnswers.get(CompanyRecipientNamePage(srn, 1)) mustBe Some(companyRecipientName)
          userAnswers.get(CompanyRecipientCrnPage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            ConditionalYesNo.no(noCrnReason)
          )
          userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, 1)) mustBe None
          userAnswers.get(RecipientSponsoringEmployerConnectedPartyPage(srn, 1)) mustBe Some(
            SponsoringOrConnectedParty.ConnectedParty
          )
        }
      )
    }

    "when partnership utr" in {

      val userAnswers = emptyUserAnswers

      val result = transformer.transformFromEtmp(
        userAnswers,
        allowedAccessRequest.srn,
        loans(partnershipRecipientName, RecipientIdentityType(IdentityType.UKPartnership, Some(utr.value), None, None))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LoansRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(true)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            IdentityType.UKPartnership
          )
          userAnswers.get(PartnershipRecipientNamePage(srn, 1)) mustBe Some(partnershipRecipientName)
          userAnswers.get(PartnershipRecipientUtrPage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            ConditionalYesNo.yes(utr)
          )
          userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, 1)) mustBe None
        }
      )
    }

    "when partnership no utr" in {

      val userAnswers = emptyUserAnswers

      val result = transformer.transformFromEtmp(
        userAnswers,
        allowedAccessRequest.srn,
        loans(
          "partnership " + recipientName,
          RecipientIdentityType(IdentityType.UKPartnership, None, Some(noUtrReason), None)
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LoansRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(true)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            IdentityType.UKPartnership
          )
          userAnswers.get(PartnershipRecipientNamePage(srn, 1)) mustBe Some(partnershipRecipientName)
          userAnswers.get(PartnershipRecipientUtrPage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            ConditionalYesNo.no(noUtrReason)
          )
          userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, 1)) mustBe None
        }
      )
    }

    "when other" in {

      val userAnswers = emptyUserAnswers

      val result = transformer.transformFromEtmp(
        userAnswers,
        allowedAccessRequest.srn,
        loans(
          otherRecipientName,
          RecipientIdentityType(IdentityType.Other, None, None, Some(otherRecipientDescription))
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LoansRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(LoansMadeOrOutstandingPage(srn)) mustBe Some(true)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            IdentityType.Other
          )
          userAnswers.get(OtherRecipientDetailsPage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            RecipientDetails(otherRecipientName, otherRecipientDescription)
          )
          userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, 1)) mustBe None
        }
      )
    }

    "when optional pre-pop fields are missing" in {

      val userAnswers = emptyUserAnswers

      val result = transformer.transformFromEtmp(
        userAnswers,
        allowedAccessRequest.srn,
        loans(
          name = individualRecipientName,
          recipientIdentityType = RecipientIdentityType(IdentityType.Individual, Some(nino.value), None, None),
          optSchemeHadLoans = None,
          amountOfTheLoan = partialAmountOfTheLoan,
          interestOnLoan = partialInterestOnLoan,
          optArrearsPrevYears = None,
          optOutstandingArrearsOnLoan = None
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(LoansRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(LoansMadeOrOutstandingPage(srn)) mustBe None

          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.LoanRecipient)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(IndividualRecipientNamePage(srn, 1)) mustBe Some(individualRecipientName)
          userAnswers.get(IndividualRecipientNinoPage(srn, 1)) mustBe Some(ConditionalYesNo.yes(nino))
          userAnswers.get(IsIndividualRecipientConnectedPartyPage(srn, 1)) mustBe Some(true)
          userAnswers.get(DatePeriodLoanPage(srn, 1)) mustBe Some(
            (localDate, Money(Double.MinPositiveValue), Int.MaxValue)
          )
          userAnswers.get(AmountOfTheLoanPage(srn, 1)) mustBe Some(
            AmountOfTheLoan(money, Some(Money(0.0)), Some(Money(0.0)))
          )
          userAnswers.get(AreRepaymentsInstalmentsPage(srn, 1)) mustBe Some(false)
          userAnswers.get(InterestOnLoanPage(srn, 1)) mustBe Some(
            InterestOnLoan(money, percentage, Some(Money(0.0)))
          )
          userAnswers.get(SecurityGivenForLoanPage(srn, 1)) mustBe Some(ConditionalYesNo.yes(security))
        }
      )
    }

    "when loan progress is set correctly" in {
      val userAnswers = emptyUserAnswers

      val loansData = loans(
        name = individualRecipientName,
        recipientIdentityType = RecipientIdentityType(IdentityType.Individual, Some(nino.value), None, None)
      )

      val result = transformer.transformFromEtmp(userAnswers, allowedAccessRequest.srn, loansData)

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => userAnswers.get(LoansProgress(srn, 1)) mustBe Some(SectionJourneyStatus.Completed)
      )
    }

    "should not default total income to zero when prePopulated entity is not yet checked" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        loansBlankTotalIncome(prePopulated = Some(false), recordVersion = Some("001"))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(AmountOfTheLoanPage(srn, 1)).flatMap(_.optAmountOutstanding) mustBe None
          userAnswers.get(AmountOfTheLoanPage(srn, 1)).flatMap(_.optCapRepaymentCY) mustBe None
          userAnswers.get(InterestOnLoanPage(srn, 1)).flatMap(_.optIntReceivedCY) mustBe None
          userAnswers.get(OutstandingArrearsOnLoanPage(srn, 1)).flatMap(_.value.toOption) mustBe None
        }
      )
    }

    "should default total income to zero when prePopulated entity is checked" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        loansBlankTotalIncome(prePopulated = Some(true), recordVersion = Some("001"))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(AmountOfTheLoanPage(srn, 1)).flatMap(_.optAmountOutstanding) mustBe Some(Money(0))
          userAnswers.get(AmountOfTheLoanPage(srn, 1)).flatMap(_.optCapRepaymentCY) mustBe Some(Money(0))
          userAnswers.get(InterestOnLoanPage(srn, 1)).flatMap(_.optIntReceivedCY) mustBe Some(Money(0))
          userAnswers.get(OutstandingArrearsOnLoanPage(srn, 1)).flatMap(_.value.toOption) mustBe Some(
            Money(0)
          )
        }
      )
    }

    "should default total income to zero when the version of the return is more than 1" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        loansBlankTotalIncome(prePopulated = None, recordVersion = Some("002"))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(AmountOfTheLoanPage(srn, 1)).flatMap(_.optAmountOutstanding) mustBe Some(Money(0))
          userAnswers.get(AmountOfTheLoanPage(srn, 1)).flatMap(_.optCapRepaymentCY) mustBe Some(Money(0))
          userAnswers.get(InterestOnLoanPage(srn, 1)).flatMap(_.optIntReceivedCY) mustBe Some(Money(0))
          userAnswers.get(OutstandingArrearsOnLoanPage(srn, 1)).flatMap(_.value.toOption) mustBe Some(
            Money(0)
          )
        }
      )
    }

    def loansBlankTotalIncome(prePopulated: Option[Boolean], recordVersion: Option[String]): Loans =
      Loans(
        recordVersion = Some("001"),
        optSchemeHadLoans = Some(true),
        Seq(
          LoanTransactions(
            prePopulated,
            recipientIdentityType = RecipientIdentityType(Individual, None, Some("sdf"), Some("sdf")),
            loanRecipientName = name,
            connectedPartyStatus = true,
            optRecipientSponsoringEmployer = Some("Yes"),
            datePeriodLoanDetails = LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
            loanAmountDetails = LoanAmountDetails(
              amountOfTheLoan.loanAmount.value,
              None,
              None
            ),
            equalInstallments = false,
            loanInterestDetails = LoanInterestDetails(
              interestOnLoan.loanInterestAmount.value,
              interestOnLoan.loanInterestRate.value,
              None
            ),
            optSecurityGivenDetails = Some(security.value),
            optArrearsPrevYears = Some(true),
            optOutstandingArrearsOnLoan = None
          )
        )
      )

    def loans(
      name: String,
      recipientIdentityType: RecipientIdentityType,
      optSchemeHadLoans: Option[Boolean] = Some(true),
      sponsoringEmployer: Boolean = false,
      amountOfTheLoan: AmountOfTheLoan = amountOfTheLoan,
      interestOnLoan: InterestOnLoan = interestOnLoan,
      optArrearsPrevYears: Option[Boolean] = Some(true),
      optOutstandingArrearsOnLoan: Option[Double] = Some(money.value)
    ): Loans =
      Loans(
        recordVersion = Some("001"),
        optSchemeHadLoans = optSchemeHadLoans,
        Seq(
          LoanTransactions(
            prePopulated = None,
            recipientIdentityType = recipientIdentityType,
            loanRecipientName = name,
            connectedPartyStatus = true,
            optRecipientSponsoringEmployer = if (sponsoringEmployer) Some("Yes") else None,
            datePeriodLoanDetails = LoanPeriod(localDate, Double.MinPositiveValue, Int.MaxValue),
            loanAmountDetails = LoanAmountDetails(
              amountOfTheLoan.loanAmount.value,
              amountOfTheLoan.optCapRepaymentCY.map(_.value),
              amountOfTheLoan.optAmountOutstanding.map(_.value)
            ),
            equalInstallments = false,
            loanInterestDetails = LoanInterestDetails(
              interestOnLoan.loanInterestAmount.value,
              interestOnLoan.loanInterestRate.value,
              interestOnLoan.optIntReceivedCY.map(_.value)
            ),
            optSecurityGivenDetails = Some(security.value),
            optArrearsPrevYears = optArrearsPrevYears,
            optOutstandingArrearsOnLoan = optOutstandingArrearsOnLoan
          )
        )
      )
  }
}
