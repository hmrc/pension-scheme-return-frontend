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

package viewmodels.models

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue, Json}
import utils.WithName

sealed trait SectionStatus {
  val name: String
}

object SectionStatus {
  case object InProgress extends WithName("InProgress") with SectionStatus

  case object Completed extends WithName("Completed") with SectionStatus

  implicit val format: Format[SectionStatus] = new Format[SectionStatus] {
    override def reads(json: JsValue): JsResult[SectionStatus] = json match {
      case JsString(SectionStatus.InProgress.name) => JsSuccess(SectionStatus.InProgress)
      case JsString(SectionStatus.Completed.name) => JsSuccess(SectionStatus.Completed)
      case unknown => JsError(s"Unknown SectionStatus value $unknown")
    }

    override def writes(o: SectionStatus): JsValue = JsString(o.name)
  }
}
