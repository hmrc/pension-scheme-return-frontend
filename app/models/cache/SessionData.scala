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

package models.cache

import play.api.libs.json._
import utils.WithName

case class SessionData(administratorOrPractitioner: PensionSchemeUser)

object SessionData {

  implicit val reads: Reads[SessionData] = Json.reads[SessionData]
}

sealed trait PensionSchemeUser

object PensionSchemeUser {

  case object Administrator extends WithName("administrator") with PensionSchemeUser
  case object Practitioner extends WithName("practitioner") with PensionSchemeUser

  implicit val reads: Reads[PensionSchemeUser] = {
    case JsString(Administrator.name) => JsSuccess(Administrator)
    case JsString(Practitioner.name) => JsSuccess(Practitioner)
    case _ => JsError("unknown value")
  }
}
