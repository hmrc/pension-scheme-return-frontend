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

package pages

import pages.nonsipp.memberdetails._
import controllers.TestValues
import utils.IntUtils.given
import utils.UserAnswersUtils.UserAnswersOps
import models.NameDOB
import pages.behaviours.PageBehaviours

class MemberDetailsPageSpec extends PageBehaviours with TestValues {

  private val memberIndex = 1

  beRetrievable[NameDOB](MemberDetailsPage(srn, memberIndex))
  beSettable[NameDOB](MemberDetailsPage(srn, memberIndex))
  beRemovable[NameDOB](MemberDetailsPage(srn, memberIndex))

  "Retain data when member details page is modified" in {
    val srn = srnGen.sample.value
    val nino = ninoGen.sample.value

    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, memberIndex), memberDetails)
      .unsafeSet(DoesMemberHaveNinoPage(srn, memberIndex), true)
      .unsafeSet(MemberDetailsNinoPage(srn, memberIndex), nino)
      .unsafeSet(NoNINOPage(srn, memberIndex), "test reason")

    val result = userAnswers.set(MemberDetailsPage(srn, memberIndex), memberDetails).success.value
    result.get(MemberDetailsPage(srn, memberIndex)) mustBe Some(memberDetails)
    result.get(DoesMemberHaveNinoPage(srn, memberIndex)) mustBe Some(true)
    result.get(MemberDetailsNinoPage(srn, memberIndex)) mustBe Some(nino)
    result.get(NoNINOPage(srn, memberIndex)) mustBe Some("test reason")
  }
}
