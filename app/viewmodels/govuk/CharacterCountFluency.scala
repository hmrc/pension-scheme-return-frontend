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

package viewmodels.govuk

import uk.gov.hmrc.hmrcfrontend.views.viewmodels.charactercount.CharacterCount
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import viewmodels.govuk.all.{FluentLabel, LabelViewModel}
import play.api.i18n.Messages
import viewmodels.ErrorMessageAwareness
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import play.api.data.{Field, FormError}

object characterCount extends CharacterCountFluency

trait CharacterCountFluency {

  object CharacterCountViewModel extends ErrorMessageAwareness {

    def apply(
      field: Field
    )(implicit messages: Messages): CharacterCount =
      CharacterCount(
        id = field.name,
        name = field.name,
        value = field.value,
        errorMessage = errorMessage(field)
      )
  }

  implicit class FluentCharacterCount(characterCount: CharacterCount) {

    def withLabel(label: Html): CharacterCount =
      characterCount.copy(
        label = LabelViewModel(
          HtmlContent(label)
        )
      )

    def withLabel(maybeLabel: Option[Html]): CharacterCount =
      maybeLabel.fold(characterCount)(
        label =>
          characterCount.copy(
            label = LabelViewModel(
              HtmlContent(label)
            )
          )
      )

    def withLabelAsHeading(label: Html): CharacterCount =
      characterCount.copy(label = LabelViewModel(HtmlContent(label)).asPageHeading())

    def withMaxLength(maxLength: Int): CharacterCount =
      characterCount.copy(maxLength = Some(maxLength))

    def withId(id: String): CharacterCount =
      characterCount.copy(id = id)

    def withHint(hint: Option[Hint]): CharacterCount =
      characterCount.copy(hint = hint)

    def withCssClass(newClass: String): CharacterCount =
      characterCount.copy(classes = s"${characterCount.classes} $newClass")

    def withAttribute(attribute: (String, String)): CharacterCount =
      characterCount.copy(attributes = characterCount.attributes + attribute)

    def withSpellcheck(on: Boolean = true): CharacterCount =
      characterCount.copy(spellcheck = Some(on))

    def withSize(size: String): CharacterCount =
      characterCount.withCssClass(size)

    def withRows(rows: Int): CharacterCount =
      characterCount.copy(rows = rows)

    def withError(message: String): CharacterCount =
      characterCount.copy(
        errorMessage = Some(
          ErrorMessage(content = Text(message))
        )
      )

    def withError(maybeFormError: Option[FormError])(implicit messages: Messages): CharacterCount =
      maybeFormError.fold(characterCount)(error => characterCount.withError(messages(error.message, error.args: _*)))

    def withThreshold(threshold: Int): CharacterCount =
      characterCount.copy(threshold = Some(threshold))
  }
}
