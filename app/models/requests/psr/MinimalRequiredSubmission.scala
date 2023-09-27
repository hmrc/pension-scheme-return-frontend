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

import cats.data.NonEmptyList
import play.api.libs.json.{Json, OWrites}
import utils.JsonUtils._

import java.time.LocalDate

case class MinimalRequiredSubmission(
  reportDetails: ReportDetails,
  accountingPeriods: NonEmptyList[(LocalDate, LocalDate)],
  schemeDesignatory: SchemeDesignatory
)

case class ReportDetails(
  pstr: String,
  periodStart: LocalDate,
  periodEnd: LocalDate
)

case class SchemeDesignatory(
  openBankAccount: Boolean,
  reasonForNoBankAccount: Option[String],
  activeMembers: Int,
  deferredMembers: Int,
  pensionerMembers: Int,
  totalAssetValueStart: Option[Double],
  totalAssetValueEnd: Option[Double],
  totalCashStart: Option[Double],
  totalCashEnd: Option[Double],
  totalPayments: Option[Double]
)

object MinimalRequiredSubmission {
  private implicit val reportDetailsWrites: OWrites[ReportDetails] = Json.writes[ReportDetails]
  private implicit val schemeDesignatoryWrites: OWrites[SchemeDesignatory] = Json.writes[SchemeDesignatory]
  implicit val writes: OWrites[MinimalRequiredSubmission] = Json.writes[MinimalRequiredSubmission]
}
