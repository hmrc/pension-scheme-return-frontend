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

import play.api.mvc.Call
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{InlineMessage, Message}

case class RadioListViewModel(
  legend: Option[Message],
  items: List[RadioListRow],
  hint: Option[Message] = None
) {
  def withHint(message: Message): RadioListViewModel =
    copy(hint = Some(message))
}

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

sealed trait RadioListRow

case class RadioItemConditional(
  fieldType: FieldType,
  label: Option[DisplayMessage]
)

case class RadioListRowViewModel(
  content: Message,
  value: Option[String],
  divider: Option[String],
  hint: Option[Message],
  conditional: Option[RadioItemConditional] = None
) extends RadioListRow

case class RadioListRowDivider(dividerText: String) extends RadioListRow

object RadioListRowDivider {
  val Or: RadioListRowDivider = RadioListRowDivider("site.or")
}

object RadioListRowViewModel {

  def apply(content: Message, value: String): RadioListRowViewModel =
    RadioListRowViewModel(content, Some(value), None, None)

  def apply(content: Message, value: String, hint: Message): RadioListRowViewModel =
    RadioListRowViewModel(content, Some(value), None, Some(hint))

  def conditional(
    content: Message,
    value: String,
    hint: Option[Message],
    conditional: RadioItemConditional
  ): RadioListRowViewModel =
    RadioListRowViewModel(content, Some(value), None, hint, Some(conditional))
}
