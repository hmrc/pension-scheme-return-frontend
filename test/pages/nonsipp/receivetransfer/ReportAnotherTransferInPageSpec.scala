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

package pages.nonsipp.receivetransfer

import config.RefinedTypes.{Max300, Max5}
import eu.timepit.refined.refineMV
import pages.behaviours.PageBehaviours

class ReportAnotherTransferInPageSpec extends PageBehaviours {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)

  "ReportAnotherTransferInPage" - {

    val srn = srnGen.sample.value

    beRetrievable[Boolean](ReportAnotherTransferInPage(srn, index, secondaryIndex))

    beSettable[Boolean](ReportAnotherTransferInPage(srn, index, secondaryIndex))

    beRemovable[Boolean](ReportAnotherTransferInPage(srn, index, secondaryIndex))
  }

}
