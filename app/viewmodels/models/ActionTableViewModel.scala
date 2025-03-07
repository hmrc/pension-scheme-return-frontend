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

package viewmodels.models

import play.api.mvc.Call
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.Message

sealed trait TableElemBase

case class TableElem(
  text: DisplayMessage,
  hiddenText: Option[Message] = None,
  url: Option[String] = None,
  messageKey: Option[String] = None
) extends TableElemBase

case class TableElemDoubleLink(
  tableElems: (TableElem, TableElem)
) extends TableElemBase

object TableElem {
  val empty: TableElem = TableElem(DisplayMessage.Empty)
  def add(call: Call, hiddenText: Message): TableElem =
    TableElem(Message("site.add"), Some(hiddenText), Some(call.url), Some("site.add"))
  def add(url: String, hiddenText: Message): TableElem =
    TableElem(Message("site.add"), Some(hiddenText), Some(url), Some("site.add"))
  def change(call: Call, hiddenText: Message): TableElem =
    TableElem(Message("site.change"), Some(hiddenText), Some(call.url), Some("site.change"))
  def remove(call: Call, hiddenText: Message): TableElem =
    TableElem(Message("site.remove"), Some(hiddenText), Some(call.url), Some("site.remove"))
  def view(call: Call, hiddenText: Message): TableElem =
    TableElem(Message("site.view"), Some(hiddenText), Some(call.url), Some("site.view"))
}

case class ActionTableViewModel(
  inset: DisplayMessage,
  head: Option[List[TableElem]],
  rows: List[List[TableElemBase]],
  paginatedViewModel: Option[PaginatedViewModel] = None,
  yesHintText: Option[Message] = None,
  showInset: Boolean = false
)
