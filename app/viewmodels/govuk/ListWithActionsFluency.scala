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

import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, HtmlContent}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.listwithactions._

object listwithactions extends ListWithActionsFluency

trait ListWithActionsFluency {

  object ListWithActionsViewModel {
    def apply(items: Seq[ListWithActionsItem]): ListWithActions =
      ListWithActions(items)
  }

  object ListWithActionsItemViewModel {
    def apply(content: Content, actions: Seq[ListWithActionsAction] = Nil): ListWithActionsItem =
      ListWithActionsItem(name = content, actions)

    def apply(html: Html, actions: Seq[ListWithActionsAction]): ListWithActionsItem =
      apply(HtmlContent(html), actions)
  }

  object ListWithActionsActionViewModel {
    def apply(content: Content, href: String): ListWithActionsAction =
      ListWithActionsAction(content = content, href = href)

    def apply(content: Content): ListWithActionsAction =
      ListWithActionsAction(content = content)

    def apply(html: Html, href: String): ListWithActionsAction =
      apply(HtmlContent(html), href)

    def noLink(html: Html): ListWithActionsAction =
      ListWithActionsAction(
        content = HtmlContent(html),
        classes = "govuk-summary-list__key govuk-!-font-weight-regular",
        href = "none"
      )
  }

  implicit class FluentListWithActionsAction(actionList: ListWithActionsAction) {
    def withVisuallyHiddenText(text: String): ListWithActionsAction =
      actionList.copy(visuallyHiddenText = Some(text))
  }
}
