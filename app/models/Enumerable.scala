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

import eu.timepit.refined.refineV
import play.api.libs.json._
import eu.timepit.refined.api.{Refined, Validate}

trait Enumerable[A] {

  def get(str: String): Option[A]

  def toList: List[(String, A)]
}

object Enumerable {

  def apply[A](entries: (String, A)*): Enumerable[A] =
    new Enumerable[A] {

      val entriesMap = entries.toMap

      override def get(str: String): Option[A] =
        entriesMap.get(str)

      override def toList: List[(String, A)] = entries.toList
    }

  def index[A](range: Range.Inclusive)(implicit ev: Validate[Int, A]): Enumerable[Refined[Int, A]] =
    Enumerable(
      range.toList
        .map(
          refineV[A](_).fold(
            err => throw new Exception(err),
            index => index
          )
        )
        .map(index => index.value.toString -> index)*
    )

  trait Implicits {

    implicit def reads[A](implicit ev: Enumerable[A]): Reads[A] =
      Reads {
        case JsString(str) =>
          ev.get(str)
            .map { s =>
              JsSuccess(s)
            }
            .getOrElse(JsError("error.invalid"))
        case _ =>
          JsError("error.invalid")
      }

    implicit def writes[A]: Writes[A] =
      Writes(value => JsString(value.toString))
  }
}
