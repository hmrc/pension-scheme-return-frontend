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

package pages.nonsipp.unregulatedorconnectedbonds

import config.Refined.Max5000
import controllers.TestValues
import eu.timepit.refined.refineMV
import models.UserAnswers
import pages.behaviours.PageBehaviours
import utils.UserAnswersUtils.UserAnswersOps

class NameOfBondsPageSpec extends PageBehaviours with TestValues {

  private val index = refineMV[Max5000.Refined](1)
  private val index2 = refineMV[Max5000.Refined](2)

  "NameOfBondsPage" - {

    beRetrievable[String](NameOfBondsPage(srn, refineMV(1)))

    beSettable[String](NameOfBondsPage(srn, refineMV(1)))

    beRemovable[String](NameOfBondsPage(srn, refineMV(1)))
  }

  "cleanup list size of 1" - {

    val userAnswers =
      UserAnswers("id")
        .unsafeSet(NameOfBondsPage(srn, index), otherName)
        .unsafeSet(IncomeFromBondsPage(srn, index), money)
        .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)

    s"remove index" in {
      val result =
        NameOfBondsPage(srn, index).cleanup(None, userAnswers).toOption.value
      result.get(IncomeFromBondsPage(srn, index)) mustBe None
      result.get(UnregulatedOrConnectedBondsHeldPage(srn)) mustBe None
    }
  }

  "cleanup list size greater than 1" - {

    val userAnswers =
      UserAnswers("id")
        .unsafeSet(NameOfBondsPage(srn, index), otherName)
        .unsafeSet(IncomeFromBondsPage(srn, index), money)
        .unsafeSet(NameOfBondsPage(srn, index2), otherName)
        .unsafeSet(IncomeFromBondsPage(srn, index2), money)
        .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)

    s"remove index" in {
      val result =
        NameOfBondsPage(srn, index).cleanup(None, userAnswers).toOption.value
      result.get(IncomeFromBondsPage(srn, index)) mustBe None
      result.get(IncomeFromBondsPage(srn, index2)) must not be None
      result.get(UnregulatedOrConnectedBondsHeldPage(srn)) must not be None
    }
  }
}
