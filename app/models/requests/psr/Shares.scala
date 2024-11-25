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

import play.api.libs.json._
import models.{PropertyAcquiredFrom, SchemeHoldShare, TypeOfShares}

import java.time.LocalDate

case class Shares(
  recordVersion: Option[String],
  optShareTransactions: Option[List[ShareTransaction]],
  optTotalValueQuotedShares: Option[Double]
)

case class ShareTransaction(
  typeOfSharesHeld: TypeOfShares,
  shareIdentification: ShareIdentification,
  heldSharesTransaction: HeldSharesTransaction,
  optDisposedSharesTransaction: Option[Seq[DisposedSharesTransaction]]
)

case class ShareIdentification(
  nameOfSharesCompany: String,
  optCrnNumber: Option[String],
  optReasonNoCRN: Option[String],
  classOfShares: String
)

case class HeldSharesTransaction(
  schemeHoldShare: SchemeHoldShare,
  optDateOfAcqOrContrib: Option[LocalDate],
  totalShares: Int,
  optAcquiredFromName: Option[String],
  optPropertyAcquiredFrom: Option[PropertyAcquiredFrom],
  optConnectedPartyStatus: Option[Boolean],
  costOfShares: Double,
  supportedByIndepValuation: Boolean,
  optTotalAssetValue: Option[Double],
  optTotalDividendsOrReceipts: Option[Double]
)

case class DisposedSharesTransaction(
  methodOfDisposal: String,
  optOtherMethod: Option[String],
  optSalesQuestions: Option[SalesQuestions],
  optRedemptionQuestions: Option[RedemptionQuestions],
  totalSharesNowHeld: Int
)

case class SalesQuestions(
  dateOfSale: LocalDate,
  noOfSharesSold: Int,
  amountReceived: Double,
  nameOfPurchaser: String,
  purchaserType: PropertyAcquiredFrom,
  connectedPartyStatus: Boolean,
  supportedByIndepValuation: Boolean
)

case class RedemptionQuestions(
  dateOfRedemption: LocalDate,
  noOfSharesRedeemed: Int,
  amountReceived: Double
)

object Shares {
  private implicit val formatRedemptionQuestions: Format[RedemptionQuestions] =
    Json.format[RedemptionQuestions]
  private implicit val formatSalesQuestions: Format[SalesQuestions] =
    Json.format[SalesQuestions]
  private implicit val formatDisposedSharesTransaction: Format[DisposedSharesTransaction] =
    Json.format[DisposedSharesTransaction]
  private implicit val formatHeldSharesTransaction: Format[HeldSharesTransaction] = Json.format[HeldSharesTransaction]
  private implicit val formatShareIdentification: Format[ShareIdentification] = Json.format[ShareIdentification]
  private implicit val formatShareTransaction: Format[ShareTransaction] = Json.format[ShareTransaction]
  implicit val format: Format[Shares] = Json.format[Shares]
}
