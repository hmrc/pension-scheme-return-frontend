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

package views

import org.jsoup.nodes.Element
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.ActionItem

trait HtmlModels {

  case class AnchorTag(href: String, content: String)

  object AnchorTag {

    def apply(element: Element): AnchorTag =
      AnchorTag(element.attr("href"), element.text())

    def apply(item: ActionItem): AnchorTag =
      AnchorTag(
        item.href,
        s"${item.content.asHtml}${item.visuallyHiddenText.fold("")(s => s" $s")}"
      )
  }

  case class Form(method: String, action: String)

  object Form {

    def apply(element: Element): Form =
      Form(element.attr("method"), element.attr("action"))
  }

  case class RadioItem(id: String, value: String, label: String)

  object RadioItem {

    def apply(element: Element): RadioItem = {

      val id = element.id()
      val value = element.`val`()
      val label = element.siblingElements().select("[for=\"" + id + "\"]")

      RadioItem(id, value, label.text())
    }
  }

  case class Input(id: String, name: String, value: String, label: String)

  object Input {

    def apply(element: Element): Input = {

      val id = element.id()
      val name = element.attr("name")
      val value = element.`val`()
      val label = element.siblingElements().select("[for=\"" + id + "\"]")

      Input(id, name, value, label.text())
    }
  }
}
