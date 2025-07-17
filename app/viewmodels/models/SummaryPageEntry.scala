/*
 * Copyright 2025 HM Revenue & Customs
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

import viewmodels.{DisplayMessage, Margin}

sealed trait SummaryPageEntry

object SummaryPageEntry {
  case class Section(viewModel: CheckYourAnswersSummaryViewModel) extends SummaryPageEntry
  case class Heading(value: DisplayMessage) extends SummaryPageEntry
}

case class CheckYourAnswersSummaryViewModel(
  heading: DisplayMessage,
  sections: List[CheckYourAnswersSection],
  marginBottom: Option[Margin] = Some(Margin.Fixed60Bottom),
  inset: Option[DisplayMessage] = None
) {
  def withMarginBottom(margin: Margin): CheckYourAnswersSummaryViewModel = copy(marginBottom = Some(margin))

  def withInset(inset: Option[DisplayMessage]): CheckYourAnswersSummaryViewModel = copy(inset = inset)
}
