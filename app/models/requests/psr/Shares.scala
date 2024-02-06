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

package models.requests.psr

import models.{PropertyAcquiredFrom, SchemeHoldShare, TypeOfShares}
import play.api.libs.json._

import java.time.LocalDate

case class Shares(
  optShareTransactions: Option[List[ShareTransaction]]
)

case class ShareTransaction(
  typeOfSharesHeld: TypeOfShares,
  shareIdentification: ShareIdentification,
  heldSharesTransaction: HeldSharesTransaction
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
  totalDividendsOrReceipts: Double
)

object Shares {
  private implicit val formatHeldSharesTransaction: Format[HeldSharesTransaction] = Json.format[HeldSharesTransaction]
  private implicit val formatShareIdentification: Format[ShareIdentification] = Json.format[ShareIdentification]
  private implicit val formatShareTransaction: Format[ShareTransaction] = Json.format[ShareTransaction]
  implicit val format: Format[Shares] = Json.format[Shares]
}
