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

package views

import play.api.i18n.Messages
import viewmodels.models.PaginatedViewModel
import play.api.data.Form

object ViewUtils {
  def paginatedTitle(
    paginationViewModel: Option[PaginatedViewModel],
    form: Form[_],
    title: String,
    section: Option[String] = None
  )(
    implicit messages: Messages
  ): String =
    titleNoForm(
      title = s"${errorPrefix(form)} ${messages(title)} ${paginationPostfix(paginationViewModel)}",
      section = section
    )

  def paginatedTitleNoForm(
    paginationViewModel: Option[PaginatedViewModel],
    title: String,
    section: Option[String] = None
  )(
    implicit messages: Messages
  ): String =
    titleNoForm(
      title = s"${messages(title)} ${paginationPostfix(paginationViewModel)}",
      section = section
    )

  def title(form: Form[_], title: String, section: Option[String] = None)(implicit messages: Messages): String =
    titleNoForm(
      title = s"${errorPrefix(form)} ${messages(title)}",
      section = section
    )

  def titleNoForm(title: String, section: Option[String] = None)(implicit messages: Messages): String =
    s"${messages(title)} - ${section.fold("")(messages(_) + " - ")}${messages("service.title")} - ${messages("site.govuk")}"

  private def errorPrefix(form: Form[_])(implicit messages: Messages): String =
    if (form.hasErrors || form.hasGlobalErrors) messages("error.browser.title.prefix") else ""

  private def paginationPostfix(paginationViewModel: Option[PaginatedViewModel])(implicit messages: Messages): String =
    paginationViewModel.fold("")(
      paginatedViewModel =>
        if (paginatedViewModel.pagination.totalPages > 1) {
          messages(
            "site.title.postfix",
            paginatedViewModel.pagination.currentPage,
            paginatedViewModel.pagination.totalPages
          )
        } else {
          ""
        }
    )
}
