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
import viewmodels.DisplayMessage.{LinkMessage, Message}

case class TableElem(
  text: DisplayMessage,
  hiddenText: Option[Message] = None
)

object TableElem {
  val empty: TableElem = TableElem(DisplayMessage.Empty)
  def add(call: Call): TableElem = TableElem(LinkMessage(Message("site.add"), call.url))
  def change(call: Call): TableElem = TableElem(LinkMessage(Message("site.change"), call.url))
  def remove(call: Call): TableElem = TableElem(LinkMessage(Message("site.remove"), call.url))
}

case class ActionTableViewModel(
  inset: DisplayMessage,
  head: Option[List[TableElem]],
  rows: List[List[TableElem]],
  radioText: Message,
  // whether to render the radio buttons to add another entity to the list or continue
  showRadios: Boolean = true,
  paginatedViewModel: Option[PaginatedViewModel] = None,
  yesHintText: Option[Message] = None,
  showInsetWithRadios: Boolean = false,
  // whether to render the inset text
  showInset: Boolean = false
)
