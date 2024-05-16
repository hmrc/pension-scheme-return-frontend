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

import pages.nonsipp.memberdetails.MemberDetailsPage
import config.Refined.Max300
import controllers.TestValues
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.Money
import pages.behaviours.PageBehaviours

class SurrenderedBenefitsAmountPageSpec extends PageBehaviours with TestValues {

  private val memberIndex = refineMV[Max300.Refined](1)

  "SurrenderedBenefitsAmountPage" - {

    val index = refineMV[Max300.Refined](1)

    beRetrievable[Money](SurrenderedBenefitsAmountPage(srnGen.sample.value, index))

    beSettable[Money](SurrenderedBenefitsAmountPage(srnGen.sample.value, index))

    beRemovable[Money](SurrenderedBenefitsAmountPage(srnGen.sample.value, index))

  }

  "cleanup other fields when removed with index-1" in {
    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, memberIndex), memberDetails)
      .unsafeSet(SurrenderedBenefitsAmountPage(srn, memberIndex), money)
      .unsafeSet(WhenDidMemberSurrenderBenefitsPage(srn, memberIndex), localDate)
      .unsafeSet(WhyDidMemberSurrenderBenefitsPage(srn, memberIndex), otherDetails)

    val result = userAnswers.remove(SurrenderedBenefitsAmountPage(srn, memberIndex)).success.value

    result.get(SurrenderedBenefitsAmountPage(srn, memberIndex)) must be(empty)
    result.get(WhenDidMemberSurrenderBenefitsPage(srn, memberIndex)) must be(empty)
    result.get(WhyDidMemberSurrenderBenefitsPage(srn, memberIndex)) must be(empty)
  }
}
