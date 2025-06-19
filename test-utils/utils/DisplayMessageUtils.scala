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

package utils

import viewmodels.DisplayMessage
import viewmodels.DisplayMessage._

trait DisplayMessageUtils {

  def messageKey(message: DisplayMessage, separator: String = ""): String =
    allMessages(message).foldLeft("")(_ + separator + _.key).trim

  def allMessages(message: DisplayMessage): List[Message] = message match {
    case Empty => List()
    case m: Message => List(m)
    case CompoundMessage(left, right) => allMessages(left) ++ allMessages(right)
    case LinkMessage(message, _, _, _) => List(message)
    case DownloadLinkMessage(message, _) => List(message)
    case ParagraphMessage(messages) => messages.toList.flatMap(allMessages)
    case TableMessage(contents, heading) =>
      contents.foldLeft(List[Message]()) { case (acc, (headers, contents)) =>
        allMessages(headers) ++ allMessages(contents) ++ acc
      } ++ heading.toList.flatMap { case (k, v) => allMessages(k) ++ allMessages(v) }
    case ListMessage(messages, _) => messages.toList.flatMap(allMessages)
    case Heading2(content, _) => allMessages(content)
    case _ => throw new Exception("Unrecognised DisplayMessage type")
  }
}
