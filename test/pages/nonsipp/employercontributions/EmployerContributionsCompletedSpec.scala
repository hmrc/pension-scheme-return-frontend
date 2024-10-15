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

import pages.nonsipp.employercontributions.EmployerContributionsCompleted.EmployerContributionsUserAnswersOps
import config.RefinedTypes._
import eu.timepit.refined.refineMV
import models.{SchemeId, UserAnswers}
import viewmodels.models.SectionCompleted
import pages.behaviours.PageBehaviours

class EmployerContributionsCompletedSpec extends PageBehaviours {

  val srnSample: SchemeId.Srn = srnGen.sample.value

  "EmployerContributionsCYAPage" - {

    val index = refineMV[Max300.Refined](1)
    val secondaryIndex = refineMV[Max50.Refined](1)

    beRetrievable[SectionCompleted.type](EmployerContributionsCompleted(srnSample, index, secondaryIndex))

    beSettable[SectionCompleted.type](EmployerContributionsCompleted(srnSample, index, secondaryIndex))

    beRemovable[SectionCompleted.type](EmployerContributionsCompleted(srnSample, index, secondaryIndex))
  }

  "EmployerContributionsUserAnswersOps" - {

    val index = refineMV[Max300.Refined](1)
    val secondaryIndex = refineMV[Max50.Refined](1)

    val userAnswers = UserAnswers(id = "test-id")
      .set(EmployerContributionsCompleted(srnSample, index, secondaryIndex), SectionCompleted)
      .success
      .value
      .set(EmployerContributionsCompleted(srnSample, index, secondaryIndex), SectionCompleted)
      .success
      .value

    val result = userAnswers.employerContributionsCompleted(srnSample, index)
    result must contain theSameElementsAs List(secondaryIndex)

  }

}
