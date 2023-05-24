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

  private def anchor(content: Html, url: String, attrs: Map[String, String]): Html = {
    val attributes = attrs.toList.map { case (key, value) => s"""$key="$value"""" }.mkString(" ")

    HtmlFormat.raw(s"""<a href="$url" class="govuk-link" $attributes>$content</a>""")
  }

  private def anchorDownload(content: Html, url: String): Html =
    HtmlFormat.raw(s"""<a href="$url" class="govuk-link" download>$content</a>""")

  private def paragraph(content: Html): Html =
    HtmlFormat.raw(s"""<p class="govuk-body">$content</p>""")

  private def unorderedList(elements: NonEmptyList[Html]): Html =
    HtmlFormat.raw(s"""<ul class="govuk-list govuk-list--bullet">${elements.map(listItem).toList.mkString}</ul>""")

  private def listItem(content: Html): Html =
    HtmlFormat.raw(s"<li>$content</li>")

  private def simpleList(elements: NonEmptyList[Html]): Html =
    HtmlFormat.raw(elements.toList.mkString("<br>"))

  private def tableElement(element: (Html, Html)): Html = {
    val (key, value) = element
    HtmlFormat.raw(
      s"""<tr class="govuk-table__row">
         |<td class="govuk-table__cell">$key</td>
         |<td class="govuk-table__cell">$value</td>
         |</tr>""".stripMargin
    )
  }

  private def table(elements: NonEmptyList[(Html, Html)]): Html =
    HtmlFormat.raw(
      s"""<table class="govuk-table"><tbody class="govuk-table__body">${elements
        .map(tableElement)
        .toList
        .mkString}</tbody></table>"""
    )

  private def combine(left: Html, right: Html): Html =
    HtmlFormat.raw(left.body + " " + right.body)

  private def h2(content: Html, cssClass: String): Html =
    HtmlFormat.raw(
      s"""<h2 class="$cssClass">${content.body}</h2>"""
    )

  def renderMessage(message: DisplayMessage)(implicit messages: Messages): Html =
    message match {
      case Empty => Html("")
      case m @ Message(_, _) => HtmlFormat.escape(m.toMessage)
      case LinkMessage(content, url, attrs) => anchor(renderMessage(content), url, attrs)
      case DownloadLinkMessage(content, url) => anchorDownload(renderMessage(content), url)
      case ParagraphMessage(content) => paragraph(content.map(renderMessage).reduce(combine))
      case ListMessage(content, Bullet) => unorderedList(content.map(renderMessage))
      case ListMessage(content, NewLine) => simpleList(content.map(renderMessage))
      case TableMessage(content) =>
        table(content.map { case (key, value) => renderMessage(key) -> renderMessage(value) })
      case CompoundMessage(first, second) => combine(renderMessage(first), renderMessage(second))
      case Heading2(content, labelSize) => h2(renderMessage(content), labelSize.toString)
    }
}
