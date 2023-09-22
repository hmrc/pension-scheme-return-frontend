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

package models

import play.api.libs.json.{Json, OWrites}

import java.time.LocalDate

case class LoansSubmission(
  minimalRequiredSubmission: MinimalRequiredSubmission,
  checkReturnDates: Boolean,
  loans: Loans
)

case class Loans(schemeHadLoans: Boolean, loanTransactions: Seq[LoanTransactions])

case class LoanTransactions(
  recipientIdentityType: RecipientIdentityType,
  loanRecipientName: String,
  optConnectedPartyStatus: Option[Boolean],
  optRecipientSponsoringEmployer: Option[String],
  datePeriodLoanDetails: LoanPeriod,
  loanAmountDetails: LoanAmountDetails,
  equalInstallments: Boolean,
  loanInterestDetails: LoanInterestDetails,
  optSecurityGivenDetails: Option[String],
  optOutstandingArrearsOnLoan: Option[Double]
)

case class RecipientIdentityType(
  identityType: IdentityType,
  idNumber: Option[String],
  reasonNoIdNumber: Option[String],
  otherDescription: Option[String]
)
case class LoanPeriod(dateOfLoan: LocalDate, loanTotalSchemeAssets: Double, loanPeriodInMonths: Int)
case class LoanAmountDetails(loanAmount: Double, capRepaymentCY: Double, amountOutstanding: Double)
case class LoanInterestDetails(loanInterestAmount: Double, loanInterestRate: Double, intReceivedCY: Double)

object LoansSubmission {
  implicit val writes: OWrites[LoansSubmission] = Json.writes[LoansSubmission]
  private implicit val writesLoans: OWrites[Loans] = Json.writes[Loans]
  private implicit val writesLoanTransactions: OWrites[LoanTransactions] = Json.writes[LoanTransactions]
  private implicit val writesRecipientIdentityType: OWrites[RecipientIdentityType] = Json.writes[RecipientIdentityType]
  private implicit val writesLoanPeriod: OWrites[LoanPeriod] = Json.writes[LoanPeriod]
  private implicit val writesLoanAmountDetails: OWrites[LoanAmountDetails] = Json.writes[LoanAmountDetails]
  private implicit val writesLoanInterestDetails: OWrites[LoanInterestDetails] = Json.writes[LoanInterestDetails]
}
