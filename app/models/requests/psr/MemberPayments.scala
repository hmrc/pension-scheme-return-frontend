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

package models.requests.psr

import cats.syntax.either._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import viewmodels.models.MemberState

import java.time.LocalDate

case class MemberPayments(
  checked: Option[Boolean],
  recordVersion: Option[String],
  memberDetails: List[MemberDetails],
  employerContributionMade: Option[Boolean],
  transfersInMade: Option[Boolean],
  transfersOutMade: Option[Boolean],
  unallocatedContribsMade: Option[Boolean],
  unallocatedContribAmount: Option[Double],
  memberContributionMade: Option[Boolean],
  lumpSumReceived: Option[Boolean],
  pensionReceived: Option[Boolean],
  surrenderMade: Option[Boolean]
)

case class MemberDetails(
  prePopulated: Option[Boolean],
  state: MemberState,
  memberPSRVersion: Option[String],
  personalDetails: MemberPersonalDetails,
  employerContributions: List[EmployerContributions],
  transfersIn: List[TransfersIn],
  totalContributions: Option[Double],
  memberLumpSumReceived: Option[MemberLumpSumReceived],
  transfersOut: List[TransfersOut],
  benefitsSurrendered: Option[SurrenderedBenefits],
  pensionAmountReceived: Option[Double]
)

case class MemberPersonalDetails(
  firstName: String,
  lastName: String,
  nino: Option[String],
  reasonNoNINO: Option[String],
  dateOfBirth: LocalDate
)

case class EmployerContributions(
  employerName: String,
  employerType: EmployerType,
  totalTransferValue: Double
)

case class MemberLumpSumReceived(
  lumpSumAmount: Double,
  designatedPensionAmount: Double
) {
  val isZero: Boolean = lumpSumAmount == 0d && designatedPensionAmount == 0d
}

object MemberLumpSumReceived {
  val zero: MemberLumpSumReceived = MemberLumpSumReceived(0d, 0d)
}

sealed trait EmployerType

object EmployerType {
  case class UKCompany(eitherIdOrReason: Either[String, String]) extends EmployerType
  case class UKPartnership(eitherIdOrReason: Either[String, String]) extends EmployerType
  case class Other(description: String) extends EmployerType

  implicit val reads: Reads[EmployerType] =
    (__ \ "employerType")
      .read[String]
      .and(
        (__ \ "value").read[String].map(_.asRight[String]) |
          (__ \ "reason").read[String].map(_.asLeft[String])
      )
      .tupled
      .flatMap {
        case ("UKCompany", either) => Reads.pure(EmployerType.UKCompany(either))
        case ("UKPartnership", either) => Reads.pure(EmployerType.UKPartnership(either))
        case ("Other", Right(description)) => Reads.pure(EmployerType.Other(description))
        case unknown => Reads.failed(s"Failed to read EmployerType with unknown pattern $unknown")
      }

  implicit val writes: Writes[EmployerType] = {
    case UKCompany(Left(reason)) =>
      Json.obj(
        "employerType" -> "UKCompany",
        "reason" -> reason
      )
    case UKCompany(Right(value)) =>
      Json.obj(
        "employerType" -> "UKCompany",
        "value" -> value
      )
    case UKPartnership(Left(reason)) =>
      Json.obj(
        "employerType" -> "UKPartnership",
        "reason" -> reason
      )
    case UKPartnership(Right(value)) =>
      Json.obj(
        "employerType" -> "UKPartnership",
        "value" -> value
      )
    case Other(description) =>
      Json.obj(
        "employerType" -> "Other",
        "value" -> description
      )
  }
}

object MemberPersonalDetails {
  implicit val format: Format[MemberPersonalDetails] = Json.format[MemberPersonalDetails]
}

object MemberPayments {
  private implicit val formatMemberLumpSumReceived: Format[MemberLumpSumReceived] = Json.format[MemberLumpSumReceived]
  private implicit val formatEmployerContributions: Format[EmployerContributions] = Json.format[EmployerContributions]
  private implicit val formatMemberPersonalDetails: Format[MemberPersonalDetails] = Json.format[MemberPersonalDetails]
  private implicit val formatMemberDetails: Format[MemberDetails] = Json.format[MemberDetails]
  implicit val format: Format[MemberPayments] = Json.format[MemberPayments]
}
