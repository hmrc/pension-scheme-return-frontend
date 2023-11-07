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

package models.audit

import models.DateRange

case class PSRFileValidationAuditEvent(
  schemeName: String,
  schemeAdministratorName: String,
  psaOrPspId: String,
  schemeTaxReference: String,
  affinityGroup: String,
  credentialRole: String,
  taxYear: DateRange,
  validationCheckStatus: String,
  fileValidationTimeInMilliSeconds: Long,
  numberOfEntries: Int,
  numberOfFailures: Int
) extends AuditEvent {

  override def auditType: String = "PensionSchemeReturnFileValidationCheck"

  override def details: Map[String, String] = {
    val common = Map(
      "SchemeName" -> schemeName,
      "SchemeAdministratorName" -> schemeAdministratorName,
      "PensionSchemeAdministratorOrPensionSchemePractitionerId" -> psaOrPspId,
      "PensionSchemeTaxReference" -> schemeTaxReference,
      "AffinityGroup" -> affinityGroup,
      "CredentialRole(PSA/PSP)" -> credentialRole,
      "TaxYear" -> s"${taxYear.from.getYear}-${taxYear.to.getYear}"
    )

    common ++
      Map(
        "ValidationCheckStatus" -> validationCheckStatus,
        "FileValidationTimeInMilliSeconds" -> fileValidationTimeInMilliSeconds.toString,
        "NumberOfEntries" -> numberOfEntries.toString,
        "NumberOfFailures" -> numberOfFailures.toString
      )
  }
}
