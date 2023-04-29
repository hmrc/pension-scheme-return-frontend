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
import uk.gov.hmrc.govukfrontend.views.Aliases.Details
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent

object details extends DetailsFluency

trait DetailsFluency {

  object DetailsViewModel {
    def apply(summary: Html, content: Html): Details = Details(
      summary = HtmlContent(summary),
      content = HtmlContent(content)
    )
  }
}
