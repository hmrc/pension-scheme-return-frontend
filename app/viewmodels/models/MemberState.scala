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
import play.api.libs.json._

sealed trait MemberState {
  val name: String
  val active: Boolean = this == MemberState.New || this == MemberState.Changed
  val changed: Boolean = this == MemberState.Changed
  val _new: Boolean = this == MemberState.New
}

object MemberState {
  case object New extends WithName("New") with MemberState

  case object Changed extends WithName("Changed") with MemberState

  case object Deleted extends WithName("Deleted") with MemberState

  implicit val format: Format[MemberState] = new Format[MemberState] {
    override def reads(json: JsValue): JsResult[MemberState] = json match {
      case JsString(MemberState.New.name) => JsSuccess(MemberState.New)
      case JsString(MemberState.Changed.name) => JsSuccess(MemberState.Changed)
      case JsString(MemberState.Deleted.name) => JsSuccess(MemberState.Deleted)
      case unknown => JsError(s"Unknown MemberState value $unknown")
    }

    override def writes(o: MemberState): JsValue = JsString(o.name)
  }
}
