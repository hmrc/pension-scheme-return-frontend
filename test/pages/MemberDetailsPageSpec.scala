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

import pages.nonsipp.memberdetails._
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.{NameDOB, UserAnswers}
import pages.behaviours.PageBehaviours

class MemberDetailsPageSpec extends PageBehaviours {

  private val memberDetails: NameDOB = nameDobGen.sample.value

  val srn = srnGen.sample.value
  val nino = ninoGen.sample.value
  val defaultUserAnswers: UserAnswers = UserAnswers("test")

  beRetrievable[NameDOB](MemberDetailsPage(srn, refineMV(1)))
  beSettable[NameDOB](MemberDetailsPage(srn, refineMV(1)))
  beRemovable[NameDOB](MemberDetailsPage(srn, refineMV(1)))

  "Remove data when member details page is removed" in {

    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
      .unsafeSet(MemberDetailsNinoPage(srn, refineMV(1)), nino)
      .unsafeSet(NoNINOPage(srn, refineMV(1)), "test reason")
      .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), true)

    val result = userAnswers.remove(MemberDetailsPage(srn, refineMV(1))).success.value
    result.get(MemberDetailsPage(srn, refineMV(1))) must be(empty)
    result.get(DoesMemberHaveNinoPage(srn, refineMV(1))) must be(empty)
    result.get(MemberDetailsNinoPage(srn, refineMV(1))) must be(empty)
    result.get(NoNINOPage(srn, refineMV(1))) must be(empty)
  }

  "Retain data when member details page is modified" in {
    val srn = srnGen.sample.value
    val nino = ninoGen.sample.value

    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
      .unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), true)
      .unsafeSet(MemberDetailsNinoPage(srn, refineMV(1)), nino)
      .unsafeSet(NoNINOPage(srn, refineMV(1)), "test reason")

    val result = userAnswers.set(MemberDetailsPage(srn, refineMV(1)), memberDetails).success.value
    result.get(MemberDetailsPage(srn, refineMV(1))) mustBe Some(memberDetails)
    result.get(DoesMemberHaveNinoPage(srn, refineMV(1))) mustBe Some(true)
    result.get(MemberDetailsNinoPage(srn, refineMV(1))) mustBe Some(nino)
    result.get(NoNINOPage(srn, refineMV(1))) mustBe Some("test reason")
  }
}
