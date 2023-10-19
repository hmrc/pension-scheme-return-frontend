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

import models.{Address, IdentityType, SchemeHoldLandProperty}
import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class Assets(landOrProperty: LandOrProperty)
case class LandOrProperty(
  landOrPropertyHeld: Boolean,
  landOrPropertyTransactions: Seq[LandOrPropertyTransactions]
)

case class LandOrPropertyTransactions(
  propertyDetails: PropertyDetails,
  heldPropertyTransaction: HeldPropertyTransaction
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

case class PropertyAcquiredFrom(
  identityType: IdentityType,
  idNumber: Option[String],
  reasonNoIdNumber: Option[String],
  otherDescription: Option[String]
)

case class LeaseDetails(
  lesseeName: String,
  leaseGrantDate: LocalDate,
  annualLeaseAmount: Double,
  connectedPartyStatus: Boolean
)

object Assets {

  private implicit val formatLeaseDetails: OFormat[LeaseDetails] = Json.format[LeaseDetails]
  private implicit val formatPropertyAcquiredFrom: OFormat[PropertyAcquiredFrom] = Json.format[PropertyAcquiredFrom]
  private implicit val formatHeldPropertyTransaction: OFormat[HeldPropertyTransaction] =
    Json.format[HeldPropertyTransaction]
  private implicit val formatPropertyDetails: OFormat[PropertyDetails] = Json.format[PropertyDetails]
  private implicit val formatLandOrPropertyTransactions: OFormat[LandOrPropertyTransactions] =
    Json.format[LandOrPropertyTransactions]
  private implicit val formatLandOrProperty: OFormat[LandOrProperty] = Json.format[LandOrProperty]
  implicit val writes: OFormat[Assets] = Json.format[Assets]
}
