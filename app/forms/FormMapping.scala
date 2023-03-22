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

import forms.FormMapping.{BoundFormMapping, FilledFormMapping}
import play.api.data._
import play.api.mvc.Request
import utils.Transform

sealed trait FormMapping[A] { self =>

  def apply(field: String): Field = form(field)

  val mapping: Mapping[A]
  protected val form: Form[A]

  def fill[B](value: Option[B])(implicit transform: Transform[A, B]): FilledFormMapping[A] =
    FilledFormMapping(mapping, value.map(transform.from))

  def bind(data: Map[String, String]): FormMapping[A] =
    BoundFormMapping(mapping, data)

  def bindFromRequest()(implicit request: Request[_], formBinding: FormBinding): FormMapping[A] =
    BoundFormMapping(
      mapping,
      Form(mapping).bindFromRequest().data
    )

  def fold[B, R](error: FormMapping[A] => R, success: B => R)(implicit transform: Transform[A, B]): R =
    form.fold(_ => error(self), a => success(transform.to(a)))

  def fold[R](error: FormMapping[A] => R)(success: A => R): R =
    form.fold(_ => error(self), success)

  lazy val hasErrors: Boolean = form.hasErrors

  lazy val errors: Seq[FormError] = form.errors

  lazy val data: Map[String, String] = form.data
}

object FormMapping {

  def apply[A](mapping: Mapping[A]): FormMapping[A] =
    InitialFormMapping(mapping)

  case class InitialFormMapping[A](
    mapping: Mapping[A]
  ) extends FormMapping[A] {

    protected val form: Form[A] = Form(mapping)
  }

  case class FilledFormMapping[A](
    mapping: Mapping[A],
    filledValue: Option[A]
  ) extends FormMapping[A] {

    protected val form: Form[A] = {
      val f = Form(mapping)
      filledValue.fold(f)(f.fill)
    }
  }

  case class BoundFormMapping[A](
    mapping: Mapping[A],
    boundData: Map[String, String]
  ) extends FormMapping[A] {

    protected val form: Form[A] = Form(mapping).bind(boundData)
  }

}
