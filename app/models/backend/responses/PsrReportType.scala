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

package models.backend.responses

import play.api.libs.json._

sealed trait PsrReportType {
  val name: String
}

object PsrReportType {
  case object Sipp extends PsrReportType {
    val name = "SIPP"
  }

  case object Standard extends PsrReportType {
    val name = "Standard"
  }

  private val values: List[PsrReportType] =
    List(Sipp, Standard)

  implicit val formats: Format[PsrReportType] = new Format[PsrReportType] {
    override def writes(o: PsrReportType): JsValue = JsString(o.name)

    override def reads(json: JsValue): JsResult[PsrReportType] = {
      val jsonAsString = json.as[String]
      values.find(_.name == jsonAsString) match {
        case Some(status) => JsSuccess(status)
        case None => JsError(s"Unknown report status: $jsonAsString")
      }
    }
  }
}
