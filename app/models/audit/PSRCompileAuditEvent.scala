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

import play.api.libs.json.{JsObject, JsValue, Json}
import models.DateRange

case class PSRCompileAuditEvent(
  schemeName: String,
  schemeAdministratorOrPractitionerName: String,
  psaOrPspId: String,
  schemeTaxReference: String,
  affinityGroup: String,
  credentialRole: String,
  taxYear: DateRange,
  taskList: JsValue
) extends ExtendedAuditEvent {

  override def auditType: String = "PensionSchemeReturnCompiled"

  override def details: JsObject = {

    val compiledDetails = Json.obj(
      "SchemeName" -> schemeName,
      "PensionSchemeTaxReference" -> schemeTaxReference,
      "AffinityGroup" -> affinityGroup,
      "CredentialRole(PSA/PSP)" -> credentialRole,
      "TaxYear" -> taxYear.toYearFormat,
      "Sections" -> taskList
    )

    psaOrPspIdDetails(credentialRole, psaOrPspId, schemeAdministratorOrPractitionerName) ++ compiledDetails
  }
}
