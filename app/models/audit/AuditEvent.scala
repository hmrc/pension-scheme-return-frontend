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

import config.Constants.PSA
import play.api.libs.json.{JsObject, Json}
trait AuditEvent {

  def auditType: String
}

trait BasicAuditEvent extends AuditEvent {

  def details: Map[String, String]

  def psaOrPspIdDetails(
    credentialRole: String,
    psaOrPspId: String,
    schemeAdministratorOrPractitionerName: String
  ): Map[String, String] =
    credentialRole match {
      case PSA =>
        Map(
          "pensionSchemeAdministratorId" -> psaOrPspId,
          "schemeAdministratorName" -> schemeAdministratorOrPractitionerName
        )
      case _ =>
        Map(
          "pensionSchemePractitionerId" -> psaOrPspId,
          "schemePractitionerName" -> schemeAdministratorOrPractitionerName
        )
    }
}

trait ExtendedAuditEvent extends AuditEvent {

  def details: JsObject

  def psaOrPspIdDetails(
    credentialRole: String,
    psaOrPspId: String,
    schemeAdministratorOrPractitionerName: String
  ): JsObject =
    credentialRole match {
      case PSA =>
        Json.obj(
          "pensionSchemeAdministratorId" -> psaOrPspId,
          "schemeAdministratorName" -> schemeAdministratorOrPractitionerName
        )
      case _ =>
        Json.obj(
          "pensionSchemePractitionerId" -> psaOrPspId,
          "schemePractitionerName" -> schemeAdministratorOrPractitionerName
        )
    }
}
