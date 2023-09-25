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

package viewmodels.govuk

import play.api.data.FormError
import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.textarea.Textarea
import viewmodels.govuk.label._

object textarea extends TextAreaFluency

trait TextAreaFluency {

  object TextareaViewModel {
    def apply(name: String, value: Option[String]): Textarea =
      Textarea(
        id = name,
        name = name,
        value = value
      )
  }

  implicit class FluentTextArea(textArea: Textarea) {

    def withLabelAsHeading(label: Html): Textarea =
      textArea.copy(label = LabelViewModel(HtmlContent(label)).asPageHeading())

    def withRows(rows: Int): Textarea = textArea.copy(rows = rows)

    def withError(message: String): Textarea =
      textArea.copy(
        errorMessage = Some(
          ErrorMessage(content = Text(message))
        )
      )

    def withError(maybeFormError: Option[FormError])(implicit messages: Messages): Textarea =
      maybeFormError.fold(textArea)(error => textArea.withError(messages(error.message, error.args: _*)))

    def withLabel(label: Html): Textarea =
      textArea.copy(
        label = LabelViewModel(
          HtmlContent(label)
        )
      )

    def withLabel(maybeLabel: Option[Html]): Textarea =
      maybeLabel.fold(textArea)(
        label =>
          textArea.copy(
            label = LabelViewModel(
              HtmlContent(label)
            )
          )
      )

    def withHint(hint: Hint): Textarea =
      textArea.copy(hint = Some(hint))

    def withHint(hint: Option[Hint]): Textarea =
      textArea.copy(hint = hint)
  }
}
