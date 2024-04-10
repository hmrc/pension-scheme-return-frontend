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

import config.Refined.{OneTo300, OneTo5}
import eu.timepit.refined.refineMV
import pages.nonsipp.membertransferout.ReceivingSchemeNamePage
import pages.behaviours.PageBehaviours

class ReceivingSchemeNamePageSpec extends PageBehaviours {

  "ReceivingSchemeNamePage" - {

    val index = refineMV[OneTo300](1)
    val transferIndex = refineMV[OneTo5](1)

    beRetrievable[String](ReceivingSchemeNamePage(srnGen.sample.value, index, transferIndex))

    beSettable[String](ReceivingSchemeNamePage(srnGen.sample.value, index, transferIndex))

    beRemovable[String](ReceivingSchemeNamePage(srnGen.sample.value, index, transferIndex))
  }
}
