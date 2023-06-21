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
  items: List[RadioListRow]
)

object RadioListViewModel {

  def apply(
    title: Message,
    heading: InlineMessage,
    items: List[RadioListRow],
    onSubmit: Call
  ): FormPageViewModel[RadioListViewModel] =
    FormPageViewModel(
      title,
      heading,
      RadioListViewModel(None, items),
      onSubmit
    )
}

sealed trait RadioListRow {
  def radioListRow(implicit messages: Messages): RadioItem
}

case class RadioListRowViewModel(
  content: Message,
  value: Option[String],
  divider: Option[String],
  hint: Option[Message]
) extends RadioListRow {

  override def radioListRow(implicit messages: Messages): RadioItem =
    RadioItem(
      content = Text(content.toMessage),
      value = value,
      hint = hint.map(h => Hint(content = Text(h.toMessage)))
    )
}

case class RadioListRowDivider(dividerText: String) extends RadioListRow {

  override def radioListRow(implicit messages: Messages): RadioItem =
    RadioItem(
      divider = Some(messages(dividerText))
    )
}

object RadioListRowDivider {
  val Or: RadioListRowDivider = RadioListRowDivider("site.or")
}

object RadioListRowViewModel {

  def apply(content: Message, value: String): RadioListRowViewModel =
    RadioListRowViewModel(content, Some(value), None, None)

  def apply(content: Message, value: String, hint: Message): RadioListRowViewModel =
    RadioListRowViewModel(content, Some(value), None, Some(hint))
}
