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

package pages.nonsipp.common

import config.Refined.OneTo5000
import eu.timepit.refined.refineMV
import models.{IdentitySubject, IdentityType, Mode, NormalMode, UserAnswers}
import pages.behaviours.PageBehaviours
import utils.UserAnswersUtils.UserAnswersOps

class IdentityTypePageSpec extends PageBehaviours {
  "WhoReceivedLoanPage" - {
    val index = refineMV[OneTo5000](1)
    val identitySubject = IdentitySubject.LoanRecipient

    val srn = srnGen.sample.value
    val mode = modeGen.sample.value

    beRetrievable[IdentityType](IdentityTypePage(srn, index, identitySubject, mode))

    beSettable[IdentityType](IdentityTypePage(srn, index, identitySubject, mode))

    beRemovable[IdentityType](IdentityTypePage(srn, index, identitySubject, mode))

    "must be able to set and retrieve multiple" in {
      val ua = UserAnswers("test")
        .unsafeSet(IdentityTypePage(srn, refineMV[OneTo5000](1), identitySubject, NormalMode), IdentityType.Individual)
        .unsafeSet(IdentityTypePage(srn, refineMV[OneTo5000](2), identitySubject, NormalMode), IdentityType.UKCompany)
        .unsafeSet(
          IdentityTypePage(srn, refineMV[OneTo5000](3), identitySubject, NormalMode),
          IdentityType.UKPartnership
        )

      ua.map(IdentityTypes(srn, IdentitySubject.LoanRecipient)).values.toList mustBe List(
        IdentityType.Individual,
        IdentityType.UKCompany,
        IdentityType.UKPartnership
      )
    }
  }
}
