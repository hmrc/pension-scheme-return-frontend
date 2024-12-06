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

package viewmodels

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import viewmodels.DisplayMessage.Message
import viewmodels.models.{ListRow, ListSection}

class ListViewModelSpec extends AnyFreeSpec with Matchers {

  private def buildRows(i: Int): List[ListRow] =
    (1 to i).map(index => ListRow(Message(index.toString), None, None, None, None)).toList

  "ListSection.paginate" - {
    "single section" - {
      "don't paginate when they all fit on one page" in {
        val list = List(ListSection(buildRows(10)))

        list.paginateSections(1, 10) mustEqual list
      }
      "paginate across multiple pages" in {
        val rows = buildRows(25)
        val list = List(ListSection(rows))

        list.paginateSections(1, 10) mustEqual List(ListSection(rows.take(10)))
        list.paginateSections(2, 10) mustEqual List(ListSection(rows.slice(10, 20)))
        list.paginateSections(3, 10) mustEqual List(ListSection(rows.slice(20, 25)))
      }
    }

// TODO: PSR-1643 uncomment when fixing pagination for pre pop list pages

    "multiple sections" - {
      "don't paginate when they all fit on one page" in {
        val rows = buildRows(20)
        val list = List(
          ListSection(rows.take(10)),
          ListSection(rows.slice(10, 20))
        )

        list.paginateSections(1, 20) mustEqual list
      }

      "paginate across multiple pages" in {
        val rows = buildRows(25)
        val list = List(
          ListSection(Some(Message("section 1")), rows.take(5)),
          ListSection(Some(Message("section 2")), rows.slice(5, 25))
        )

        // Page 1: First 10 rows
        list.paginateSections(1, 10) mustEqual List(
          ListSection(Some(Message("section 1")), rows.take(5)),
          ListSection(Some(Message("section 2")), rows.slice(5, 10))
        )

        // Page 2: Next 10 rows
        list.paginateSections(2, 10) mustEqual List(
          ListSection(Some(Message("section 2")), rows.slice(10, 20))
        )

        // Page 3: Remaining rows
        list.paginateSections(3, 10) mustEqual List(
          ListSection(Some(Message("section 2")), rows.slice(20, 25))
        )
      }

    }
  }
}
