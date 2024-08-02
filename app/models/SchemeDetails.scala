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

import utils.WithName
import utils.Extractors.Int
import play.api.libs.json._
import play.api.libs.functional.syntax._

import java.time.LocalDate

case class SchemeDetails(
  schemeName: String,
  pstr: String,
  schemeStatus: SchemeStatus,
  schemeType: String,
  authorisingPSAID: Option[String],
  establishers: List[Establisher]
)

case class Establisher(
  name: String,
  kind: EstablisherKind
)

sealed class EstablisherKind(val value: String)

object EstablisherKind {
  case object Company extends EstablisherKind("company")
  case object Partnership extends EstablisherKind("partnership")
  case object Individual extends EstablisherKind("individual")

  implicit val reads: Reads[EstablisherKind] = Reads.StringReads.map {
    case Company.value => Company
    case Partnership.value => Partnership
    case Individual.value => Individual
  }
}

object Establisher {

  private val companyEstablisherReads: Reads[Establisher] =
    (__ \ "companyDetails" \ "companyName").read[String].map(name => Establisher(name, EstablisherKind.Company))

  private val partnershipEstablisherReads: Reads[Establisher] =
    (__ \ "partnershipDetails" \ "name").read[String].map(name => Establisher(name, EstablisherKind.Partnership))

  private val individualEstablisherReads: Reads[Establisher] = {

    (__ \ "establisherDetails" \ "firstName")
      .read[String]
      .and((__ \ "establisherDetails" \ "middleName").readNullable[String])
      .and((__ \ "establisherDetails" \ "lastName").read[String]) { (first, middle, last) =>
        val name = s"$first ${middle.fold("")(m => s"$m ")}$last"
        Establisher(name, EstablisherKind.Individual)
      }
  }

  implicit val reads: Reads[Establisher] =
    (__ \ "establisherKind").read[EstablisherKind].flatMap {
      case EstablisherKind.Company => companyEstablisherReads
      case EstablisherKind.Partnership => partnershipEstablisherReads
      case EstablisherKind.Individual => individualEstablisherReads
    }
}

object SchemeDetails {

  implicit val reads: Reads[SchemeDetails] =
    (__ \ "schemeName")
      .read[String]
      .and((__ \ "pstr").read[String])
      .and((__ \ "schemeStatus").read[SchemeStatus])
      .and((__ \ "schemeType" \ "name").read[String])
      .and((__ \ "pspDetails" \ "authorisingPSAID").readNullable[String])
      .and(
        (__ \ "establishers")
          .readWithDefault[JsArray](JsArray.empty)
          .map[List[Establisher]](
            l =>
              if (l.value.isEmpty) {
                Nil
              } else {
                l.as[List[Establisher]]
              }
          )
      )(SchemeDetails.apply _)
}

sealed trait SchemeStatus

object SchemeStatus {

  case object Pending extends WithName("Pending") with SchemeStatus
  case object PendingInfoRequired extends WithName("Pending Info Required") with SchemeStatus
  case object PendingInfoReceived extends WithName("Pending Info Received") with SchemeStatus
  case object Rejected extends WithName("Rejected") with SchemeStatus
  case object Open extends WithName("Open") with SchemeStatus
  case object Deregistered extends WithName("Deregistered") with SchemeStatus
  case object WoundUp extends WithName("Wound-up") with SchemeStatus
  case object RejectedUnderAppeal extends WithName("Rejected Under Appeal") with SchemeStatus

  implicit val reads: Reads[SchemeStatus] = {
    case JsString(Pending.name) => JsSuccess(Pending)
    case JsString(PendingInfoRequired.name) => JsSuccess(PendingInfoRequired)
    case JsString(PendingInfoReceived.name) => JsSuccess(PendingInfoReceived)
    case JsString(Rejected.name) => JsSuccess(Rejected)
    case JsString(Open.name) => JsSuccess(Open)
    case JsString(Deregistered.name) => JsSuccess(Deregistered)
    case JsString(WoundUp.name) => JsSuccess(WoundUp)
    case JsString(RejectedUnderAppeal.name) => JsSuccess(RejectedUnderAppeal)
    case _ => JsError("Unrecognized scheme status")
  }
}

case class ListMinimalSchemeDetails(schemeDetails: List[MinimalSchemeDetails])

object ListMinimalSchemeDetails {

  implicit val reads: Reads[ListMinimalSchemeDetails] = Json.reads[ListMinimalSchemeDetails]
}

case class MinimalSchemeDetails(
  name: String,
  srn: String,
  schemeStatus: SchemeStatus,
  openDate: Option[LocalDate],
  windUpDate: Option[LocalDate]
)

object MinimalSchemeDetails {

  private val dateRegex = "(\\d{4})-(\\d{1,2})-(\\d{1,2})".r
  private implicit val readLocalDate: Reads[LocalDate] = Reads[LocalDate] {
    case JsString(dateRegex(Int(year), Int(month), Int(day))) =>
      JsSuccess(LocalDate.of(year, month, day))
    case err => JsError(s"Unable to read local date from $err")
  }

  implicit val reads: Reads[MinimalSchemeDetails] =
    (__ \ "name")
      .read[String]
      .and((__ \ "referenceNumber").read[String])
      .and((__ \ "schemeStatus").read[SchemeStatus])
      .and((__ \ "openDate").readNullable[LocalDate])
      .and((__ \ "windUpDate").readNullable[LocalDate])(MinimalSchemeDetails.apply _)
}
