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
import utils.UserAnswersUtils.UserAnswersOps
import models.{ConditionalYesNo, Utr}
import viewmodels.models.SectionStatus
import pages.behaviours.PageBehaviours

class PartnershipEmployerUtrPageSpec extends PageBehaviours with TestValues {

  "PartnershipEmployerUtrPage" - {

    val memberIndex = refineMV[Max300.Refined](1)
    val secondaryIndex = refineMV[Max50.Refined](1)

    beRetrievable[ConditionalYesNo[String, Utr]](
      PartnershipEmployerUtrPage(srnGen.sample.value, memberIndex, secondaryIndex)
    )

    beSettable[ConditionalYesNo[String, Utr]](
      PartnershipEmployerUtrPage(srnGen.sample.value, memberIndex, secondaryIndex)
    )

    beRemovable[ConditionalYesNo[String, Utr]](
      PartnershipEmployerUtrPage(srnGen.sample.value, memberIndex, secondaryIndex)
    )

    "Dependent values: section status and were employer contributions made are" - {

      "changing when type of business added" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)

        val result = userAnswers
          .set(PartnershipEmployerUtrPage(srn, memberIndex, secondaryIndex), ConditionalYesNo.yes[String, Utr](utr))
          .success
          .value

        result.get(EmployerContributionsPage(srn)) must be(Some(true))
        result.get(PartnershipEmployerUtrPage(srn, memberIndex, secondaryIndex)) must be(
          Some(ConditionalYesNo.yes[String, Utr](utr))
        )
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.InProgress))
        result.get(EmployerContributionsMemberListPage(srn)) must be(empty)
      }

      "not changing when value stays the same" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(EmployerNamePage(srn, memberIndex, secondaryIndex), employerName)
          .unsafeSet(
            PartnershipEmployerUtrPage(srn, memberIndex, secondaryIndex),
            ConditionalYesNo.yes[String, Utr](utr)
          )
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)
          .unsafeSet(EmployerContributionsMemberListPage(srn), true)

        val result = userAnswers
          .set(PartnershipEmployerUtrPage(srn, memberIndex, secondaryIndex), ConditionalYesNo.yes[String, Utr](utr))
          .success
          .value

        result.get(EmployerContributionsPage(srn)) must be(Some(true))
        result.get(PartnershipEmployerUtrPage(srn, memberIndex, secondaryIndex)) must be(
          Some(ConditionalYesNo.yes[String, Utr](utr))
        )
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.Completed))
        result.get(EmployerContributionsMemberListPage(srn)) must be(Some(true))
      }

      "changing when value is different" in {
        val userAnswers = defaultUserAnswers
          .unsafeSet(EmployerContributionsPage(srn), true)
          .unsafeSet(
            PartnershipEmployerUtrPage(srn, memberIndex, secondaryIndex),
            ConditionalYesNo.yes[String, Utr](utr)
          )
          .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)
          .unsafeSet(EmployerContributionsMemberListPage(srn), true)

        val result =
          userAnswers
            .set(
              PartnershipEmployerUtrPage(srn, memberIndex, secondaryIndex),
              ConditionalYesNo.no[String, Utr](noUtrReason)
            )
            .success
            .value

        result.get(PartnershipEmployerUtrPage(srn, memberIndex, secondaryIndex)) must be(
          Some(ConditionalYesNo.no[String, Utr](noUtrReason))
        )
        result.get(EmployerContributionsSectionStatus(srn)) must be(Some(SectionStatus.InProgress))
        result.get(EmployerContributionsMemberListPage(srn)) must be(empty)
      }
    }

  }

}
