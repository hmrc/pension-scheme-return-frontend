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

package pages.nonsipp.moneyborrowed

import utils.IntUtils.given
import pages.behaviours.PageBehaviours

import java.time.LocalDate

class WhenBorrowedPageSpec extends PageBehaviours {

  private val srn = srnGen.sample.value

  "WhenBorrowedPage" - {

    val index = 1

    beRetrievable[LocalDate](WhenBorrowedPage(srn, index))

    beSettable[LocalDate](WhenBorrowedPage(srn, index))

    beRemovable[LocalDate](WhenBorrowedPage(srn, index))
  }
}
