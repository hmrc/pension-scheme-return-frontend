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

package viewmodels.govuk

import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, HtmlContent}
import viewmodels.Margin
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.table._

object table extends TableFluency

trait TableFluency {

  object TableViewModel {

    def apply(rows: Seq[Seq[TableRow]]): Table =
      Table(rows = rows)

    def apply(head: Seq[HeadCell], rows: Seq[Seq[TableRow]]): Table =
      Table(head = Some(head), rows = rows)
  }

  implicit class FluentTable(list: Table) {

    def withCssClass(className: String): Table =
      list.copy(classes = s"${list.classes} $className")

    def withAttribute(attribute: (String, String)): Table =
      list.copy(attributes = list.attributes + attribute)

    def withMarginBottom(maybeMargin: Option[Margin]): Table =
      maybeMargin.fold(list)(margin => list.withCssClass(margin.className))

    def withCaption(caption: String): Table =
      list.copy(caption = Some(caption))

    def withCaptionClasses(captionClasses: String): Table =
      list.copy(captionClasses = captionClasses)
  }

  object TableRowViewModel {

    def apply(content: Content): TableRow =
      TableRow(content = content)

    def apply(content: Html): TableRow =
      apply(HtmlContent(content))
  }

  implicit class FluentTableRow(row: TableRow) {

    def withCssClass(className: String): TableRow =
      row.copy(classes = s"${row.classes} $className")

    def bold: TableRow =
      withCssClass("govuk-!-font-weight-bold")

    def widthOneThird: TableRow =
      withCssClass("govuk-!-width-one-third")
  }

}
