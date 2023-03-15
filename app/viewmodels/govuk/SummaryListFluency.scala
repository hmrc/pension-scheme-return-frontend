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

package viewmodels.govuk

import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist._

object summarylist extends SummaryListFluency

trait SummaryListFluency {

  object SummaryListViewModel {

    def apply(rows: Seq[SummaryListRow]): SummaryList =
      SummaryList(rows = rows)
  }

  implicit class FluentSummaryList(list: SummaryList) {

    def withoutBorders(): SummaryList =
      list.copy(classes = s"${list.classes} govuk-summary-list--no-border")

    def withCssClass(className: String): SummaryList =
      list.copy(classes = s"${list.classes} $className")

    def withAttribute(attribute: (String, String)): SummaryList =
      list.copy(attributes = list.attributes + attribute)
  }

  object SummaryListRowViewModel {

    def apply(
      key: Key,
      value: Value
    ): SummaryListRow =
      SummaryListRow(
        key = key,
        value = value
      )

    def apply(
      key: Html,
      actions: Seq[ActionItem]
    ): SummaryListRow =
      apply(Key(HtmlContent(key)), Value(), actions)

    def apply(
      key: Key,
      value: Value,
      actions: Seq[ActionItem]
    ): SummaryListRow =
      SummaryListRow(
        key = key,
        value = value,
        actions = Some(Actions(items = actions))
      )

    def apply(
      key: String,
      value: String,
      actions: Seq[ActionItem]
    ): SummaryListRow =
      SummaryListRow(
        key = Key(Text(key)),
        value = Value(Text(value)),
        actions = Some(Actions(items = actions))
      )
  }

  implicit class FluentSummaryListRow(row: SummaryListRow) {

    def withCssClass(className: String): SummaryListRow =
      row.copy(classes = s"${row.classes} $className")

    def withAction(item: ActionItem): SummaryListRow = {
      val actions = row.actions.map(a => a.copy(items = a.items :+ item))

      row.copy(actions = actions.orElse(Some(Actions(items = Seq(item)))))
    }
  }

  object ActionItemViewModel {

    def apply(
      content: Content,
      href: String
    ): ActionItem =
      ActionItem(
        content = content,
        href = href
      )

    def apply(html: Html, href: String): ActionItem = apply(HtmlContent(html), href)
  }

  implicit class FluentActionItem(actionItem: ActionItem) {

    def withVisuallyHiddenText(text: String): ActionItem =
      actionItem.copy(visuallyHiddenText = Some(text))

    def withCssClass(className: String): ActionItem =
      actionItem.copy(classes = s"${actionItem.classes} $className")

    def withAttribute(attribute: (String, String)): ActionItem =
      actionItem.copy(attributes = actionItem.attributes + attribute)
  }

  object KeyViewModel {

    def apply(content: Content): Key =
      Key(content = content)

    def apply(html: Html): Key =
      apply(HtmlContent(html))
  }

  implicit class FluentKey(key: Key) {

    def withCssClass(className: String): Key =
      key.copy(classes = s"${key.classes} $className")

    def withRegularFont: Key =
      withCssClass("govuk-!-font-weight-regular")
  }

  object ValueViewModel {

    def apply(content: Content): Value =
      Value(content = content)
  }

  implicit class FluentValue(value: Value) {

    def withCssClass(className: String): Value =
      value.copy(classes = s"${value.classes} $className")
  }
}
