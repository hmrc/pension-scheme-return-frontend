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
import viewmodels.DisplayMessage._

case class ContentPageViewModel(
  title: SimpleMessage,
  heading: SimpleMessage,
  paragraphs: List[SimpleMessage],
  listItems: List[SimpleMessage],
  buttonText: SimpleMessage,
  isStartButton: Boolean,
  onSubmit: Call
)

object ContentPageViewModel {

  def apply(
    title: String,
    heading: String,
    buttonText: String,
    isStartButton: Boolean,
    onSubmit: Call,
    paragraphs: String*
  ): ContentPageViewModel =
    ContentPageViewModel(title, heading, paragraphs.toList, buttonText, isStartButton, onSubmit)

  def apply(
    title: String,
    heading: String,
    paragraphs: List[String],
    buttonText: String,
    isStartButton: Boolean,
    onSubmit: Call
  ): ContentPageViewModel =
    ContentPageViewModel(
      SimpleMessage(title),
      SimpleMessage(heading),
      paragraphs.map(SimpleMessage(_)),
      List(),
      SimpleMessage(buttonText),
      isStartButton,
      onSubmit
    )
}
