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

import controllers.TestValues
import utils.IntUtils.given
import utils.UserAnswersUtils.UserAnswersOps
import models.IdentityType
import pages.behaviours.PageBehaviours

class EmployerTypeOfBusinessPageSpec extends PageBehaviours with TestValues {

  private val memberIndex = 1
  private val indexOne = 1
  private val typeOfBusiness = IdentityType.UKCompany
  private val otherTypeOfBusiness = IdentityType.UKPartnership

  "EmployerTypeOfBusinessPage" - {

    val index = 1
    val srn = srnGen.sample.value

    beRetrievable[IdentityType](EmployerTypeOfBusinessPage(srn, memberIndex, index))

    beSettable[IdentityType](EmployerTypeOfBusinessPage(srn, memberIndex, index))

    beRemovable[IdentityType](EmployerTypeOfBusinessPage(srn, memberIndex, index))

    "Dependent values: section status and were employer contributions made are" - {

      "changing when type of business added" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)

        val result =
          userAnswers.set(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne), typeOfBusiness).success.value

        result.get(EmployerContributionsPage(srn)) must be(Some(true))
        result.get(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne)) must be(Some(typeOfBusiness))
      }

      "not changing when value stays the same" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerNamePage(srn, memberIndex, indexOne), employerName)
          .unsafeSet(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne), typeOfBusiness)

        val result =
          userAnswers.set(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne), typeOfBusiness).success.value

        result.get(EmployerContributionsPage(srn)) must be(Some(true))
        result.get(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne)) must be(Some(typeOfBusiness))
      }

      "changing when value is different" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerNamePage(srn, memberIndex, indexOne), employerName)

        val result =
          userAnswers.set(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne), otherTypeOfBusiness).success.value

        result.get(EmployerTypeOfBusinessPage(srn, memberIndex, indexOne)) must be(Some(otherTypeOfBusiness))
      }
    }
  }

}
