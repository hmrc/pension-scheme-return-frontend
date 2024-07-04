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

package viewmodels.models

import play.api.libs.json._

sealed trait PSRStatus {
  val name: String
  val isCompiled: Boolean = this == Compiled
  val isSubmitted: Boolean = this == Submitted
}

case object Compiled extends PSRStatus {
  val name = "Compiled"
}

case object Submitted extends PSRStatus {
  val name = "Submitted"
}

object PSRStatus {

  private val values: List[PSRStatus] = List(Compiled, Submitted)

  implicit val formats: Format[PSRStatus] = new Format[PSRStatus] {
    override def writes(o: PSRStatus): JsValue = JsString(o.name)

    override def reads(json: JsValue): JsResult[PSRStatus] = {
      val jsonAsString = json.as[String]
      values.find(_.toString == jsonAsString) match {
        case Some(status) => JsSuccess(status)
        case None => JsError(s"Unknown psr status: $jsonAsString")
      }
    }
  }
}
