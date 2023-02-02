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

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import play.twirl.api.Html
import viewmodels.ComplexMessageElement.{LinkedMessage, Message}
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{ComplexMessage, SimpleMessage}

import scala.jdk.CollectionConverters.IteratorHasAsScala

trait HtmlHelper {

  def mainContent(html: Html): Element =
    Jsoup.parse(html.body).getElementsByAttributeValue("data-testid", "main-content").first()

  def title(html: Html): String = Jsoup.parse(html.body).title()

  def h1(html: Html): String = mainContent(html).getElementsByTag("h1").first().text()

  def p(html: Html): List[String] =
    mainContent(html).getElementsByTag("p").iterator().asScala.map(_.text()).toList

  def tr(html: Html): List[List[String]] =
    mainContent(html).getElementsByTag("tr").iterator().asScala.toList.map(
      _.getElementsByTag("td").iterator().asScala.toList.map(_.text())
    )

  def insetText(html: Html): String =
    mainContent(html).getElementsByClass("govuk-inset-text").first().text()

  def button(html: Html): Element =
    mainContent(html).getElementsByTag("button").first()

  def anchorButton(html: Html): Element =
    mainContent(html).select("a[role=button]").first()

  def form(html: Html): Element =
    mainContent(html).getElementsByTag("form").first()

  def errorSummary(html: Html): Elements =
    mainContent(html).getElementsByClass("govuk-error-summary")

  def errorMessage(html: Html): Elements =
    mainContent(html).getElementsByClass("govuk-error-message")

  def messageKey(message: DisplayMessage): String = message match {
    case SimpleMessage(key, _) => key
    case ComplexMessage(elements, _) => elements.map{
      case Message(key, _) => key
      case LinkedMessage(key, _, _) => key
    }.reduce(_ + _)
  }
}