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

package pages.nonsipp.membertransferout

import utils.IntUtils.given
import pages.behaviours.PageBehaviours

import java.time.LocalDate

class WhenWasTransferMadePageSpec extends PageBehaviours {

  "WhenWasTransferMadePage" - {

    val index = 1
    val secondaryIndex = 1
    val srnSample = srnGen.sample.value

    beRetrievable[LocalDate](WhenWasTransferMadePage(srnSample, index, secondaryIndex))

    beSettable[LocalDate](WhenWasTransferMadePage(srnSample, index, secondaryIndex))

    beRemovable[LocalDate](WhenWasTransferMadePage(srnSample, index, secondaryIndex))
  }
}
