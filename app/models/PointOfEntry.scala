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

import play.api.libs.json._
import utils.WithName

sealed trait PointOfEntry {
  val pointOfEntry: String
}

object PointOfEntry {
  case object HowWereSharesDisposedPointOfEntry
      extends WithName("HowWereSharesDisposedPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "HowWereSharesDisposedPointOfEntry"
  }

  case object HowWereBondsDisposedPointOfEntry extends WithName("HowWereBondsDisposedPointOfEntry") with PointOfEntry {
    val pointOfEntry = "HowWereBondsDisposedPointOfEntry"
  }

  case object WhoWereTheSharesSoldToPointOfEntry
      extends WithName("WhoWereTheSharesSoldToPointOfEntry")
      with PointOfEntry {
    val pointOfEntry = "WhoWereTheSharesSoldToPointOfEntry"
  }

  case object NoPointOfEntry extends WithName("NoPointOfEntry") with PointOfEntry {
    val pointOfEntry = "NoPointOfEntry"
  }

  implicit val format: Format[PointOfEntry] = new Format[PointOfEntry] {
    override def reads(json: JsValue): JsResult[PointOfEntry] = json match {
      case JsString(HowWereSharesDisposedPointOfEntry.name) => JsSuccess(HowWereSharesDisposedPointOfEntry)
      case JsString(WhoWereTheSharesSoldToPointOfEntry.name) => JsSuccess(WhoWereTheSharesSoldToPointOfEntry)
      case JsString(HowWereBondsDisposedPointOfEntry.name) => JsSuccess(HowWereBondsDisposedPointOfEntry)
      case JsString(NoPointOfEntry.name) => JsSuccess(NoPointOfEntry)
      case unknown => JsError(s"Unknown PointOfEntry value: $unknown")
    }

    override def writes(o: PointOfEntry): JsValue = JsString(o.pointOfEntry)
  }
}
