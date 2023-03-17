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
import play.api.data.{Form, Mapping}
import uk.gov.hmrc.domain.Nino

import javax.inject.Inject

class TextFormProvider @Inject()() extends Mappings {

  val textAreaRegex = "^[a-zA-Z\\-' ]+$"
  val textAreaMaxLength = 160

  def apply(requiredKey: String): Form[String] =
    Form(
      "value" -> text(requiredKey)
    )

  def textArea(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = validated(
    requiredKey -> args,
    _.verifying(verify[String](invalidCharactersKey, _.matches(textAreaRegex), args: _*)),
    _.verifying(verify[String](tooLongKey, _.length <= textAreaMaxLength, args: _*))
  )

  def nino(
    requiredKey: String,
    invalidKey: String,
    duplicates: List[Nino],
    duplicateKey: String,
    args: Any*
  ): Form[Nino] =
    Form(
      "value" -> text(requiredKey, args.toList)
        .verifying(verify(invalidKey, Nino.isValid, args: _*))
        .verifying(verify[String](duplicateKey, !duplicates.map(_.nino).contains(_), args: _*))
        .transform[Nino](Nino, _.nino)
    )

  private def validated(
    requiredKey: (String, Seq[Any]),
    mappings: (Mapping[String] => Mapping[String])*
  ): Form[String] =
    Form(
      "value" -> mappings.foldLeft[Mapping[String]](text(requiredKey._1, requiredKey._2))(
        (previousMapping, mapping) => mapping(previousMapping)
      )
    )
}
