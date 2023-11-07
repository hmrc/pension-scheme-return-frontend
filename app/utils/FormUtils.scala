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

import models.requests.DataRequest
import play.api.data.{Form, FormError}
import play.api.libs.json.Reads
import queries.Gettable

object FormUtils {

  implicit class FormOps[A](form: Form[A]) {
    def fromUserAnswers[B](
      page: Gettable[B]
    )(implicit rds: Reads[B], request: DataRequest[_], transform: Transform[A, B]): Form[A] =
      request.userAnswers.get(page).fold(form)(a => form.fill(transform.from(a)))

    def fromUserAnswers(page: Gettable[A])(implicit rds: Reads[A], request: DataRequest[_]): Form[A] =
      request.userAnswers.get(page).fold(form)(form.fill)

    // removes any additional form errors that use the same key
    val uniqueFormErrors: Form[A] = {
      val formErrors = form.errors.foldLeft[List[FormError]](Nil)(
        (errors, err) => if (errors.map(_.key).contains(err.key)) errors else errors :+ err
      )

      form.copy(errors = formErrors)
    }
  }
}
