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

package views.components

import play.twirl.api.{Html, HtmlFormat}
import cats.data.NonEmptyList
import viewmodels.DisplayMessage.ListType._
import viewmodels.DisplayMessage._
import play.api.i18n.Messages
import viewmodels.DisplayMessage

object Components {

  private def anchor(content: Html, url: String, attrs: Map[String, String]): Html = {
    val attributes = attrs.toList.map { case (key, value) => s"""$key="$value"""" }.mkString(" ")

    if (attributes.endsWith("class=\"govuk-task-list__link\"")) {
      HtmlFormat.raw(s"""<a href="$url" class="govuk-link govuk-task-list__link" $attributes>$content</a>""")
    } else {
      HtmlFormat.raw(s"""<a href="$url" class="govuk-link" $attributes>$content</a>""")
    }
  }

  private def anchorWithHiddenText(content: Html, url: String, attrs: Map[String, String], hiddenText: Html): Html = {
    val attributes = attrs.toList.map { case (key, value) => s"""$key="$value"""" }.mkString(" ")

    HtmlFormat.raw(
      s"""<a href="$url" class="govuk-link" $attributes >$content<span class="govuk-visually-hidden">$hiddenText</span></a>"""
    )
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

  private def tableHeading(element: (Html, Html)): Html = {
    val (key, value) = element
    HtmlFormat.raw(
      s"""<thead class="govuk-table__head">
         |  <tr class="govuk-table__row">
         |    <th scope="col" class="govuk-table__header">$key</th>
         |    <th scope="col" class="govuk-table__header">$value</th>
         |  </tr>
         |</thead>""".stripMargin
    )
  }

  private def table(elements: NonEmptyList[(Html, Html)], heading: Option[(Html, Html)]): Html =
    HtmlFormat.raw(
      s"""
        <table class="govuk-table">
          ${heading.map(tableHeading).getOrElse(HtmlFormat.empty)}
          <tbody class="govuk-table__body">
            ${elements.map(tableElement).toList.mkString}
          </tbody>
        </table>"""
    )

  private def combine(left: Html, right: Html): Html =
    HtmlFormat.raw(left.body + " " + right.body)

  private def h2(content: Html, cssClass: String): Html =
    HtmlFormat.raw(
      s"""<h2 class="$cssClass">${content.body}</h2>"""
    )

  private def hint(content: Html): Html =
    HtmlFormat.raw(
      s"""<p class="govuk-hint">${content.body}</p>"""
    )

  def renderMessage(message: DisplayMessage)(implicit messages: Messages): HtmlFormat.Appendable =
    message match {
      case Empty => Html("")
      case m @ Message(_, _) => HtmlFormat.escape(m.toMessage)
      case LinkMessage(content, url, attrs, None) => anchor(renderMessage(content), url, attrs)
      case LinkMessage(content, url, attrs, Some(hiddenText)) =>
        anchorWithHiddenText(renderMessage(content), url, attrs, renderMessage(hiddenText))
      case DownloadLinkMessage(content, url) => anchorDownload(renderMessage(content), url)
      case ParagraphMessage(content) => paragraph(content.map(renderMessage).reduce(using combine(_, _)))
      case ListMessage(content, Bullet) => unorderedList(content.map(renderMessage))
      case ListMessage(content, NewLine) => simpleList(content.map(renderMessage))
      case TableMessage(content, heading) =>
        table(
          content.map { case (key, value) => renderMessage(key) -> renderMessage(value) },
          heading.map { case (key, value) => renderMessage(key) -> renderMessage(value) }
        )
      case CompoundMessage(first, second) => combine(renderMessage(first), renderMessage(second))
      case Heading2(content, headingSize) => h2(renderMessage(content), headingSize.toString)
      case HintMessage(content) => hint(renderMessage(content))
    }
}
