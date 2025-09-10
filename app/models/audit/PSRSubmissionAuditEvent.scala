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

package models.audit

import models.requests.psr.PsrSubmission
import play.api.libs.json.{JsObject, Json}
import models.DateRange

import java.time.{LocalDate, ZoneId}
import java.time.format.DateTimeFormatter

case class PSRSubmissionAuditEvent(
  schemeName: String,
  schemeAdministratorOrPractitionerName: String,
  psaOrPspId: String,
  schemeTaxReference: String,
  affinityGroup: String,
  credentialRole: String,
  taxYear: DateRange,
  psrSubmission: PsrSubmission
) extends ExtendedAuditEvent {

  override def auditType: String = "PensionSchemeReturnSubmitted"

  override def details: JsObject = {

    val submissionDetails = Json.obj(
      "schemeName" -> schemeName,
      "pensionSchemeTaxReference" -> schemeTaxReference,
      "affinityGroup" -> affinityGroup,
      "credentialRolePsaPsp" -> credentialRole,
      "taxYear" -> taxYear.toYearFormat,
      "date" -> LocalDate.now(ZoneId.of("Europe/London")).format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
      "payload" -> Json.toJson(psrSubmission),
      "submissionAmendment" -> psrSubmission.minimalRequiredSubmission.reportDetails.fbVersion.exists(_.toInt > 1)
    )
    psaOrPspIdDetails(credentialRole, psaOrPspId, schemeAdministratorOrPractitionerName) ++ submissionDetails
  }
}
