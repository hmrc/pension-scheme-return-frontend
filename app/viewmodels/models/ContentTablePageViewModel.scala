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
import viewmodels.DisplayMessage.{InlineMessage, Message}

case class ContentTablePageViewModel(
  inset: Option[DisplayMessage], // will be displayed under the table
  beforeTable: Option[DisplayMessage],
  afterTable: Option[DisplayMessage],
  rows: List[(DisplayMessage, DisplayMessage)]
)

object ContentTablePageViewModel {

  def apply(
    inset: DisplayMessage,
    rows: List[(DisplayMessage, DisplayMessage)]
  ): ContentTablePageViewModel =
    ContentTablePageViewModel(Some(inset), beforeTable = None, afterTable = None, rows)

  def apply(
    title: Message,
    heading: InlineMessage,
    inset: DisplayMessage,
    buttonText: Message,
    onSubmit: Call,
    rows: (DisplayMessage, DisplayMessage)*
  ): FormPageViewModel[ContentTablePageViewModel] =
    FormPageViewModel(
      title,
      heading,
      ContentTablePageViewModel(inset, rows.toList),
      onSubmit
    ).withButtonText(buttonText)
}
