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

import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.Transform

case class ConditionalYesNo[A](value: Either[String, A])

object ConditionalYesNo {
  private def reads[A: Reads]: Reads[ConditionalYesNo[A]] =
    (__ \ "no").read[String].map(value => ConditionalYesNo[A](Left(value))) |
      (__ \ "yes").read[A].map(value => ConditionalYesNo[A](Right(value)))

  private def writes[A: Writes]: Writes[ConditionalYesNo[A]] =
    (o: ConditionalYesNo[A]) =>
      Json.obj(
        o.value.fold(
          no => "no" -> no,
          yes => "yes" -> yes
        )
      )

  implicit def format[A: Reads: Writes]: Format[ConditionalYesNo[A]] = Format(reads, writes)

  implicit def transform[A]: Transform[Either[String, A], ConditionalYesNo[A]] =
    Transform.instance[Either[String, A], ConditionalYesNo[A]](ConditionalYesNo(_), _.value)
}
