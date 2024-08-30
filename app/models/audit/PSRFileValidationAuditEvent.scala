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

import models.DateRange

case class PSRFileValidationAuditEvent(
  schemeName: String,
  schemeAdministratorOrPractitionerName: String,
  psaOrPspId: String,
  schemeTaxReference: String,
  affinityGroup: String,
  credentialRole: String,
  taxYear: DateRange,
  validationCheckStatus: String,
  fileValidationTimeInMilliSeconds: Long,
  numberOfEntries: Int,
  numberOfFailures: Int
) extends BasicAuditEvent {

  override def auditType: String = "PensionSchemeReturnFileValidationCheck"

  override def details: Map[String, String] = {

    val common = Map(
      "schemeName" -> schemeName,
      "pensionSchemeTaxReference" -> schemeTaxReference,
      "affinityGroup" -> affinityGroup,
      "credentialRole(PSA/PSP)" -> credentialRole,
      "taxYear" -> taxYear.toYearFormat,
      "validationCheckStatus" -> validationCheckStatus,
      "fileValidationTimeInMilliSeconds" -> fileValidationTimeInMilliSeconds.toString,
      "numberOfEntries" -> numberOfEntries.toString,
      "numberOfFailures" -> numberOfFailures.toString
    )

    psaOrPspIdDetails(credentialRole, psaOrPspId, schemeAdministratorOrPractitionerName) ++ common
  }
}
