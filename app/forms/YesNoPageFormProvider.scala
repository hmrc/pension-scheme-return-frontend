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
import play.api.data.Forms.mapping
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.domain.Nino
import uk.gov.voa.play.form.ConditionalMappings

import javax.inject.Inject

class YesNoPageFormProvider @Inject()() {

  protected[forms] val textAreaRegex = """^[a-zA-Z0-9\-'" \t\r\n,.@/]+$"""
  protected[forms] val textAreaMaxLength = 160

  val formKey = "value"

  def apply(
    requiredKey: String,
    invalidKey: String
  ): Form[Boolean] =
    Form("value" -> Mappings.boolean(requiredKey, invalidKey))

  def apply(
    requiredKey: String
  ): Form[Boolean] =
    Form("value" -> Mappings.boolean(requiredKey, ""))

  def apply(
    requiredKey: String,
    args: List[String]
  ): Form[Boolean] =
    Form("value" -> Mappings.boolean(requiredKey, "", args))

  def conditional[No, Yes](
    requiredKey: String,
    mappingNo: Mapping[No],
    mappingYes: Mapping[Yes],
    args: String*
  ): Form[Either[No, Yes]] =
    Form[Either[No, Yes]](
      mapping(
        "value" -> Mappings.boolean(requiredKey, args = args.toList),
        "value.yes" -> ConditionalMappings.mandatoryIfTrue("value", mappingYes),
        "value.no" -> ConditionalMappings.mandatoryIfFalse("value", mappingNo)
      ) {
        case (bool, yes, no) =>
          ((bool, yes, no): @unchecked) match {
            case (false, _, Some(value)) => Left(value)
            case (true, Some(value), _) => Right(value)
          }
      } {
        case Left(value) => Some((false, None, Some(value)))
        case Right(value) => Some((true, Some(value), None))
      }
    )

  def conditionalYes[Yes](
    requiredKey: String,
    mappingYes: Mapping[Yes],
    args: String*
  ): Form[Either[Unit, Yes]] =
    Form[Either[Unit, Yes]](
      mapping(
        "value" -> Mappings.boolean(requiredKey, args = args.toList),
        "value.yes" -> ConditionalMappings.mandatoryIfTrue("value", mappingYes),
        "value.no" -> Mappings.unit
      ) {
        case (bool, yes, _) =>
          ((bool, yes): @unchecked) match {
            case (true, Some(value)) => Right(value)
            case (false, _) => Left(())
          }
      } {
        case Left(_) => Some((false, None, Some(())))
        case Right(value) => Some((true, Some(value), None))
      }
    )

  def ninoDuplicates(
    requiredKey: String,
    invalidKey: String,
    duplicates: List[Nino],
    duplicateKey: String,
    args: Any*
  ): Form[Nino] =
    Form(
      formKey -> Mappings.ninoNoDuplicates(requiredKey, invalidKey, duplicates, duplicateKey, args: _*)
    )
}
