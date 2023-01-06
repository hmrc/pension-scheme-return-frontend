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

//package models.backend.responses
//
//import models.frontend.request.DataEntryRule
//import play.api.libs.json.Json
//
//case class PensionSchemeReturnResponse(
//                                      rule: DataEntryRule
//                                      )
//
//object PensionSchemeReturnResponse {
//
//  implicit val dataEntryRuleReads= Json.reads[DataEntryRule]
//  implicit val pensionSchemeReturnReads= Json.reads[PensionSchemeReturnResponse]
//}

package uk.gov.hmrc.pensionschemereturn.models

import play.api.libs.json._

case class PensionSchemeReturn(
                                name: DataEntry[String]
                              )

case class DataEntry[A](
                         value: A,
                         rule: DataEntryRule,
                         changed: DataEntryChanged[A]
                       )

case class DataEntryChanged[A](
                                version: String,
                                previousValue: A
                              )

sealed trait DataEntryRule

object DataEntryRule {
  case object Updated extends DataEntryRule

  case object Fixed extends DataEntryRule

  case object None extends DataEntryRule
}

object PensionSchemeReturn {
  implicit val dataEntryRuleReads: Reads[DataEntryRule] = {
    case JsString("updated") => JsSuccess(DataEntryRule.Updated)
    case JsString("fixed") => JsSuccess(DataEntryRule.Fixed)
    case JsString("none") => JsSuccess(DataEntryRule.None)
    case _ => JsError ("Unexpected Rule")
  }

  implicit def dataEntryChangedWrites[A: Reads]: Reads[DataEntryChanged[A]] = Json.reads[DataEntryChanged[A]]

  implicit def dataEntryWrites[A: Reads]: Reads[DataEntry[A]] = Json.reads[DataEntry[A]]

  implicit val reads: Reads[PensionSchemeReturn] = Json.reads[PensionSchemeReturn]
}