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

import models.{DateRange, UploadStatus}
import models.UploadStatus.UploadStatus

case class PSRUpscanFileUploadAuditEvent(
  schemeName: String,
  schemeAdministratorOrPractitionerName: String,
  psaOrPspId: String,
  schemeTaxReference: String,
  affinityGroup: String,
  credentialRole: String,
  taxYear: DateRange,
  outcome: UploadStatus,
  uploadTimeInMilliSeconds: Long
) extends BasicAuditEvent {

  override def auditType: String = "PensionSchemeReturnFileUpload"

  override def details: Map[String, String] = {

    val common = Map(
      "schemeName" -> schemeName,
      "pensionSchemeTaxReference" -> schemeTaxReference,
      "affinityGroup" -> affinityGroup,
      "credentialRolePsaPsp" -> credentialRole,
      "taxYear" -> taxYear.toYearFormat,
      "fileUploadType" -> "MembersDetails",
      "uploadTime" -> uploadTimeInMilliSeconds.toString
    )

    val outcomeMap = outcome match {
      case UploadStatus.Failed(failure) =>
        Map(
          "fileUploadStatus" -> "Failed",
          "typeOfError" -> failure.failureReason,
          "failureMessage" -> failure.message
        )

      case UploadStatus.Success(_, _, downloadUrl, size) =>
        Map(
          "fileUploadStatus" -> "Success",
          "fileReference" -> downloadUrl,
          "fileSize" -> size.getOrElse(0L).toString
        )

      case UploadStatus.InProgress =>
        Map(
          "fileUploadStatus" -> "InProgress"
        )
    }

    psaOrPspIdDetails(credentialRole, psaOrPspId, schemeAdministratorOrPractitionerName) ++ common ++ outcomeMap
  }
}
