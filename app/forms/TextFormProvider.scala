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

package forms

import forms.mappings.Mappings
import config.Constants.{maxInputLength, maxTextAreaLength}
import uk.gov.hmrc.domain.Nino
import play.api.data.Form

import javax.inject.Inject

class TextFormProvider @Inject() {

  protected[forms] val nameRegex = "^[a-zA-Z\\-' ]+$"
  protected[forms] val multipleNamesRegex = "^[a-zA-Z\\-' ,]+$"
  protected[forms] val textAreaRegex = """^[a-zA-Z0-9\-’`'" \t,.@/&()]+$"""
  protected[forms] val psaIdRegex = "^(A[0-9]{7})$"
  protected[forms] val psaIdMaxLength = 8

  val formKey = "value"

  def apply(requiredKey: String): Form[String] =
    Form(
      formKey -> Mappings.text(requiredKey)
    )

  def textArea(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List((textAreaRegex, invalidCharactersKey)),
      maxTextAreaLength,
      tooLongKey,
      args*
    )
  )

  def textAreaShorter(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List((textAreaRegex, invalidCharactersKey)),
      maxInputLength,
      tooLongKey,
      args*
    )
  )

  def nino(
    requiredKey: String,
    invalidKey: String,
    duplicates: List[Nino],
    duplicateKey: String,
    args: Any*
  ): Form[Nino] =
    Form(
      formKey -> Mappings.ninoNoDuplicates(requiredKey, invalidKey, duplicates, duplicateKey, args*)
    )

  def name(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List((nameRegex, invalidCharactersKey)),
      maxTextAreaLength,
      tooLongKey,
      args*
    )
  )

  def multipleNames(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List((multipleNamesRegex, invalidCharactersKey)),
      maxTextAreaLength,
      tooLongKey,
      args*
    )
  )

  def text(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedText(
      requiredKey,
      List((textAreaRegex, invalidCharactersKey)),
      maxTextAreaLength,
      tooLongKey,
      args*
    )
  )

  def psaId(
    requiredKey: String,
    tooLongKey: String,
    invalidCharactersKey: String,
    invalidNoMatchKey: String,
    authorisingPSAID: Option[String],
    args: Any*
  ): Form[String] = Form(
    formKey -> Mappings.validatedPsaId(
      requiredKey,
      List((psaIdRegex, invalidCharactersKey)),
      psaIdMaxLength,
      tooLongKey,
      authorisingPSAID: Option[String],
      invalidNoMatchKey: String,
      args*
    )
  )
}
