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

import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.Message

import scala.collection.immutable.List

case class ListRow(
  text: DisplayMessage,
  change: Option[ListRowLink],
  remove: Option[ListRowLink],
  view: Option[ViewOnlyLink],
  check: Option[ListRowLink]
)

sealed trait ViewOnlyLink

case class ListRowNoLink(text: DisplayMessage) extends ViewOnlyLink
case class ListRowLink(url: String, hiddenText: Message) extends ViewOnlyLink

object ListRow {
  def apply(
    text: DisplayMessage,
    changeUrl: String,
    changeHiddenText: Message,
    removeUrl: String,
    removeHiddenText: Message
  ): ListRow = ListRow(
    text,
    change = Some(ListRowLink(changeUrl, changeHiddenText)),
    remove = Some(ListRowLink(removeUrl, removeHiddenText)),
    view = None,
    check = None
  )

  def view(text: DisplayMessage, url: String, hiddenText: Message): ListRow =
    ListRow(
      text,
      change = None,
      remove = None,
      view = Some(ListRowLink(url, hiddenText)),
      check = None
    )

  def check(text: DisplayMessage, url: String, hiddenText: Message): ListRow =
    ListRow(
      text,
      change = None,
      remove = None,
      view = None,
      check = Some(ListRowLink(url, hiddenText))
    )

  def viewNoLink(text: DisplayMessage, value: DisplayMessage): ListRow =
    ListRow(
      text,
      change = None,
      remove = None,
      view = Some(ListRowNoLink(value)),
      check = None
    )
}

case class ListViewModel(
  inset: DisplayMessage,
  sections: List[Section],
  radioText: Message,
  // whether to render the radio buttons to add another entity to the list or continue
  showRadios: Boolean = true,
  paginatedViewModel: Option[PaginatedViewModel] = None,
  yesHintText: Option[Message] = None,
  showInsetWithRadios: Boolean = false
)

case class Section(label: Option[DisplayMessage], rows: List[ListRow])

object Section {
  def apply(rows: List[ListRow]): Section =
    Section(label = None, rows)

  def heading(label: DisplayMessage, rows: List[ListRow]): Section =
    Section(Some(label), rows)
}
