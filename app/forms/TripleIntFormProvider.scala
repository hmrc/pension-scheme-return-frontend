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

import forms.mappings.Mappings
import forms.mappings.errors.IntFormErrors
import models.SchemeMemberNumbers
import play.api.data.{Form, Mapping}
import play.api.data.Forms.mapping

import javax.inject.Inject

class TripleIntFormProvider @Inject() extends Mappings {

  def apply(
    field1Errors: IntFormErrors,
    field2Errors: IntFormErrors,
    field3Errors: IntFormErrors,
    args: Seq[String] = Seq.empty
  ): Form[(Int, Int, Int)] =
    Form(
      formMapping[(Int, Int, Int)](
        field1Errors,
        field2Errors,
        field3Errors,
        args
      )(Tuple3.apply)(Some(_))
    )

  def schemeMembers(
    activeMembersErrors: IntFormErrors,
    deferredMembersErrors: IntFormErrors,
    pensionerMemberErrors: IntFormErrors,
    args: Seq[String] = Seq.empty
  ): Form[SchemeMemberNumbers] =
    Form(
      formMapping(
        activeMembersErrors,
        deferredMembersErrors,
        pensionerMemberErrors,
        args
      )(SchemeMemberNumbers.apply)(SchemeMemberNumbers.unapply)
    )

  private def formMapping[T](
    field1Errors: IntFormErrors,
    field2Errors: IntFormErrors,
    field3Errors: IntFormErrors,
    args: Seq[String] = Seq.empty
  )(apply: (Int, Int, Int) => T)(unapply: T => Option[(Int, Int, Int)]): Mapping[T] =
    mapping[T, Int, Int, Int](
      "value.1" -> int(field1Errors, args),
      "value.2" -> int(field2Errors, args),
      "value.3" -> int(field3Errors, args)
    )(apply)(unapply)
}
