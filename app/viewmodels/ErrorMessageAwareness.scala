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

package viewmodels

import play.api.data.{Field, FormError}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage

trait ErrorMessageAwareness {

  def errorMessage(field: Field)(implicit messages: Messages): Option[ErrorMessage] =
    field.error
      .map {
        _ =>
          val errorMessages = field.errors.map(err => messages(err.message, err.args))
          ErrorMessage(content = HtmlContent(errorMessages.mkString("<br>")))
      }

  def errorMessage(errors: Seq[FormError])(implicit messages: Messages): Option[ErrorMessage] =
    errors.headOption
      .map {
        _ =>
          val errorMessages = errors.map(err => messages(err.message, err.args))
          ErrorMessage(content = HtmlContent(errorMessages.mkString("<br>")))
      }
}
