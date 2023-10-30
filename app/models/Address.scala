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

import play.api.libs.functional.FunctionalBuilder
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json.{JsPath, Json, Reads, Writes}

import java.util.Locale

case class ALFCountry(code: String, name: String)

case class ALFAddress(lines: Seq[String], postcode: Option[String], country: ALFCountry) extends {
  val firstLine: Option[String] = lines.headOption
  val secondLine: Option[String] = lines.drop(1).headOption
  val thirdLine: Option[String] = lines.drop(2).headOption
  val town: Option[String] = lines.drop(3).headOption
}

case class Address(
  addressLine1: String,
  addressLine2: String,
  addressLine3: Option[String],
  town: Option[String],
  postCode: Option[String],
  country: String,
  countryCode: String
)

object Address {

  private val addressReadsBuilder
    : FunctionalBuilder[Reads]#CanBuild6[String, String, Option[String], Option[String], Option[String], String] =
    (JsPath \ "addressLine1")
      .read[String]
      .and((JsPath \ "addressLine2").read[String])
      .and((JsPath \ "addressLine3").readNullable[String])
      .and((JsPath \ "town").readNullable[String])
      .and((JsPath \ "postCode").readNullable[String])
      .and((JsPath \ "countryCode").read[String])
  implicit val addressReads: Reads[Address] =
    addressReadsBuilder.apply(
      (addressLine1, addressLine2, addressLine3, town, postCode, countryCode) => {
        val locale = new Locale("en", countryCode)
        Address(
          addressLine1,
          addressLine2,
          addressLine3,
          town,
          postCode,
          locale.getDisplayCountry(),
          countryCode
        )
      }
    )
  implicit val addressWrites: Writes[Address] = Json.writes[Address]
}
