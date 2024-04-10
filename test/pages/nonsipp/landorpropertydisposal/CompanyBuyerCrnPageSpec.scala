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

import config.Refined.{Max50, OneTo5000}
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, Crn}
import pages.behaviours.PageBehaviours

class CompanyBuyerCrnPageSpec extends PageBehaviours {

  "CompanyBuyerCRNPage" - {

    val index = refineMV[OneTo5000](1)
    val disposalIndex = refineMV[Max50.Refined](1)

    beRetrievable[ConditionalYesNo[String, Crn]](
      CompanyBuyerCrnPage(srnGen.sample.value, index, disposalIndex)
    )

    beSettable[ConditionalYesNo[String, Crn]](
      CompanyBuyerCrnPage(srnGen.sample.value, index, disposalIndex)
    )

    beRemovable[ConditionalYesNo[String, Crn]](
      CompanyBuyerCrnPage(srnGen.sample.value, index, disposalIndex)
    )
  }
}
