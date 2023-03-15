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
import viewmodels.DisplayMessage.Message

case class ContentTablePageViewModel(
  title: Message,
  heading: DisplayMessage,
  rows: List[(DisplayMessage, DisplayMessage)],
  inset: DisplayMessage,
  buttonText: DisplayMessage,
  onSubmit: Call
)

object ContentTablePageViewModel {

  def apply(
    title: String,
    heading: DisplayMessage,
    inset: DisplayMessage,
    buttonText: DisplayMessage,
    onSubmit: Call,
    rows: (DisplayMessage, DisplayMessage)*
  ): ContentTablePageViewModel =
    ContentTablePageViewModel(
      Message(title),
      heading,
      rows.toList,
      inset,
      buttonText,
      onSubmit
    )
}
