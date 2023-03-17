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

package views.components

import cats.data.NonEmptyList
import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.ListType._
import viewmodels.DisplayMessage._

object Components {

  private def anchor(content: Html, url: String): Html =
    HtmlFormat.raw(s"""<a href="$url" class="govuk-link">$content</a>""")

  private def paragraph(content: Html): Html =
    HtmlFormat.raw(s"""<p class="govuk-body">$content</p>""")

  private def unorderedList(elements: NonEmptyList[Html]): Html =
    HtmlFormat.raw(s"""<ul class="govuk-list govuk-list--bullet">${elements.map(listItem).toList.mkString}</ul>""")

  private def listItem(content: Html): Html =
    HtmlFormat.raw(s"<li>$content</li>")

  private def simpleList(elements: NonEmptyList[Html]): Html =
    HtmlFormat.raw(elements.toList.mkString("<br>"))

  private def combine(left: Html, right: Html): Html =
    HtmlFormat.raw(left.body + " " + right.body)

  def renderMessage(message: DisplayMessage)(implicit messages: Messages): Html =
    message match {
      case m @ Message(_, _) => HtmlFormat.escape(m.toMessage)
      case LinkMessage(content, url) => anchor(renderMessage(content), url)
      case ParagraphMessage(content) => paragraph(content.map(renderMessage).reduce(combine))
      case ListMessage(content, Bullet) => unorderedList(content.map(renderMessage))
      case ListMessage(content, NewLine) => simpleList(content.map(renderMessage))
    }
}
