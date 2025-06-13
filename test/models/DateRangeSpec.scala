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

package models

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.BaseSpec
import org.scalacheck.Gen

class DateRangeSpec extends BaseSpec with ScalaCheckPropertyChecks {

  "ordering" - {

    "sort latest first by `to` date" in {

      forAll(Gen.listOf(dateRangeGen)) { dates =>
        dates.sorted.map(_.to).foldLeft(latestDate) { case (prev, curr) =>
          assert(!curr.isAfter(prev), s"$curr is before $prev")
          curr
        }
      }
    }
  }
}
