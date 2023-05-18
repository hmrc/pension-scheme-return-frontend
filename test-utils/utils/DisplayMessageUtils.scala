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

package utils

import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{
  CompoundMessage,
  DownloadLinkMessage,
  Empty,
  LinkMessage,
  ListMessage,
  Message,
  ParagraphMessage,
  TableMessage
}

trait DisplayMessageUtils {

  def messageKey(message: DisplayMessage): String =
    allMessages(message).foldLeft("")(_ + _.key)

  def allMessages(message: DisplayMessage): List[Message] = message match {
    case Empty => List()
    case m: Message => List(m)
    case CompoundMessage(left, right) => allMessages(left) ++ allMessages(right)
    case LinkMessage(message, _) => List(message)
    case DownloadLinkMessage(message, _) => List(message)
    case ParagraphMessage(messages) => messages.toList.flatMap(allMessages)
    case TableMessage(contents) =>
      contents.foldLeft(List[Message]()) {
        case (acc, (headers, contents)) =>
          allMessages(headers) ++ allMessages(contents) ++ acc
      }
    case ListMessage(messages, _) => messages.toList.flatMap(allMessages)
  }
}
