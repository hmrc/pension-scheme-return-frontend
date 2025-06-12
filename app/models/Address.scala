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

package models

import utils.Country
import cats.data.NonEmptyList
import play.api.libs.json._
import play.api.libs.functional.syntax.toFunctionalBuilderOps

case class ALFCountry(code: String, name: String)

case class ALFAddress(lines: Seq[String], town: String, postcode: String, country: ALFCountry) {
  val firstLine: Option[String] = lines.headOption
  val secondLine: Option[String] = lines.drop(1).headOption
  val thirdLine: Option[String] = lines.drop(2).headOption
}

/**
 * Based on this schema:
 * https://github.com/hmrc/address-lookup/blob/main/public/api/conf/1.0/docs/uk-address-object.json First line, town,
 * postcode and country are required
 */
case class ALFAddressResponse(id: String, address: ALFAddress)

object ALFAddressResponse {
  implicit val countryFormat: OFormat[ALFCountry] = Json.format[ALFCountry]
  implicit val addressFormat: OFormat[ALFAddress] = Json.format[ALFAddress]
  implicit val responseFormat: OFormat[ALFAddressResponse] = Json.format[ALFAddressResponse]
}

sealed trait AddressType

case object ManualAddress extends AddressType

case object LookupAddress extends AddressType

case class Address(
  id: String,
  addressLine1: String,
  addressLine2: Option[String],
  addressLine3: Option[String],
  town: String,
  postCode: Option[String],
  country: String,
  countryCode: String,
  addressType: AddressType,
  street: Option[String] = None,
  houseNumber: Option[Int] = None,
  flatNumber: Option[Int] = None,
  flat: Option[String] = None,
  canRemove: Boolean = true
) {
  val asString: String =
    s"""$addressLine1, ${addressLine2.fold("")(al2 => s"$al2, ")}${addressLine3.fold("")(al3 =>
        s"$al3, "
      )}$town${postCode
        .fold("")(postcode => s", $postcode")}"""

  val asNel: NonEmptyList[String] = NonEmptyList.of(addressLine1) ++ List(addressLine2, addressLine3).flatten ++ List(
    town
  ) ++ List.from(postCode) :+ country

  val asUKAddressTuple: (String, Option[String], Option[String], String, Option[String]) =
    (addressLine1, addressLine2, addressLine3, town, postCode)

  val asInternationalAddressTuple: (String, Option[String], Option[String], String, Option[String], String) =
    (addressLine1, addressLine2, addressLine3, town, postCode, countryCode)

  val isManualAddress: Boolean = addressType match {
    case ManualAddress => true
    case LookupAddress => false
  }
}

object Address {

  implicit val lookupFormat: OFormat[LookupAddress.type] = Json.format[LookupAddress.type]
  implicit val manualFormat: OFormat[ManualAddress.type] = Json.format[ManualAddress.type]
  implicit val addressTypeFormat: OFormat[AddressType] = Json.format[AddressType]

  private val addressReadsBuilder =
    (JsPath \ "id")
      .readWithDefault("manual")
      .and((JsPath \ "addressLine1").read[String])
      .and((JsPath \ "addressLine2").readNullable[String])
      .and((JsPath \ "addressLine3").readNullable[String])
      .and((JsPath \ "town").read[String])
      .and((JsPath \ "postCode").readNullable[String])
      .and((JsPath \ "countryCode").read[String])
      .and((JsPath \ "addressType").readWithDefault[AddressType](ManualAddress))
      .and((JsPath \ "street").readNullable[String])
      .and((JsPath \ "houseNumber").readNullable[Int])
      .and((JsPath \ "flatNumber").readNullable[Int])
      .and((JsPath \ "flat").readNullable[String])

  implicit val addressReads: Reads[Address] =
    addressReadsBuilder.apply {
      (
        id,
        addressLine1,
        addressLine2,
        addressLine3,
        town,
        postCode,
        countryCode,
        addressType,
        street,
        houseNumber,
        flatNumber,
        flat
      ) =>
        Address(
          id,
          addressLine1,
          addressLine2,
          addressLine3,
          town,
          postCode,
          Country.getCountry(countryCode).getOrElse(""),
          countryCode,
          addressType,
          street,
          houseNumber,
          flatNumber,
          flat
        )
    }

  implicit val addressWrites: Writes[Address] = Json.writes[Address]

  def fromManualUKAddress(tup: (String, Option[String], Option[String], String, Option[String])): Address = {
    val (addressLine1, addressLine2, addressLine3, town, postCode) = tup
    Address(
      "manual",
      addressLine1,
      addressLine2,
      addressLine3,
      town,
      postCode,
      "United Kingdom",
      "GB",
      ManualAddress
    )
  }

  def fromManualInternationalAddress(
    tup: (String, Option[String], Option[String], String, Option[String], String)
  ): Address = {
    val (addressLine1, addressLine2, addressLine3, town, postCode, countryCode) = tup
    Address(
      "manual",
      addressLine1,
      addressLine2,
      addressLine3,
      town,
      postCode,
      Country.getCountry(countryCode).getOrElse(countryCode),
      countryCode,
      ManualAddress
    )
  }
}
