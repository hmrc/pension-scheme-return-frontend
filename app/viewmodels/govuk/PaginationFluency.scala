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

import uk.gov.hmrc.govukfrontend.views.viewmodels.pagination._
import play.api.i18n.Messages

object pagination extends PaginationFluency

trait PaginationFluency {

  object PaginationViewModel {
    def apply(pagination: models.Pagination)(
      implicit messages: Messages
    ): Pagination = {
      import pagination._

      val showPreviousPageLink: Boolean = currentPage > 1
      val showNextPageLink: Boolean = (currentPage * pageSize) < totalSize

      val firstItem: Option[PaginationItem] = Option.when(currentPage > 1)(
        PaginationItem(
          href = call(1).url,
          number = Some("1")
        )
      )

      val previousEllipses: Option[PaginationItem] = Option.when(currentPage > 3)(ellipsePaginationItem)

      val previousItem: Option[PaginationItem] = Option.when(currentPage > 2)(
        PaginationItem(
          href = call(currentPage - 1).url,
          number = Some((currentPage - 1).toString)
        )
      )

      val currentItem: Option[PaginationItem] = Some(
        PaginationItem(
          href = call(currentPage).url,
          number = Some(currentPage.toString),
          current = Some(true)
        )
      )

      val nextItem: Option[PaginationItem] = Option.when((totalPages - currentPage) > 1)(
        PaginationItem(
          href = call(currentPage + 1).url,
          number = Some((currentPage + 1).toString)
        )
      )

      val nextEllipses: Option[PaginationItem] = Option.when((totalPages - currentPage) > 2)(ellipsePaginationItem)

      val lastItem: Option[PaginationItem] = Option.when(currentPage < totalPages)(
        PaginationItem(
          href = call(totalPages).url,
          number = Some(totalPages.toString)
        )
      )

      val pageItems = Seq(
        firstItem,
        previousEllipses,
        previousItem,
        currentItem,
        nextItem,
        nextEllipses,
        lastItem
      ).flatten

      Pagination(
        items = Option.when(totalSize > pageSize)(pageItems),
        next = Option.when(showNextPageLink)(PaginationLink(call(currentPage + 1).url, Some(messages("site.next")))),
        previous =
          Option.when(showPreviousPageLink)(PaginationLink(call(currentPage - 1).url, Some(messages("site.previous"))))
      )
    }
  }

  private val ellipsePaginationItem = PaginationItem(
    href = "",
    ellipsis = Some(true)
  )
}
