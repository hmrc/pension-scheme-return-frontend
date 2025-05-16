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

package pages.nonsipp.otherassetsheld

import pages.nonsipp.otherassetsdisposal._
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import models.{HowDisposed, UserAnswers}
import viewmodels.models.SectionJourneyStatus
import pages.behaviours.PageBehaviours
import config.RefinedTypes.{Max50, Max5000}
import controllers.TestValues

class WhatIsOtherAssetPageSpec extends PageBehaviours with TestValues {

  private val index = refineMV[Max5000.Refined](1)
  private val index2 = refineMV[Max5000.Refined](2)
  private val disposalIndex1 = refineMV[Max50.Refined](1)
  private val disposalIndex2 = refineMV[Max50.Refined](2)

  "WhatIsOtherAssetPage" - {

    beRetrievable[String](WhatIsOtherAssetPage(srn, refineMV(1)))

    beSettable[String](WhatIsOtherAssetPage(srn, refineMV(1)))

    beRemovable[String](WhatIsOtherAssetPage(srn, refineMV(1)))
  }

  "cleanup list size of 1" - {

    val userAnswers =
      UserAnswers("id")
        .unsafeSet(WhatIsOtherAssetPage(srn, index), otherName)
        .unsafeSet(IncomeFromAssetPage(srn, index), money)
        .unsafeSet(OtherAssetsHeldPage(srn), true)
        .unsafeSet(OtherAssetsListPage(srn), true)

    "remove index" in {
      val result =
        WhatIsOtherAssetPage(srn, index).cleanup(None, userAnswers).toOption.value
      result.get(IncomeFromAssetPage(srn, index)) mustBe None
      result.get(OtherAssetsHeldPage(srn)) mustBe None
      result.get(OtherAssetsListPage(srn)) mustBe None
    }
  }

  "cleanup list size greater than 1" - {

    val userAnswers =
      UserAnswers("id")
        .unsafeSet(WhatIsOtherAssetPage(srn, index), otherName)
        .unsafeSet(IncomeFromAssetPage(srn, index), money)
        .unsafeSet(OtherAssetsProgress(srn, index), SectionJourneyStatus.Completed)
        .unsafeSet(WhatIsOtherAssetPage(srn, index2), otherName)
        .unsafeSet(IncomeFromAssetPage(srn, index2), money)
        .unsafeSet(OtherAssetsProgress(srn, index2), SectionJourneyStatus.Completed)
        .unsafeSet(OtherAssetsHeldPage(srn), true)
        .unsafeSet(OtherAssetsListPage(srn), true)
        // Disposal details:
        .unsafeSet(OtherAssetsDisposalPage(srn), true)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, index, disposalIndex1), HowDisposed.Transferred)
        .unsafeSet(HowWasAssetDisposedOfPage(srn, index, disposalIndex2), HowDisposed.Other(otherDetails))
        .unsafeSet(AnyPartAssetStillHeldPage(srn, index, disposalIndex1), true)
        .unsafeSet(AnyPartAssetStillHeldPage(srn, index, disposalIndex2), false)
        .unsafeSet(OtherAssetsDisposalProgress(srn, index, disposalIndex1), SectionJourneyStatus.Completed)
        .unsafeSet(OtherAssetsDisposalProgress(srn, index, disposalIndex2), SectionJourneyStatus.Completed)

    "remove index" in {
      val result =
        WhatIsOtherAssetPage(srn, index).cleanup(None, userAnswers).toOption.value
      result.get(IncomeFromAssetPage(srn, index)) mustBe None
      result.get(OtherAssetsProgress(srn, index)) mustBe None
      result.get(IncomeFromAssetPage(srn, index2)) must not be None
      result.get(OtherAssetsProgress(srn, index2)) must not be None
      result.get(OtherAssetsHeldPage(srn)) must not be None
      result.get(OtherAssetsListPage(srn)) must not be None
    }

    "remove disposal" in {
      val result =
        WhatIsOtherAssetPage(srn, index).cleanup(None, userAnswers).toOption.value
      result.get(IncomeFromAssetPage(srn, index)) mustBe None
      result.get(OtherAssetsDisposalPage(srn)) mustBe None
      result.get(HowWasAssetDisposedOfPage(srn, index, disposalIndex1)) mustBe None
      result.get(HowWasAssetDisposedOfPage(srn, index, disposalIndex2)) mustBe None
      result.get(AnyPartAssetStillHeldPage(srn, index, disposalIndex1)) mustBe None
      result.get(AnyPartAssetStillHeldPage(srn, index, disposalIndex2)) mustBe None
      result.get(OtherAssetsDisposalProgress(srn, index, disposalIndex1)) mustBe None
      result.get(OtherAssetsDisposalProgress(srn, index, disposalIndex2)) mustBe None
    }
  }
}
