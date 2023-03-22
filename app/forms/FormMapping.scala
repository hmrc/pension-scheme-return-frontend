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

import play.api.data.{Form, FormBinding, Mapping}
import play.api.mvc.Request

case class FormMapping[A, B](
  mapping: Mapping[A],
  filledValue: Option[B],
  apply: A => B,
  unapply: B => A
) { self =>

  val form: Form[B] = Form(mapping.transform(apply, unapply))

  val filledForm: Form[B] =
    filledValue.fold(form)(form.fill)

  def fill(value: Option[B]): FormMapping[A, B] = copy(filledValue = value)

  def bind(data: Map[String, String]): Form[B] = form.bind(data)

  def bindFromRequest(implicit request: Request[_], formBinding: FormBinding): Form[B] = form.bindFromRequest()

  def transform[C](apply: B => C, unapply: C => B): FormMapping[A, C] =
    FormMapping[A, C](
      mapping,
      filledValue.map(apply),
      self.apply.andThen(apply),
      unapply.andThen(self.unapply)
    )
}
