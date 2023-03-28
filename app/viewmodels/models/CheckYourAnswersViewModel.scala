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

import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import viewmodels.DisplayMessage.Message
import viewmodels.govuk.summarylist._

case class CheckYourAnswersViewModel(
  title: Message,
  heading: Message,
  rows: List[CheckYourAnswersRowViewModel],
  onSubmit: Call
)

object CheckYourAnswersViewModel {

  def apply(
    rows: Seq[CheckYourAnswersRowViewModel],
    onSubmit: Call
  ): CheckYourAnswersViewModel =
    CheckYourAnswersViewModel(
      Message("checkYourAnswers.title"),
      Message("checkYourAnswers.heading"),
      rows.toList,
      onSubmit
    )

  def apply(
    title: String,
    heading: Message,
    rows: Seq[CheckYourAnswersRowViewModel],
    onSubmit: Call
  ): CheckYourAnswersViewModel =
    CheckYourAnswersViewModel(
      Message(title),
      heading,
      rows.toList,
      onSubmit
    )
}

case class CheckYourAnswersRowViewModel(
  key: Message,
  value: Message,
  actions: Seq[SummaryAction]
) {

  def toSummaryListRow(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = key.toMessage,
      value = value.toMessage,
      actions = actions.map { a =>
        ActionItemViewModel(Text(a.content.toMessage), a.href)
          .withVisuallyHiddenText(a.visuallyHiddenContent.toMessage)
      }
    )

  def withAction(action: SummaryAction): CheckYourAnswersRowViewModel =
    copy(actions = actions :+ action)
}

object CheckYourAnswersRowViewModel {

  def apply(
    key: String,
    value: String
  ): CheckYourAnswersRowViewModel =
    CheckYourAnswersRowViewModel(Message(key), Message(value), Seq())

  def apply(
    key: Message,
    value: String
  ): CheckYourAnswersRowViewModel =
    CheckYourAnswersRowViewModel(key, Message(value), Seq())
}

case class SummaryAction(content: Message, href: String, visuallyHiddenContent: Message) {

  def withVisuallyHiddenContent(content: Message): SummaryAction = copy(visuallyHiddenContent = content)
  def withVisuallyHiddenContent(content: String): SummaryAction = withVisuallyHiddenContent(Message(content))
}

object SummaryAction {

  def apply(content: String, href: String): SummaryAction =
    SummaryAction(Message(content), href, Message(""))
}
