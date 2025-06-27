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

package queries

import play.api.libs.json.{__, JsPath}
import models.UserAnswers

import scala.annotation.nowarn
import scala.util.{Success, Try}

sealed trait Query {

  def path: JsPath
}

trait Gettable[A] extends Query

trait Removable[A] extends Query with Cleanup[A]

trait SoftRemovable[A] extends Gettable[A] with Settable[A] with Removable[A]

object SoftRemovable {
  val path: JsPath = __ \ "soft-deleted"
}

trait Settable[A] extends Query with Cleanup[A]

trait Cleanup[A] {
  def cleanup(@nowarn value: Option[A], userAnswers: UserAnswers): Try[UserAnswers] =
    Success(userAnswers)
}
