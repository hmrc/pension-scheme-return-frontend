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

package pages.nonsipp.employercontributions

import config.Refined.{Max300, Max50}
import controllers.TestValues
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.{ConditionalYesNo, Crn}
import viewmodels.models.SectionStatus
import pages.behaviours.PageBehaviours

class EmployerCompanyCrnPageSpec extends PageBehaviours with TestValues {

  "CompanyBuyerCRNPage" - {

    val memberIndex = refineMV[Max300.Refined](1)
    val index = refineMV[Max50.Refined](1)

    beRetrievable[ConditionalYesNo[String, Crn]](
      EmployerCompanyCrnPage(srnGen.sample.value, memberIndex, index)
    )

    beSettable[ConditionalYesNo[String, Crn]](
      EmployerCompanyCrnPage(srnGen.sample.value, memberIndex, index)
    )

    beRemovable[ConditionalYesNo[String, Crn]](
      EmployerCompanyCrnPage(srnGen.sample.value, memberIndex, index)
    )

    "Dependent values: section status and were employer contributions made are" - {

      "changing when type of business added" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)

        val result = userAnswers
          .set(EmployerCompanyCrnPage(srn, memberIndex, index), ConditionalYesNo.yes[String, Crn](crn))
          .success
          .value

        result.get(EmployerContributionsPage(srn)) must be(Some(true))
        result.get(EmployerCompanyCrnPage(srn, memberIndex, index)) must be(
          Some(ConditionalYesNo.yes[String, Crn](crn))
        )
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.InProgress))
      }

      "not changing when value stays the same" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerNamePage(srn, memberIndex, index), employerName)
          .unsafeSet(EmployerCompanyCrnPage(srn, memberIndex, index), ConditionalYesNo.yes[String, Crn](crn))
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)

        val result = userAnswers
          .set(EmployerCompanyCrnPage(srn, memberIndex, index), ConditionalYesNo.yes[String, Crn](crn))
          .success
          .value

        result.get(EmployerContributionsPage(srn)) must be(Some(true))
        result.get(EmployerCompanyCrnPage(srn, memberIndex, index)) must be(
          Some(ConditionalYesNo.yes[String, Crn](crn))
        )
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.Completed))
      }

      "changing when value is different" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerCompanyCrnPage(srn, memberIndex, index), ConditionalYesNo.yes[String, Crn](crn))
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)

        val result =
          userAnswers
            .set(EmployerCompanyCrnPage(srn, memberIndex, index), ConditionalYesNo.no[String, Crn](noCrnReason))
            .success
            .value

        result.get(EmployerCompanyCrnPage(srn, memberIndex, index)) must be(
          Some(ConditionalYesNo.no[String, Crn](noCrnReason))
        )
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.InProgress))
      }
    }
  }
}
