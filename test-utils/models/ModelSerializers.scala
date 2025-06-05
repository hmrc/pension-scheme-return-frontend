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

import play.api.libs.json.Json.JsValueWrapper
import models.cache.PensionSchemeUser
import play.api.libs.json._

import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait ModelSerializers {

  implicit val writesIndividualDetails: Writes[IndividualDetails] = Json.writes[IndividualDetails]
  implicit val writesMinimalDetails: Writes[MinimalDetails] = Json.writes[MinimalDetails]
  implicit val writesPensionSchemeUser: Writes[PensionSchemeUser] = s => JsString(s.toString)
  implicit val writesSchemeStatus: Writes[SchemeStatus] = s => JsString(s.toString)

  implicit val writesEstablisher: Writes[Establisher] = establisher =>
    (establisher.kind match {
      case EstablisherKind.Company =>
        Json.obj("companyDetails" -> Json.obj("companyName" -> establisher.name))
      case EstablisherKind.Partnership =>
        Json.obj("partnershipDetails" -> Json.obj("name" -> establisher.name))
      case EstablisherKind.Individual =>
        val first :: rest = establisher.name.split(" ").toList
        val last :: middles = rest.reverse
        val middle = middles.iterator.reduceOption((a, b) => s"$a $b")
        Json.obj(
          "establisherDetails" -> Json
            .obj(
              "firstName" -> first,
              "lastName" -> last
            )
            .++(middle.fold(Json.obj())(m => Json.obj("middleName" -> m)))
        )
    }) ++ Json.obj("establisherKind" -> establisher.kind.value)

  implicit val writeSchemeDetails: Writes[SchemeDetails] = { details =>
    val authorisingPSAID: JsObject = details.authorisingPSAID.fold(Json.obj())(psaId =>
      Json.obj("pspDetails" -> Json.obj("authorisingPSAID" -> psaId))
    )

    Json.obj(
      "schemeName" -> details.schemeName,
      "pstr" -> details.pstr,
      "schemeStatus" -> details.schemeStatus,
      "schemeType" -> Json.obj("name" -> details.schemeType),
      "establishers" -> details.establishers
    ) ++ authorisingPSAID
  }

  implicit val writeMinimalSchemeDetails: Writes[MinimalSchemeDetails] = { details =>
    def formatDate(date: LocalDate): String =
      date.format(DateTimeFormatter.ofPattern("yyyy-M-d"))

    val fields =
      List[Option[(String, JsValueWrapper)]](
        Some("name" -> details.name),
        Some("referenceNumber" -> details.srn),
        Some("schemeStatus" -> details.schemeStatus.toString),
        details.openDate.map(d => "openDate" -> formatDate(d)),
        details.windUpDate.map(d => "windUpDate" -> formatDate(d))
      ).flatten

    Json.obj(fields: _*)
  }

  implicit val writeListMinimalSchemeDetails: Writes[ListMinimalSchemeDetails] = Json.writes[ListMinimalSchemeDetails]
}
