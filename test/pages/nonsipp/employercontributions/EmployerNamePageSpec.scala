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

import config.Refined.{Max300, Max50}
import controllers.TestValues
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, Crn}
import utils.UserAnswersUtils.UserAnswersOps
import pages.behaviours.PageBehaviours
import pages.nonsipp.memberdetails.MemberDetailsPage

class EmployerNamePageSpec extends PageBehaviours with TestValues {
  private val memberIndex = refineMV[Max300.Refined](1)
  private val index = refineMV[Max50.Refined](1)

  "EmployerNamePage" - {

    beRetrievable[String](EmployerNamePage(srnGen.sample.value, memberIndex, index))

    beSettable[String](EmployerNamePage(srnGen.sample.value, memberIndex, index))

    beRemovable[String](EmployerNamePage(srnGen.sample.value, memberIndex, index))
  }

  "cleanup other fields when removed" in {
    val srn = srnGen.sample.value

    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, memberIndex), memberDetails)
      .unsafeSet(EmployerNamePage(srn, memberIndex, index), employerName)
      .unsafeSet(TotalEmployerContributionPage(srn, memberIndex, index), money)
      .unsafeSet(EmployerCompanyCrnPage(srn, memberIndex, index), ConditionalYesNo.yes[String, Crn](crn))

    val result = userAnswers.remove(EmployerNamePage(srn, memberIndex, index)).success.value

    result.get(EmployerNamePage(srn, memberIndex, index)) must be(empty)
    result.get(TotalEmployerContributionPage(srn, memberIndex, index)) must be(empty)
    result.get(EmployerCompanyCrnPage(srn, memberIndex, index)) must be(empty)
  }
}
