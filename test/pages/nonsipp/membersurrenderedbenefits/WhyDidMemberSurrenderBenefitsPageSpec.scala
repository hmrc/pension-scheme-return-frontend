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

package pages.nonsipp.membersurrenderedbenefits

import config.RefinedTypes.OneTo300
import eu.timepit.refined.refineMV
import pages.behaviours.PageBehaviours

class WhyDidMemberSurrenderBenefitsPageSpec extends PageBehaviours {

  "WhyDidMemberSurrenderBenefitsPage" - {
    val srnSample = srnGen.sample.value
    val index = refineMV[OneTo300](1)

    beRetrievable[String](WhyDidMemberSurrenderBenefitsPage(srnSample, index))

    beSettable[String](WhyDidMemberSurrenderBenefitsPage(srnSample, index))

    beRemovable[String](WhyDidMemberSurrenderBenefitsPage(srnSample, index))

    WhyDidMemberSurrenderBenefitsPage(srnSample, index).toString mustBe "surrenderReason"
  }

}
