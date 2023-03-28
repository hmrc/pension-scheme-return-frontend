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

package pages

import config.Refined.Max99
import eu.timepit.refined.refineMV
import models.{NameDOB, UserAnswers}
import pages.behaviours.PageBehaviours
import uk.gov.hmrc.domain.Nino
import utils.UserAnswersUtils.UserAnswersOps

import java.time.LocalDate

class MemberDetailsPageSpec extends PageBehaviours {

  private val memberDetails: NameDOB = nameDobGen.sample.value

  "Add member details page with any data" in {

    val srn = srnGen.sample.value
    val nino = Nino("AB123456A")
    val userAnswers = UserAnswers("test")
      .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
      .unsafeSet(MemberDetailsNinoPage(srn, refineMV(1)), nino)
      .unsafeSet(NoNINOPage(srn, refineMV(1)), "test reason")
      .unsafeSet(NationalInsuranceNumberPage(srn, refineMV(1)), true)

    val result = userAnswers.remove(MemberDetailsPage(srn, refineMV(1))).success.value
    result.get(MemberDetailsPage(srn, refineMV(1))) must be(empty)
    result.get(NationalInsuranceNumberPage(srn, refineMV(1))) must be(empty)
    result.get(MemberDetailsNinoPage(srn, refineMV(1))) must be(empty)
    result.get(NoNINOPage(srn, refineMV(1))) must be(empty)
  }
}
