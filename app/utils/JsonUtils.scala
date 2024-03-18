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

package utils

import cats.data.NonEmptyList
import cats.syntax.either._
import play.api.libs.json._
import play.api.libs.functional.syntax._

object JsonUtils {
  implicit def nelWrites[A: Writes]: Writes[NonEmptyList[A]] = Writes(nel => Json.toJson(nel.toList))

  implicit class JsObjectOps(json: JsObject) {
    def +?(o: Option[JsObject]): JsObject = o.fold(json)(_ ++ json)
  }

  // Creates a Json format for an either value type
  def eitherFormat[A: Format, B: Format](leftName: String, rightName: String): Format[Either[A, B]] =
    Format(
      fjs = (__ \ leftName).read[A].map(_.asLeft[B]) |
        (__ \ rightName).read[B].map(_.asRight[A]),
      tjs = _.fold(
        left => Json.obj(leftName -> left),
        right => Json.obj(rightName -> right)
      )
    )
}
