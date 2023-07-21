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

package models
import models.ConditionalYesNo.ConditionalYes
import play.api.libs.json.{Reads, Writes}
import uk.gov.hmrc.domain.{SimpleName, SimpleObjectReads, SimpleObjectWrites, TaxIdentifier}
import utils.Transform

case class Security(security: String) extends TaxIdentifier with SimpleName {

  require(Security.isValid(security), s"$security is not a valid security.")

  override def toString = security

  def value = security

  val name = "security"
}

object Security extends (String => Security) {
  implicit val securityWrite: Writes[Security] = new SimpleObjectWrites[Security](_.value)
  implicit val securityRead: Reads[Security] = new SimpleObjectReads[Security]("security", Security.apply)

  private val validSecurityFormat = """^[a-zA-Z0-9\-'" \t\r\n,.@/]+$"""
  private val maxLength = 160

  def isValid(security: String) = security != null && security.matches(validSecurityFormat)
  def maxLengthCheck(security: String) = security != null && (maxLength > security.length)

}
