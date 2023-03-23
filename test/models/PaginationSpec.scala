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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class PaginationSpec extends AnyFreeSpec with Matchers {

  private val call = controllers.routes.UnauthorisedController.onPageLoad

  "Pagination" - {
    "current page is 1, there is only 1 element and page size is 3" in {
      val result = Pagination(1, 3, 1, _ => call)

      result.pageStart mustBe 1
      result.pageEnd mustBe 1
      result.totalPages mustBe 1
    }

    "current page is 1, there are 3 elements and page size is 3" in {
      val result = Pagination(1, 3, 3, _ => call)

      result.pageStart mustBe 1
      result.pageEnd mustBe 3
      result.totalPages mustBe 1
    }

    "current page is 1, there are 4 elements and page size is 3" in {
      val result = Pagination(1, 3, 4, _ => call)

      result.pageStart mustBe 1
      result.pageEnd mustBe 3
      result.totalPages mustBe 2
    }

    "current page is 1, there are 6 elements and page size is 3" in {
      val result = Pagination(1, 3, 6, _ => call)

      result.pageStart mustBe 1
      result.pageEnd mustBe 3
      result.totalPages mustBe 2
    }

    "current page is 1, there are 7 elements and page size is 3" in {
      val result = Pagination(1, 3, 7, _ => call)

      result.pageStart mustBe 1
      result.pageEnd mustBe 3
      result.totalPages mustBe 3
    }

    "current page is 2, there are 7 elements and page size is 3" in {
      val result = Pagination(2, 3, 7, _ => call)

      result.pageStart mustBe 3
      result.pageEnd mustBe 6
      result.totalPages mustBe 3
    }

    "current page is 2 (final page), there are 4 elements and page size is 3" in {
      val result = Pagination(2, 3, 4, _ => call)

      result.pageStart mustBe 3
      result.pageEnd mustBe 4
      result.totalPages mustBe 2
    }
  }
}
