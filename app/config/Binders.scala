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

package config

import play.api.mvc.PathBindable
import models.SchemeId.Srn
import models.IdentitySubject

object Binders {

  implicit val srnBinder: PathBindable[Srn] = new PathBindable[Srn] {

    override def bind(key: String, value: String): Either[String, Srn] =
      Srn(value).toRight("Invalid scheme reference number")

    override def unbind(key: String, value: Srn): String = value.value
  }

  implicit val identitySubjectBinder: PathBindable[IdentitySubject] = new PathBindable[IdentitySubject] {

    override def bind(key: String, value: String): Either[String, IdentitySubject] =
      Right(IdentitySubject.withNameWithDefault(value))

    override def unbind(key: String, value: IdentitySubject): String = value.name
  }
}
