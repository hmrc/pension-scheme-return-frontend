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

import utils.WithName
import play.api.mvc.JavascriptLiteral

sealed trait ManualOrUpload {
  val name: String

  def fold[A](manual: => A, upload: => A): A = this match {
    case ManualOrUpload.Manual => manual
    case ManualOrUpload.Upload => upload
  }
}
object ManualOrUpload extends Enumerable.Implicits {

  case object Manual extends WithName("manual") with ManualOrUpload
  case object Upload extends WithName("upload") with ManualOrUpload

  val values: List[ManualOrUpload] = List(Manual, Upload)

  implicit val enumerable: Enumerable[ManualOrUpload] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val jsLiteral: JavascriptLiteral[ManualOrUpload] = (value: ManualOrUpload) => value.name
}
