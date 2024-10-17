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

package viewmodels.models

import play.api.mvc.Call
import viewmodels.DisplayMessage.{BlockMessage, InlineMessage, Message}

case class DateRangeViewModel(
  startDateLabel: Message,
  startDateHint: Message,
  endDateLabel: Message,
  endDateHint: Message
)

object DateRangeViewModel {

  def apply(
    title: Message,
    heading: InlineMessage,
    startDateHint: Message,
    endDateHint: Message,
    description: Option[BlockMessage],
    onSubmit: Call
  ): FormPageViewModel[DateRangeViewModel] =
    FormPageViewModel(
      title,
      heading,
      DateRangeViewModel(
        Message("site.startDate"),
        startDateHint,
        Message("site.endDate"),
        endDateHint
      ),
      onSubmit
    ).withDescription(description)
}
