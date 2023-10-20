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

package pages.nonsipp.employercontributions

import pages.nonsipp.employercontributions.OtherEmployeeDescriptionPage

import config.Refined._
import eu.timepit.refined.refineMV

import pages.behaviours.PageBehaviours

class OtherEmployeeDescriptionPageSpec extends PageBehaviours {

  "OtherEmployeeDescriptionPage" - {

    val index = refineMV[Max300.Refined](1)
    val secondaryIndex = refineMV[Max50.Refined](1)

    beRetrievable[String](OtherEmployeeDescriptionPage(srnGen.sample.value, index, secondaryIndex))

    beSettable[String](OtherEmployeeDescriptionPage(srnGen.sample.value, index, secondaryIndex))

    beRemovable[String](OtherEmployeeDescriptionPage(srnGen.sample.value, index, secondaryIndex))
  }
}
