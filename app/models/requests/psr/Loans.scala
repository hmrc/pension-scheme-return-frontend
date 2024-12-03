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

package models.requests.psr

import play.api.libs.json.{Json, OFormat}
import models.IdentityType

import java.time.LocalDate

case class Loans(
  recordVersion: Option[String],
  optSchemeHadLoans: Option[Boolean],
  loanTransactions: Seq[LoanTransactions]
)

case class LoanTransactions(
  recipientIdentityType: RecipientIdentityType,
  loanRecipientName: String,
  connectedPartyStatus: Boolean,
  optRecipientSponsoringEmployer: Option[String],
  datePeriodLoanDetails: LoanPeriod,
  loanAmountDetails: LoanAmountDetails,
  equalInstallments: Boolean,
  loanInterestDetails: LoanInterestDetails,
  optSecurityGivenDetails: Option[String],
  optArrearsPrevYears: Option[Boolean],
  optOutstandingArrearsOnLoan: Option[Double]
)

case class RecipientIdentityType(
  identityType: IdentityType,
  idNumber: Option[String],
  reasonNoIdNumber: Option[String],
  otherDescription: Option[String]
)
case class LoanPeriod(
  dateOfLoan: LocalDate,
  loanTotalSchemeAssets: Double,
  loanPeriodInMonths: Int
)

case class LoanAmountDetails(
  loanAmount: Double,
  optCapRepaymentCY: Option[Double],
  optAmountOutstanding: Option[Double]
)

case class LoanInterestDetails(
  loanInterestAmount: Double,
  loanInterestRate: Double,
  optIntReceivedCY: Option[Double]
)

object Loans {

  private implicit val formatLoanInterestDetails: OFormat[LoanInterestDetails] = Json.format[LoanInterestDetails]
  private implicit val formatLoanAmountDetails: OFormat[LoanAmountDetails] = Json.format[LoanAmountDetails]
  private implicit val formatLoanPeriod: OFormat[LoanPeriod] = Json.format[LoanPeriod]
  private implicit val formatRecipientIdentityType: OFormat[RecipientIdentityType] = Json.format[RecipientIdentityType]
  private implicit val formatLoanTransactions: OFormat[LoanTransactions] = Json.format[LoanTransactions]
  implicit val format: OFormat[Loans] = Json.format[Loans]
}
