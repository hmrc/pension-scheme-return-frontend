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

case class PSRStartAuditEvent(
  schemeName: String,
  schemeAdministratorName: String,
  psaOrPspId: String,
  schemeTaxReference: String,
  affinityGroup: String,
  credentialRole: String,
  taxYear: DateRange,
  howManyMembers: Int,
  howManyDeferredMembers: Int,
  howManyPensionerMembers: Int
) extends AuditEvent {

  override def auditType: String = "PensionSchemeReturnStart"

  override def details: Map[String, String] = Map(
    "SchemeName" -> schemeName,
    "SchemeAdministratorName" -> schemeAdministratorName,
    "PensionSchemeAdministratorOrPensionSchemePractitionerId" -> psaOrPspId,
    "PensionSchemeTaxReference" -> schemeTaxReference,
    "AffinityGroup" -> affinityGroup,
    "CredentialRole(PSA/PSP)" -> credentialRole,
    "TaxYear" -> s"${taxYear.from.getYear}-${taxYear.to.getYear}",
    "HowManyMembers" -> howManyMembers.toString,
    "HowManyDeferredMembers" -> howManyDeferredMembers.toString,
    "HowManyPensionerMembers" -> howManyPensionerMembers.toString
  )
}
