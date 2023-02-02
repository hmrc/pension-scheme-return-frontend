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

import play.api.i18n.Messages

sealed trait DisplayMessage

object DisplayMessage {
  case class SimpleMessage(key: String, args: List[Any]) extends DisplayMessage {
    def toMessage(implicit messages: Messages): String = messages(key, args: _*)
  }

  object SimpleMessage {
    def apply(key: String): SimpleMessage = new SimpleMessage(key, List())

    def apply(key: String, args: Any*): SimpleMessage = new SimpleMessage(key, args.toList)
  }

  case class ComplexMessage(elements: List[ComplexMessageElement], delimiter: Delimiter) extends DisplayMessage

  object ComplexMessage {
    def apply(elements: ComplexMessageElement*): ComplexMessage = ComplexMessage(elements.toList, Delimiter.SingleSpace)
  }
}

sealed trait ComplexMessageElement

object ComplexMessageElement {
  case class Message(key: String, args: List[Any] = Nil) extends ComplexMessageElement
  case class LinkedMessage(key: String, url: String, args: List[Any] = Nil) extends ComplexMessageElement
}

sealed trait Delimiter

object Delimiter {
  case object SingleSpace extends Delimiter
  case object Newline extends Delimiter
}
