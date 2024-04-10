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

import utils.Transform
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class ConditionalYesNo[No, Yes](value: Either[No, Yes])

object ConditionalYesNo {

  type ConditionalYes[A] = ConditionalYesNo[Unit, A]

  implicit val unitWrites: Writes[Unit] = _ => JsNull

  implicit val unitReads: Reads[Unit] = _ => JsSuccess(())

  def yes[No, Yes](value: Yes): ConditionalYesNo[No, Yes] = ConditionalYesNo(Right(value))
  def no[No, Yes](value: No): ConditionalYesNo[No, Yes] = ConditionalYesNo(Left(value))
  private def reads[No: Reads, Yes: Reads]: Reads[ConditionalYesNo[No, Yes]] =
    (__ \ "no").read[No].map(value => ConditionalYesNo[No, Yes](Left(value))) |
      (__ \ "yes").read[Yes].map(value => ConditionalYesNo[No, Yes](Right(value)))

  private def writes[No: Writes, Yes: Writes]: Writes[ConditionalYesNo[No, Yes]] =
    (o: ConditionalYesNo[No, Yes]) =>
      Json.obj(
        o.value.fold(
          no => "no" -> no,
          yes => "yes" -> yes
        )
      )

  implicit def format[No: Reads: Writes, Yes: Reads: Writes]: Format[ConditionalYesNo[No, Yes]] = Format(reads, writes)

  implicit def transform[No, Yes]: Transform[Either[No, Yes], ConditionalYesNo[No, Yes]] =
    Transform.instance[Either[No, Yes], ConditionalYesNo[No, Yes]](ConditionalYesNo(_), _.value)
}
