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

import utils.WithName
import play.api.mvc.Call
import play.api.libs.json._

/**
 * Defines the completion state of a journey within a section
 */
sealed trait SectionJourneyStatus extends Product with Serializable {
  val inProgress: Boolean
  val completed: Boolean
}

object SectionJourneyStatus {
  case class InProgress(url: String) extends WithName(InProgress.name) with SectionJourneyStatus {
    val inProgress: Boolean = true
    val completed: Boolean = false
  }

  object InProgress extends WithName("JourneyInProgress") {
    def apply(call: Call) = new InProgress(call.url)
  }

  case object Completed extends WithName("JourneyCompleted") with SectionJourneyStatus {
    val inProgress: Boolean = false
    val completed: Boolean = true
  }

  implicit val format: Format[SectionJourneyStatus] = new Format[SectionJourneyStatus] {
    override def reads(json: JsValue): JsResult[SectionJourneyStatus] =
      (__ \ "status")
        .read[String]
        .flatMap[SectionJourneyStatus] {
          case InProgress.name => (__ \ "url").read[String].map(SectionJourneyStatus.InProgress(_))
          case Completed.name => Reads.pure(SectionJourneyStatus.Completed)
          case _ => Reads(_ => JsError("Unknown status"))
        }
        .reads(json)

    override def writes(o: SectionJourneyStatus): JsValue = o match {
      case InProgress(current) =>
        Json.obj(
          "status" -> Json.toJson(SectionJourneyStatus.InProgress.name),
          "url" -> Json.toJson(current)
        )
      case Completed =>
        Json.obj(
          "status" -> Json.toJson(SectionJourneyStatus.Completed.name)
        )
    }
  }
}
