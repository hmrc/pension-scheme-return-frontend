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

package utils

import queries.Settable
import play.api.libs.json.{JsPath, JsValue, Writes}
import models.UserAnswers
import viewmodels.models.Flag

object UserAnswersUtils {

  implicit class UserAnswersOps(val userAnswers: UserAnswers) extends AnyVal {

    def unsafeSet[A](page: Settable[A], value: A)(implicit writes: Writes[A]): UserAnswers =
      userAnswers.set(page, value).get

    def unsafeSet(page: Settable[Flag]): UserAnswers =
      userAnswers.set(page, Flag).get

    def unsafeSet(path: JsPath, value: JsValue): UserAnswers =
      userAnswers.set(path, value).get
  }
}
