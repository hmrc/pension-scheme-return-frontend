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
import models.IdentityType
import viewmodels.models.SectionStatus
import pages.behaviours.PageBehaviours

class EmployerTypeOfBusinessPageSpec extends PageBehaviours with TestValues {

  private val memberIndex = refineMV[Max300.Refined](1)
  private val indexOne = refineMV[Max50.Refined](1)
  private val typeOfBusiness = IdentityType.UKCompany
  private val otherTypeOfBusiness = IdentityType.UKPartnership

  "EmployerTypeOfBusinessPage" - {

    val index = refineMV[Max50.Refined](1)

    beRetrievable[IdentityType](EmployerTypeOfBusinessPage(srnGen.sample.value, memberIndex, index))

    beSettable[IdentityType](EmployerTypeOfBusinessPage(srnGen.sample.value, memberIndex, index))

    beRemovable[IdentityType](EmployerTypeOfBusinessPage(srnGen.sample.value, memberIndex, index))

    "Dependent values: section status and were employer contributions made are" - {

      "changing when type of business added" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)

        val result =
          userAnswers.set(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne), typeOfBusiness).success.value

        result.get(EmployerContributionsPage(srn)) must be(Some(true))
        result.get(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne)) must be(Some(typeOfBusiness))
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.InProgress))
      }

      "not changing when value stays the same" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerNamePage(srn, memberIndex, indexOne), employerName)
          .unsafeSet(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne), typeOfBusiness)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)

        val result =
          userAnswers.set(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne), typeOfBusiness).success.value

        result.get(EmployerContributionsPage(srn)) must be(Some(true))
        result.get(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne)) must be(Some(typeOfBusiness))
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.Completed))
      }

      "changing when value is different" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerNamePage(srn, memberIndex, indexOne), employerName)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)

        val result =
          userAnswers.set(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne), otherTypeOfBusiness).success.value

        result.get(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne)) must be(Some(otherTypeOfBusiness))
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.InProgress))
      }
    }
  }

}
