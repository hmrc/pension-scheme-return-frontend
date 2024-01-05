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

package forms.mappings.errors

import config.Constants._
import forms.mappings.Regex

case class InputFormErrors(
  requiredKey: String,
  regexChecks: List[(Regex, String)],
  max: (Int, String),
  args: Any*
)

object InputFormErrors {

  def input(
    requiredKey: String,
    invalidCharactersKey: String,
    maxError: String,
    args: Any*
  ): InputFormErrors = InputFormErrors(
    requiredKey,
    List((textAreaRegex, invalidCharactersKey)),
    (maxInputLength, maxError),
    args: _*
  )

  def genericInput(
    requiredKey: String,
    invalidCharactersKey: String,
    maxError: String,
    regex: String,
    maxLength: Int,
    args: Any*
  ): InputFormErrors = InputFormErrors(
    requiredKey,
    List((regex, invalidCharactersKey)),
    (maxLength, maxError),
    args: _*
  )

  def textArea(
    requiredKey: String,
    invalidCharactersKey: String,
    maxError: String,
    args: Any*
  ): InputFormErrors = InputFormErrors(
    requiredKey,
    List((textAreaRegex, invalidCharactersKey)),
    (maxTextAreaLength, maxError),
    args: _*
  )

  def postcode(
    requiredKey: String,
    invalidCharactersKey: String,
    invalidFormatKey: String,
    args: Any*
  ): InputFormErrors =
    InputFormErrors(
      requiredKey,
      List(
        (postcodeCharsRegex, invalidCharactersKey),
        (postcodeFormatRegex, invalidFormatKey)
      ),
      (maxTextAreaLength, invalidFormatKey),
      args: _*
    )
}
