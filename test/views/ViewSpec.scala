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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.i18n.Messages
import play.twirl.api.Html
import utils.{BaseSpec, DisplayMessageUtils}
import viewmodels.DisplayMessage
import views.components.Components

trait ViewSpec
    extends BaseSpec
    with ScalaCheckPropertyChecks
    with HtmlHelper
    with ViewBehaviours
    with DisplayMessageUtils {

  def renderedErrorMessage(key: String) = s"Error: $key"

  def renderMessage(message: DisplayMessage)(implicit m: Messages): Html =
    Components.renderMessage(message)

  def renderText(message: DisplayMessage)(implicit m: Messages): String =
    Jsoup.parse(renderMessage(message).body).text()

}
