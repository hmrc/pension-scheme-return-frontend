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

package models
import uk.gov.hmrc.domain._
import play.api.libs.json.{Reads, Writes}

case class Crn(crn: String) extends TaxIdentifier with SimpleName {

  require(Crn.isValid(crn), s"$crn is not a valid crn.")

  override def toString = crn

  def value = crn

  val name = "crn"
}

object Crn extends (String => Crn) {
  implicit val crnWrite: Writes[Crn] = new SimpleObjectWrites[Crn](_.value)
  implicit val crnRead: Reads[Crn] = new SimpleObjectReads[Crn]("crn", Crn.apply)

  private val validCrnFormat = "^[A-Za-z0-9 ]*$"
  private val length = 8

  def isValid(crn: String): Boolean = crn != null && crn.matches(validCrnFormat)
  def isLengthInRange(crn: String): Boolean = crn != null && (cutSpaces(crn).length == length)

  // To do not let spaces tamper the length check
  private def cutSpaces(crn: String): String = crn.replaceAll("\\s", "")
}
