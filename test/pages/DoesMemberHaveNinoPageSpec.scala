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

import play.api.libs.json.Writes._
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsNinoPage, NoNINOPage}
import eu.timepit.refined.refineMV
import uk.gov.hmrc.domain.Nino
import models.UserAnswers
import pages.behaviours.PageBehaviours

class DoesMemberHaveNinoPageSpec extends PageBehaviours {

  val srn = srnGen.sample.value

  "DoesMemberHaveNinoPage" - {

    beRetrievable[Boolean](DoesMemberHaveNinoPage(srn, refineMV(1)))

    beSettable[Boolean](DoesMemberHaveNinoPage(srn, refineMV(1)))

    beRemovable[Boolean](DoesMemberHaveNinoPage(srn, refineMV(1)))

    "cleanup no NINO page if page value is true" in {

      val userAnswers = UserAnswers("test").set(NoNINOPage(srn, refineMV(1)), "test reason").success.value
      userAnswers.get(NoNINOPage(srn, refineMV(1))) must be(Some("test reason"))

      val result = userAnswers.set(DoesMemberHaveNinoPage(srn, refineMV(1)), true).success.value
      result.get(NoNINOPage(srn, refineMV(1))) must be(empty)
    }

    "cleanup enter NINO page if page value is false" in {

      val nino = Nino("AB123456A")
      val userAnswers = UserAnswers("test").set(MemberDetailsNinoPage(srn, refineMV(1)), nino).success.value
      userAnswers.get(MemberDetailsNinoPage(srn, refineMV(1))) must be(Some(nino))

      val result = userAnswers.set(DoesMemberHaveNinoPage(srn, refineMV(1)), false).success.value
      result.get(MemberDetailsNinoPage(srn, refineMV(1))) must be(empty)
    }

    "cleanup both enter NINO page and no NINO page if page value is missing" in {

      val nino = Nino("AB123456A")
      val userAnswers = UserAnswers("test")
        .set(MemberDetailsNinoPage(srn, refineMV(1)), nino)
        .success
        .value
        .set(NoNINOPage(srn, refineMV(1)), "test reason")
        .success
        .value
      userAnswers.get(MemberDetailsNinoPage(srn, refineMV(1))) must be(Some(nino))
      userAnswers.get(NoNINOPage(srn, refineMV(1))) must be(Some("test reason"))

      val result = userAnswers.remove(DoesMemberHaveNinoPage(srn, refineMV(1))).success.value
      result.get(MemberDetailsNinoPage(srn, refineMV(1))) must be(empty)
      result.get(NoNINOPage(srn, refineMV(1))) must be(empty)
    }
  }
}
