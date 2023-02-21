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
import viewmodels.DisplayMessage.SimpleMessage

case class YesNoPageViewModel(
  title: SimpleMessage,
  heading: SimpleMessage,
  description: Option[SimpleMessage],
  legend: Option[SimpleMessage],
  onSubmit: Call
)

object YesNoPageViewModel {

  def apply(title: String, heading: String, description: String, legend: String, onSubmit: Call): YesNoPageViewModel =
    YesNoPageViewModel(
      SimpleMessage(title),
      SimpleMessage(heading),
      Some(SimpleMessage(description)),
      Some(SimpleMessage(legend)),
      onSubmit
    )

  def apply(title: String, heading: String, legend: String, onSubmit: Call): YesNoPageViewModel =
    YesNoPageViewModel(
      SimpleMessage(title),
      SimpleMessage(heading),
      None,
      Some(SimpleMessage(legend)),
      onSubmit
    )

  def apply(title: String, heading: String, description: Option[String], legend: String, onSubmit: Call): YesNoPageViewModel =
    YesNoPageViewModel(
      SimpleMessage(title),
      SimpleMessage(heading),
      description.map(d => SimpleMessage(d)),
      Some(SimpleMessage(legend)),
      onSubmit
    )

  def apply(title: SimpleMessage, heading: SimpleMessage, onSubmit: Call): YesNoPageViewModel =
    YesNoPageViewModel(
      title,
      heading,
      None,
      None,
      onSubmit
    )
}