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

import pages.nonsipp.membercontributions.{MemberContributionsPage, TotalMemberContributionPage}
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.UserAnswers
import pages.behaviours.PageBehaviours

class MemberContributionsPageSpec extends PageBehaviours {

  "MemberContributionsPage" - {

    val srn = srnGen.sample.value

    beRetrievable[Boolean](MemberContributionsPage(srn))

    beSettable[Boolean](MemberContributionsPage(srn))

    beRemovable[Boolean](MemberContributionsPage(srn))

    "cleanup" - {

      val userAnswers =
        UserAnswers("id")
          .unsafeSet(MemberContributionsPage(srn), true)
          .unsafeSet(TotalMemberContributionPage(srn, refineMV(1)), moneyGen.sample.value)
          .unsafeSet(TotalMemberContributionPage(srn, refineMV(2)), moneyGen.sample.value)

      List(Some(true), None).foreach { answer =>
        s"retain total member contributions when answer is $answer" in {

          val result = MemberContributionsPage(srn).cleanup(answer, userAnswers).toOption.value

          result.get(TotalMemberContributionPage(srn, refineMV(1))) must not be None
          result.get(TotalMemberContributionPage(srn, refineMV(2))) must not be None
        }
      }

      "remove all total member contributions when answer is Some(false)" in {

        val result = MemberContributionsPage(srn).cleanup(Some(false), userAnswers).toOption.value

        result.get(TotalMemberContributionPage(srn, refineMV(1))) mustBe None
        result.get(TotalMemberContributionPage(srn, refineMV(2))) mustBe None
      }
    }

  }
}
