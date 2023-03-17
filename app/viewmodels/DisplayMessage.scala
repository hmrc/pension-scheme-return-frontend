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

package viewmodels

import cats.data.NonEmptyList
import play.api.i18n.Messages

sealed trait DisplayMessage

object DisplayMessage {

  sealed trait InlineMessage extends DisplayMessage

  sealed trait BlockMessage extends DisplayMessage

  case class Message(key: String, args: List[Message]) extends InlineMessage {

    def toMessage(implicit messages: Messages): String =
      messages(key, args.map(_.toMessage): _*)
  }

  object Message {

    def apply(key: String, args: Message*): Message =
      Message(key, args.toList)
  }

  case class LinkMessage(content: Message, url: String) extends InlineMessage

  case class ParagraphMessage(content: NonEmptyList[InlineMessage]) extends BlockMessage

  object ParagraphMessage {

    def apply(headContent: InlineMessage, tailContents: InlineMessage*): ParagraphMessage =
      ParagraphMessage(NonEmptyList(headContent, tailContents.toList))
  }

  case class ListMessage(content: NonEmptyList[InlineMessage], listType: ListType) extends BlockMessage

  object ListMessage {

    def apply(listType: ListType, headContent: InlineMessage, tailContents: InlineMessage*): ListMessage =
      ListMessage(NonEmptyList(headContent, tailContents.toList), listType)
  }

  sealed trait ListType

  object ListType {
    case object Bullet extends ListType
    case object NewLine extends ListType
  }
}
