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

package viewmodels.models

import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.Aliases.{Hint, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import viewmodels.DisplayMessage.{InlineMessage, Message}

case class RadioListViewModel(
  legend: Option[Message],
  items: List[RadioListRowViewModel],
)

object RadioListViewModel {

  def apply(title: Message, heading: InlineMessage, items: List[RadioListRowViewModel], onSubmit: Call): PageViewModel[RadioListViewModel] =
    PageViewModel(
      title,
      heading,
      RadioListViewModel(None, items),
      onSubmit
    )
}

case class RadioListRowViewModel(
  content: Message,
  value: String,
  hint: Option[Message]
) {

  def radioListRow(implicit messages: Messages): RadioItem =
    RadioItem(
      content = Text(content.toMessage),
      value = Some(value),
      hint = hint.map(h => Hint(content = Text(h.toMessage)))
    )
}

object RadioListRowViewModel {

  def apply(content: Message, value: String): RadioListRowViewModel =
    RadioListRowViewModel(content, value, None)

  def apply(content: Message, value: String, hint: Message): RadioListRowViewModel =
    RadioListRowViewModel(content, value, Some(hint))
}
