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
import play.api.libs.json.{JsString, Json, OWrites, Writes}

import java.time.LocalDate

case class Assets(landOrProperty: LandOrProperty)
case class LandOrProperty(
  landOrPropertyHeld: Boolean,
  //disposeAnyLandOrProperty: Boolean,
  landOrPropertyTransactions: Seq[LandOrPropertyTransactions]
)

case class LandOrPropertyTransactions(
  propertyDetails: PropertyDetails,
  heldPropertyTransaction: HeldPropertyTransaction
)

case class PropertyDetails(
  landOrPropertyInUK: Boolean,
  addressDetails: Address,
  landRegistryTitleNumberKey: String,
  landRegistryTitleNumberValue: String
)

case class HeldPropertyTransaction(
  methodOfHolding: SchemeHoldLandProperty,
  dateOfAcquisitionOrContribution: Option[LocalDate],
  propertyAcquiredFromName: Option[String],
  propertyAcquiredFrom: Option[PropertyAcquiredFrom],
  connectedPartyStatus: Boolean,
  totalCostOfLandOrProperty: Double,
  indepValuationSupport: Option[Boolean],
  isLandOrPropertyResidential: Boolean,
  leaseDetails: LeaseDetails,
  landOrPropertyLeased: Boolean,
  totalIncomeOrReceipts: Double
)

case class PropertyAcquiredFrom(
  identityType: IdentityType,
  idNumber: Option[String],
  reasonNoIdNumber: Option[String],
  otherDescription: Option[String],
  connectedPartyStatus: Boolean
)

case class LeaseDetails(
  lesseeName: String,
  leaseGrantDate: Double,
  annualLeaseAmount: String
)

object Assets {

  private implicit val writesLeaseDetails: OWrites[LeaseDetails] = Json.writes[LeaseDetails]
  private implicit val writesSchemeHoldLandProperty: Writes[SchemeHoldLandProperty] =
    Writes(value => JsString(value.toString))
  private implicit val writesPropertyAcquiredFrom: OWrites[PropertyAcquiredFrom] = Json.writes[PropertyAcquiredFrom]
  private implicit val writesHeldPropertyTransaction: OWrites[HeldPropertyTransaction] =
    Json.writes[HeldPropertyTransaction]
  private implicit val writesPropertyDetails: OWrites[PropertyDetails] = Json.writes[PropertyDetails]
  private implicit val writesLandOrPropertyTransactions: OWrites[LandOrPropertyTransactions] =
    Json.writes[LandOrPropertyTransactions]
  private implicit val writesLandOrProperty: OWrites[LandOrProperty] = Json.writes[LandOrProperty]
  implicit val writes: OWrites[Assets] = Json.writes[Assets]
}
