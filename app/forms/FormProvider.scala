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

import play.api.data.FieldMapping
import play.api.data.Forms.mapping

abstract class FormProvider[E, A](fieldMapping: (E, Seq[String]) => FieldMapping[A]) {

  def apply(
    errors: E,
    args: Seq[String]
  ): FormMapping[A, A] =
    FormMapping(
      mapping("value" -> fieldMapping(errors, args))(identity)(a => Some(identity(a))),
      identity,
      identity
    )

  def apply(
    errors1: E,
    errors2: E,
    args: Seq[String]
  ): FormMapping[(A, A), (A, A)] =
    FormMapping(
      mapping[(A, A), A, A](
        "value.1" -> fieldMapping(errors1, args),
        "value.2" -> fieldMapping(errors2, args)
      )(Tuple2.apply)(Tuple2.unapply),
      identity,
      identity
    )
}
