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

package pages.nonsipp.shares

import controllers.TestValues
import utils.IntUtils.given
import pages.nonsipp.sharesdisposal.HowWereSharesDisposedPage
import utils.UserAnswersUtils.UserAnswersOps
import models.HowSharesDisposed
import pages.behaviours.PageBehaviours

class ClassOfSharesPageSpec extends PageBehaviours with TestValues {

  "ClassOfSharesPage" - {
    val srn = srnGen.sample.value
    val indexOne = 1
    val indexTwo = 2
    val disposalIndexOne = 1
    val disposalIndexTwo = 2

    beRetrievable[String](ClassOfSharesPage(srn, indexOne))

    beSettable[String](ClassOfSharesPage(srn, indexOne))

    beRemovable[String](ClassOfSharesPage(srn, indexOne))

    "cleanup" - {
      val userAnswers =
        defaultUserAnswers
          .unsafeSet(ClassOfSharesPage(srn, indexOne), classOfShares)
          .unsafeSet(ClassOfSharesPage(srn, indexTwo), classOfShares)
          .unsafeSet(HowWereSharesDisposedPage(srn, indexOne, disposalIndexOne), HowSharesDisposed.Transferred)
          .unsafeSet(HowWereSharesDisposedPage(srn, indexOne, disposalIndexTwo), HowSharesDisposed.Transferred)
          .unsafeSet(HowWereSharesDisposedPage(srn, indexTwo, disposalIndexOne), HowSharesDisposed.Transferred)

      s"retain dependant values when this is not a deletion" in {
        val result = ClassOfSharesPage(srn, indexOne).cleanup(Some(""), userAnswers).toOption.value

        result.get(HowWereSharesDisposedPage(srn, indexOne, disposalIndexOne)) must not be None
        result.get(HowWereSharesDisposedPage(srn, indexOne, disposalIndexTwo)) must not be None
        result.get(HowWereSharesDisposedPage(srn, indexTwo, disposalIndexOne)) must not be None
      }

      s"remove dependant values when this page is deleted" in {
        val result = ClassOfSharesPage(srn, indexOne).cleanup(None, userAnswers).toOption.value

        result.get(HowWereSharesDisposedPage(srn, indexOne, disposalIndexOne)) mustBe None
        result.get(HowWereSharesDisposedPage(srn, indexOne, disposalIndexTwo)) mustBe None
        result.get(HowWereSharesDisposedPage(srn, indexTwo, disposalIndexOne)) must not be None
      }
    }
  }
}
