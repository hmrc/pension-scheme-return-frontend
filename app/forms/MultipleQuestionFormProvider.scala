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

package forms

import play.api.data.Forms.mapping
import play.api.data.{Form, Mapping}

object MultipleQuestionFormProvider {

  def apply[A, B, C](
    a: Mapping[A],
    b: Mapping[B],
    c: Mapping[C]
  ): Form[(A, B, C)] =
    Form(
      mapping[(A, B, C), A, B, C](
        "value.1" -> a,
        "value.2" -> b,
        "value.3" -> c
      )(Tuple3.apply)(Tuple3.unapply)
    )

  def apply[A, B](
    a: Mapping[A],
    b: Mapping[B]
  ): Form[(A, B)] =
    Form(
      mapping[(A, B), A, B](
        "value.1" -> a,
        "value.2" -> b
      )(Tuple2.apply)(Tuple2.unapply)
    )
}
