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

package pages.nonsipp.landorpropertydisposal

import config.RefinedTypes.{Max50, OneTo5000}
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, Crn}
import eu.timepit.refined.api.Refined
import pages.behaviours.PageBehaviours

class CompanyBuyerCrnPageSpec extends PageBehaviours {

  "CompanyBuyerCrnPage" - {
    val srnSample = srnGen.sample.value
    val index: Refined[Int, OneTo5000] = refineMV[OneTo5000](1)
    val disposalIndex: Refined[Int, Max50.Refined] = refineMV[Max50.Refined](1)

    beRetrievable[ConditionalYesNo[String, Crn]](
      CompanyBuyerCrnPage(srnSample, index, disposalIndex)
    )

    beSettable[ConditionalYesNo[String, Crn]](
      CompanyBuyerCrnPage(srnSample, index, disposalIndex)
    )

    beRemovable[ConditionalYesNo[String, Crn]](
      CompanyBuyerCrnPage(srnSample, index, disposalIndex)
    )

    CompanyBuyerCrnPage(srnSample, index, disposalIndex).toString mustBe "idNumber"
  }
}
