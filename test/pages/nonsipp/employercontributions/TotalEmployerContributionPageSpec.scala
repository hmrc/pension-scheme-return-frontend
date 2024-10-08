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

import config.Refined._
import controllers.TestValues
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.Money
import viewmodels.models.SectionStatus
import pages.behaviours.PageBehaviours

class TotalEmployerContributionPageSpec extends PageBehaviours with TestValues {

  "TotalEmployerContributionPage" - {

    val memberIndex = refineMV[Max300.Refined](1)
    val secondaryIndex = refineMV[Max50.Refined](1)

    beRetrievable[Money](TotalEmployerContributionPage(srnGen.sample.value, memberIndex, secondaryIndex))

    beSettable[Money](TotalEmployerContributionPage(srnGen.sample.value, memberIndex, secondaryIndex))

    beRemovable[Money](TotalEmployerContributionPage(srnGen.sample.value, memberIndex, secondaryIndex))

    "Dependent values: section status and were employer contributions made are" - {

      "changing when type of business added" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)

        val result = userAnswers
          .set(TotalEmployerContributionPage(srn, memberIndex, secondaryIndex), money)
          .success
          .value

        result.get(EmployerContributionsPage(srn)) must be(Some(true))
        result.get(TotalEmployerContributionPage(srn, memberIndex, secondaryIndex)) must be(Some(money))
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.InProgress))
      }

      "not changing when value stays the same" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerNamePage(srn, memberIndex, secondaryIndex), employerName)
          .unsafeSet(TotalEmployerContributionPage(srn, memberIndex, secondaryIndex), money)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)

        val result = userAnswers
          .set(TotalEmployerContributionPage(srn, memberIndex, secondaryIndex), money)
          .success
          .value

        result.get(EmployerContributionsPage(srn)) must be(Some(true))
        result.get(TotalEmployerContributionPage(srn, memberIndex, secondaryIndex)) must be(Some(money))
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.Completed))
      }

      "changing when value is different" in {
        val otherMoney: Money = Money(money.value + 1)
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(TotalEmployerContributionPage(srn, memberIndex, secondaryIndex), money)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)

        val result =
          userAnswers
            .set(TotalEmployerContributionPage(srn, memberIndex, secondaryIndex), otherMoney)
            .success
            .value

        result.get(TotalEmployerContributionPage(srn, memberIndex, secondaryIndex)) must be(
          Some(otherMoney)
        )
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.InProgress))
      }
    }
  }
}
