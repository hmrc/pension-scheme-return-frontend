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

case class ContentPageViewModel(
  title: DisplayMessage,
  heading: DisplayMessage,
  paragraphs: List[DisplayMessage],
  buttonText: DisplayMessage,
  onSubmit: Call
)

object ContentPageViewModel {

  def apply(title: String, heading: String, buttonText: String, onSubmit: Call, paragraphs: String*): ContentPageViewModel =
    ContentPageViewModel(title, heading, paragraphs.toList, buttonText, onSubmit)

  def apply(title: String, heading: String, paragraphs: List[String], buttonText: String, onSubmit: Call): ContentPageViewModel =
    ContentPageViewModel(
      DisplayMessage(title),
      DisplayMessage(heading),
      paragraphs.map(DisplayMessage(_)),
      DisplayMessage(buttonText),
      onSubmit
    )
}