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

import uk.gov.hmrc.govukfrontend.views.viewmodels.errorsummary.{ErrorLink, ErrorSummary}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{Content, Text}
import play.api.i18n.Messages
import play.api.data.{Form, FormError}

object errorsummary extends ErrorSummaryFluency

trait ErrorSummaryFluency {

  object ErrorSummaryViewModel {

    def apply(
      form: Form[?],
      errorLinkOverrides: Map[String, String] = Map.empty
    )(implicit messages: Messages): ErrorSummary =
      apply(form.errors, errorLinkOverrides)

    def apply(errors: Seq[FormError])(implicit messages: Messages): ErrorSummary =
      apply(errors, Map())

    def apply(
      errors: Seq[FormError],
      errorLinkOverrides: Map[String, String]
    )(implicit messages: Messages): ErrorSummary = {

      val allErrors = errors.distinctBy(_.message).map { error =>
        ErrorLink(
          href = Some(s"#${errorLinkOverrides.getOrElse(error.key, error.key)}"),
          content = Text(
            messages(
              error.message,
              error.args.map {
                case s: String => messages(s)
                case any => any
              }*
            )
          )
        )
      }

      ErrorSummary(
        errorList = allErrors,
        title = Text(messages("error.summary.title"))
      )
    }
  }

  implicit class FluentErrorSummary(errorSummary: ErrorSummary) {

    def withDescription(description: Content): ErrorSummary =
      errorSummary.copy(description = description)

    def withCssClass(newClass: String): ErrorSummary =
      errorSummary.copy(classes = s"${errorSummary.classes} $newClass")

    def withAttribute(attribute: (String, String)): ErrorSummary =
      errorSummary.copy(attributes = errorSummary.attributes + attribute)
  }
}
