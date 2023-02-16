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

import config.Refined.{Max10, OneToTen}
import eu.timepit.refined.refineV
import models.SchemeId.Srn
import play.api.mvc.{JavascriptLiteral, PathBindable}

object Binders {

  implicit val srnBinder: PathBindable[Srn] = new PathBindable[Srn] {

    override def bind(key: String, value: String): Either[String, Srn] = {
      Srn(value).toRight("Invalid scheme reference number")
    }

    override def unbind(key: String, value: Srn): String = value.value
  }

  implicit val max10PathBinder: PathBindable[Max10] =
    new PathBindable[Max10] {
      override def bind(key: String, value: String): Either[String, Max10] = value
        .toIntOption
        .toRight(s"value for key $key was not an Integer")
        .flatMap(refineV[OneToTen](_))

      override def unbind(key: String, value: Max10): String = value.value.toString
    }

  implicit val max10JSLiteral: JavascriptLiteral[Max10] =
    (value: Max10) => value.value.toString
}