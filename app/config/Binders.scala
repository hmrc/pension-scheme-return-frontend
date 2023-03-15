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

package config

import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import models.SchemeId.Srn
import play.api.mvc.{JavascriptLiteral, PathBindable}

object Binders {

  implicit val srnBinder: PathBindable[Srn] = new PathBindable[Srn] {

    override def bind(key: String, value: String): Either[String, Srn] =
      Srn(value).toRight("Invalid scheme reference number")

    override def unbind(key: String, value: Srn): String = value.value
  }

  implicit def refinedIntPathBinder[T](implicit ev: Validate[Int, T]): PathBindable[Refined[Int, T]] =
    new PathBindable[Refined[Int, T]] {
      override def bind(key: String, value: String): Either[String, Refined[Int, T]] =
        value.toIntOption
          .toRight(s"value for key $key was not an Integer")
          .flatMap(refineV[T](_))

      override def unbind(key: String, value: Refined[Int, T]): String = value.value.toString
    }

  implicit def refinedIntJSLiteral[T]: JavascriptLiteral[Refined[Int, T]] =
    (value: Refined[Int, T]) => value.value.toString
}
