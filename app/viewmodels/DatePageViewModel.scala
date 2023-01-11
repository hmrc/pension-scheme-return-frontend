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

case class DisplayMessage(key: String, args: List[Any]) {

  def toMessage(implicit messages: Messages) = messages(key, args: _*)
}

object DisplayMessage {
  def apply(key: String): DisplayMessage = DisplayMessage(key, List())
  def apply(key: String, args: Any*): DisplayMessage = DisplayMessage(key, args.toList)
}

case class PensionSchemeModel(
  title: DisplayMessage,
  legend: DisplayMessage,
  hint: DisplayMessage,
  //onSubmit: UrlParams => Call
)