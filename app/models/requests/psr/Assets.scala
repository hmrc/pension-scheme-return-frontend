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

import play.api.libs.json._
import models._

import java.time.LocalDate

case class Assets(optLandOrProperty: Option[LandOrProperty], optBorrowing: Option[Borrowing], optBonds: Option[Bonds])

case class LandOrProperty(
  landOrPropertyHeld: Boolean,
  disposeAnyLandOrProperty: Boolean,
  landOrPropertyTransactions: Seq[LandOrPropertyTransactions]
)

case class LandOrPropertyTransactions(
  propertyDetails: PropertyDetails,
  heldPropertyTransaction: HeldPropertyTransaction,
  optDisposedPropertyTransaction: Option[Seq[DisposedPropertyTransaction]]
)

case class PropertyDetails(
  landOrPropertyInUK: Boolean,
  addressDetails: Address,
  landRegistryTitleNumberKey: Boolean,
  landRegistryTitleNumberValue: String
)

case class HeldPropertyTransaction(
  methodOfHolding: SchemeHoldLandProperty,
  dateOfAcquisitionOrContribution: Option[LocalDate],
  optPropertyAcquiredFromName: Option[String],
  optPropertyAcquiredFrom: Option[PropertyAcquiredFrom],
  optConnectedPartyStatus: Option[Boolean],
  totalCostOfLandOrProperty: Double,
  optIndepValuationSupport: Option[Boolean],
  isLandOrPropertyResidential: Boolean,
  optLeaseDetails: Option[LeaseDetails],
  landOrPropertyLeased: Boolean,
  totalIncomeOrReceipts: Double
)

case class DisposedPropertyTransaction(
  methodOfDisposal: String,
  optOtherMethod: Option[String],
  optDateOfSale: Option[LocalDate],
  optNameOfPurchaser: Option[String],
  optPropertyAcquiredFrom: Option[PropertyAcquiredFrom],
  optSaleProceeds: Option[Double],
  optConnectedPartyStatus: Option[Boolean],
  optIndepValuationSupport: Option[Boolean],
  portionStillHeld: Boolean
)

case class LeaseDetails(
  lesseeName: String,
  leaseGrantDate: LocalDate,
  annualLeaseAmount: Double,
  connectedPartyStatus: Boolean
)

case class Borrowing(moneyWasBorrowed: Boolean, moneyBorrowed: Seq[MoneyBorrowed])

case class MoneyBorrowed(
  dateOfBorrow: LocalDate,
  schemeAssetsValue: Double,
  amountBorrowed: Double,
  interestRate: Double,
  borrowingFromName: String,
  connectedPartyStatus: Boolean,
  reasonForBorrow: String
)

case class Bonds(
  bondsWereAdded: Boolean,
  bondsWereDisposed: Boolean,
  bondTransactions: Seq[BondTransactions]
)

case class BondTransactions(
  nameOfBonds: String,
  methodOfHolding: SchemeHoldBond,
  optDateOfAcqOrContrib: Option[LocalDate],
  costOfBonds: Double,
  optConnectedPartyStatus: Option[Boolean],
  bondsUnregulated: Boolean,
  totalIncomeOrReceipts: Double,
  optBondsDisposed: Option[Seq[BondDisposed]]
)

case class BondDisposed(
  methodOfDisposal: String,
  optOtherMethod: Option[String],
  optDateSold: Option[LocalDate],
  optAmountReceived: Option[Double],
  optBondsPurchaserName: Option[String],
  optConnectedPartyStatus: Option[Boolean],
  totalNowHeld: Int
)

object Assets {
  private implicit val formatBondDisposed: OFormat[BondDisposed] = Json.format[BondDisposed]
  private implicit val formatBondTransactions: OFormat[BondTransactions] = Json.format[BondTransactions]
  private implicit val formatBonds: OFormat[Bonds] = Json.format[Bonds]
  private implicit val formatMoneyBorrowed: OFormat[MoneyBorrowed] = Json.format[MoneyBorrowed]
  private implicit val formatBorrowing: OFormat[Borrowing] = Json.format[Borrowing]

  private implicit val formatLeaseDetails: OFormat[LeaseDetails] = Json.format[LeaseDetails]
  private implicit val formatHeldPropertyTransaction: OFormat[HeldPropertyTransaction] =
    Json.format[HeldPropertyTransaction]
  private implicit val formatDisposedPropertyTransaction: OFormat[DisposedPropertyTransaction] =
    Json.format[DisposedPropertyTransaction]
  private implicit val formatPropertyDetails: OFormat[PropertyDetails] = Json.format[PropertyDetails]
  private implicit val formatLandOrPropertyTransactions: OFormat[LandOrPropertyTransactions] =
    Json.format[LandOrPropertyTransactions]
  private implicit val formatLandOrProperty: OFormat[LandOrProperty] = Json.format[LandOrProperty]

  implicit val format: OFormat[Assets] = Json.format[Assets]
}
