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

import config.Refined.OneTo300
import eu.timepit.refined.refineMV
import eu.timepit.refined.api.Refined
import pages.behaviours.PageBehaviours

class WhyDidMemberSurrenderBenefitsPageSpec extends PageBehaviours {
  val srn = srnGen.sample.value
  val index: Refined[Int, OneTo300] = refineMV[OneTo300](1)
  "WhyDidMemberSurrenderBenefitsPage" - {

    beRetrievable[String](WhyDidMemberSurrenderBenefitsPage(srnGen.sample.value, index))

    beSettable[String](WhyDidMemberSurrenderBenefitsPage(srnGen.sample.value, index))

    beRemovable[String](WhyDidMemberSurrenderBenefitsPage(srnGen.sample.value, index))

    WhyDidMemberSurrenderBenefitsPage(srn, index).toString mustBe "surrenderReason"
  }

}
