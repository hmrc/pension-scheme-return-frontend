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
import models.Enumerable
import models.GenericFormMapper.{ConditionalRadioMapper, StringFieldMapper}
import play.api.data.Forms.mapping
import play.api.data.{Form, Mapping}
import uk.gov.voa.play.form.ConditionalMappings

import javax.inject.Inject

class RadioListFormProvider @Inject() extends Mappings {

  def apply[A: Enumerable](
    requiredKey: String
  ): Form[A] =
    Form("value" -> enumerable(requiredKey, requiredKey))

  // mapping for a list of radios where one can conditionally reveal html
  def singleConditional[A, Conditional](
    requiredKey: String,
    conditionalKey: String,
    conditionalMapping: Mapping[Conditional]
  )(implicit ev: ConditionalRadioMapper[Conditional, A]): Form[A] = {

    val conditional: Map[String, String] => Boolean = _.get("value").contains(conditionalKey)

    Form(
      mapping[A, String, Option[Conditional]](
        "value" -> text(requiredKey),
        "isRequired" -> ConditionalMappings.mandatoryIf[Conditional](conditional, conditionalMapping)
      )((x, y) => ev.to((x, y)))(ev.from)
    )
  }

  def conditionalM[A, Conditional](
    requiredKey: String,
    conditionalMappings: List[(String, Option[Mapping[Conditional]])],
    prePopKey: Option[String] = None
  )(implicit ev: ConditionalRadioMapper[Conditional, A], ev2: StringFieldMapper[Conditional]): Form[A] = {

    def conditionalF(key: String): Map[String, String] => Boolean =
      m => m.get("value").contains(key) && m.contains(s"$key-conditional")

    val c = conditionalMappings.map { case (key, mapping) => conditionalF(key) -> mapping }

    Form(
      mapping[A, String, Option[Conditional]](
        "value" -> text(requiredKey),
        "conditional" -> conditional[Conditional](c, prePopKey)
      )((x, y) => ev.to((x, y)))(ev.from)
    )
  }
}
