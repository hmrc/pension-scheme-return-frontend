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
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.Message
import viewmodels.govuk.summarylist._

case class CheckYourAnswersViewModel(
  sections: List[CheckYourAnswersSection],
  marginBottom: Option[Int] = None
) {
  def withMarginBottom(margin: Int): CheckYourAnswersViewModel = copy(marginBottom = Some(margin))
}

case class CheckYourAnswersSection(
  heading: Option[DisplayMessage],
  rows: List[CheckYourAnswersRowViewModel]
)

object CheckYourAnswersViewModel {

  def apply(
    rows: Seq[CheckYourAnswersRowViewModel],
    onSubmit: Call
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel(
      Message("checkYourAnswers.title"),
      Message("checkYourAnswers.heading"),
      CheckYourAnswersViewModel(List(CheckYourAnswersSection(None, rows.toList))),
      onSubmit
    )

  def singleSection(rows: List[CheckYourAnswersRowViewModel]): CheckYourAnswersViewModel =
    CheckYourAnswersViewModel(List(CheckYourAnswersSection(None, rows)))
}

case class CheckYourAnswersRowViewModel(
  key: Message,
  value: Message,
  actions: Seq[SummaryAction],
  oneHalfWidth: Boolean = false
) {

  def toSummaryListRow(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = if (oneHalfWidth) KeyViewModel(key.toMessage).withOneHalfWidth() else KeyViewModel(key.toMessage),
      value = ValueViewModel(value.toMessage),
      actions = actions.map { a =>
        ActionItemViewModel(Text(a.content.toMessage), a.href)
          .withVisuallyHiddenText(a.visuallyHiddenContent.toMessage)
      }
    )

  def toSummaryListRowPartitioned(implicit messages: Messages): SummaryListRow =
    SummaryListRowViewModel(
      key = if (oneHalfWidth) KeyViewModel(key.toMessage).withOneHalfWidth() else KeyViewModel(key.toMessage),
      value = ValueViewModel(value.toMessage).withCssClass("govuk-!-width-one-quarter"),
      actions = actions.map { a =>
        ActionItemViewModel(Text(a.content.toMessage), a.href)
          .withVisuallyHiddenText(a.visuallyHiddenContent.toMessage)
      }
    )

  def withAction(action: SummaryAction): CheckYourAnswersRowViewModel =
    copy(actions = actions :+ action)

  def with2Actions(action1: SummaryAction, action2: SummaryAction): CheckYourAnswersRowViewModel =
    copy(actions = actions :+ action1 :+ action2)

  def withChangeAction(changeUrl: String): CheckYourAnswersRowViewModel = withAction(
    SummaryAction("site.change", changeUrl)
  )

  def withChangeAction(changeUrl: String, hidden: String): CheckYourAnswersRowViewModel = withAction(
    SummaryAction("site.change", changeUrl).withVisuallyHiddenContent(hidden)
  )

  def withChangeAction(changeUrl: String, hidden: Message): CheckYourAnswersRowViewModel = withAction(
    SummaryAction("site.change", changeUrl).withVisuallyHiddenContent(hidden)
  )

  def withOneHalfWidth(): CheckYourAnswersRowViewModel = copy(oneHalfWidth = true)
}

object CheckYourAnswersRowViewModel {

  def apply(
    key: String,
    value: String
  ): CheckYourAnswersRowViewModel =
    apply(Message(key), Message(value))

  def apply(
    key: Message,
    value: String
  ): CheckYourAnswersRowViewModel =
    apply(key, Message(value))

  def apply(
    key: Message,
    value: Message
  ): CheckYourAnswersRowViewModel =
    CheckYourAnswersRowViewModel(key, value, Seq())
}

case class SummaryAction(content: Message, href: String, visuallyHiddenContent: Message) {

  def withVisuallyHiddenContent(content: Message): SummaryAction = copy(visuallyHiddenContent = content)
  def withVisuallyHiddenContent(content: String): SummaryAction = withVisuallyHiddenContent(Message(content))
}

object SummaryAction {

  def apply(content: String, href: String): SummaryAction =
    SummaryAction(Message(content), href, Message(""))
}
