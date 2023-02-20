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
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.govuk.summarylist._

case class CheckYourAnswersViewModel(
                                      title: SimpleMessage,
                                      heading: SimpleMessage,
                                      rows: List[CheckYourAnswersRowViewModel],
                                      onSubmit: Call
                                    )

object CheckYourAnswersViewModel {

  def apply(
             title: String,
             heading: String,
             rows: Seq[CheckYourAnswersRowViewModel],
             onSubmit: Call
           ): CheckYourAnswersViewModel =
              CheckYourAnswersViewModel(
                SimpleMessage(title),
                SimpleMessage(heading),
                rows.toList,
                onSubmit
              )
}

case class CheckYourAnswersRowViewModel(
  key: SimpleMessage,
  value: SimpleMessage,
  actions: Seq[(SimpleMessage, String)]
) {

  def toSummaryListRow(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key = key.toMessage,
      value = value.toMessage,
      actions = actions.map { case (content, href) => ActionItemViewModel(Text(content.toMessage), href) }
    )
  }

  def withAction(action: (SimpleMessage, String)): CheckYourAnswersRowViewModel =
    copy(actions = actions :+ action)
}

object CheckYourAnswersRowViewModel {

  def apply(
    key: String,
    value: String
  ): CheckYourAnswersRowViewModel =
    CheckYourAnswersRowViewModel(SimpleMessage(key), SimpleMessage(value), Seq())
}
