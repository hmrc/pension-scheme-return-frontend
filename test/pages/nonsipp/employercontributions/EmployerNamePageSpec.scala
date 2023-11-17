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
import pages.behaviours.PageBehaviours
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.memberpayments.EmployerContributionsPage
import utils.UserAnswersUtils.UserAnswersOps

class EmployerNamePageSpec extends PageBehaviours with TestValues {

  private val memberIndex = refineMV[Max300.Refined](1)
  private val indexOne = refineMV[Max50.Refined](1)
  private val indexTwo = refineMV[Max50.Refined](2)

  "EmployerNamePage" - {

    val index = refineMV[Max50.Refined](1)

    beRetrievable[String](EmployerNamePage(srnGen.sample.value, memberIndex, index))

    beSettable[String](EmployerNamePage(srnGen.sample.value, memberIndex, index))

    beRemovable[String](EmployerNamePage(srnGen.sample.value, memberIndex, index))
  }

  "cleanup other fields when removed with index-1" in {
    val srn = srnGen.sample.value

    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, memberIndex), memberDetails)
      .unsafeSet(EmployerNamePage(srn, memberIndex, indexOne), employerName)
      .unsafeSet(TotalEmployerContributionPage(srn, memberIndex, indexOne), money)
      .unsafeSet(EmployerCompanyCrnPage(srn, memberIndex, indexOne), ConditionalYesNo.yes[String, Crn](crn))
      .unsafeSet(ContributionsFromAnotherEmployerPage(srn, memberIndex, indexOne), true)
      .unsafeSet(EmployerContributionsPage(srn), true)

    val result = userAnswers.remove(EmployerNamePage(srn, memberIndex, indexOne)).success.value

    result.get(EmployerNamePage(srn, memberIndex, indexOne)) must be(empty)
    result.get(TotalEmployerContributionPage(srn, memberIndex, indexOne)) must be(empty)
    result.get(EmployerCompanyCrnPage(srn, memberIndex, indexOne)) must be(empty)
    result.get(ContributionsFromAnotherEmployerPage(srn, memberIndex, indexOne)) must be(empty)
    result.get(EmployerContributionsPage(srn)) must be(empty)
  }

  "cleanup other fields when removed with index bigger than 1" in {
    val srn = srnGen.sample.value

    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, memberIndex), memberDetails)
      .unsafeSet(EmployerNamePage(srn, memberIndex, indexTwo), employerName)
      .unsafeSet(TotalEmployerContributionPage(srn, memberIndex, indexTwo), money)
      .unsafeSet(EmployerCompanyCrnPage(srn, memberIndex, indexTwo), ConditionalYesNo.yes[String, Crn](crn))
      .unsafeSet(ContributionsFromAnotherEmployerPage(srn, memberIndex, indexTwo), true)
      .unsafeSet(EmployerContributionsPage(srn), true)

    val result = userAnswers.remove(EmployerNamePage(srn, memberIndex, indexTwo)).success.value

    result.get(EmployerNamePage(srn, memberIndex, indexTwo)) must be(empty)
    result.get(TotalEmployerContributionPage(srn, memberIndex, indexTwo)) must be(empty)
    result.get(EmployerCompanyCrnPage(srn, memberIndex, indexTwo)) must be(empty)
    result.get(ContributionsFromAnotherEmployerPage(srn, memberIndex, indexTwo)) must be(empty)
    result.get(EmployerContributionsPage(srn)) must be(Some(true))
  }
}
