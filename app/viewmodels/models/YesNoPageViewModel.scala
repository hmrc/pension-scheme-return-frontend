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
import viewmodels.DisplayMessage.{BlockMessage, CompoundMessage, LinkMessage, ListMessage, Message, ParagraphMessage}
import viewmodels.implicits._

case class YesNoPageViewModel(
  title: Message,
  heading: Message,
  description: List[DisplayMessage],
  legend: Option[Message] = None,
  hint: Option[Message] = None,
  yes: Option[Message] = None,
  no: Option[Message] = None,
  refresh: Option[Int] = None,
  details: Option[DisplayMessage] = None,
  onSubmit: Call
) {

  def withDescription(message: BlockMessage, messages: BlockMessage*): YesNoPageViewModel =
    copy(description = description ++ (message :: messages.toList))

  def withHint(message: Message): YesNoPageViewModel =
    copy(hint = Some(message))
}

object YesNoPageViewModel {

  def apply(title: Message, heading: Message, onSubmit: Call, details: Option[DisplayMessage]): YesNoPageViewModel =
    YesNoPageViewModel(
      title,
      heading,
      List(),
      None,
      None,
      None,
      None,
      None,
      details,
      onSubmit = onSubmit
    )

  def apply(title: Message, heading: Message, onSubmit: Call): YesNoPageViewModel =
    YesNoPageViewModel(
      title,
      heading,
      List(),
      None,
      None,
      None,
      None,
      None,
      None,
      onSubmit = onSubmit
    )
}
