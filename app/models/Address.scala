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

import play.api.libs.json.{Json, OFormat}

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
  implicit val format: OFormat[Address] = Json.format[Address]
}
