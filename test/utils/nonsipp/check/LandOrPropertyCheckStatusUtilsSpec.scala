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

package utils.nonsipp.check

import models.IdentityType._
import pages.nonsipp.landorproperty._
import utils.nonsipp.check.LandOrPropertyCheckStatusUtils.checkLandOrPropertyRecord
import org.scalatest.OptionValues
import models._
import pages.nonsipp.common._
import models.IdentitySubject._
import org.scalatest.matchers.must.Matchers
import models.ConditionalYesNo._
import config.RefinedTypes.Max5000
import controllers.{ControllerBaseSpec, ControllerBehaviours}

class LandOrPropertyCheckStatusUtilsSpec
    extends ControllerBaseSpec
    with ControllerBehaviours
    with Matchers
    with OptionValues {

  private val conditionalYesNoLRTN: ConditionalYesNo[String, String] = ConditionalYesNo.yes("landRegistryTitleNumber")

  private def addLOPBaseAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(LandPropertyInUKPage(srn, index), true)
      .unsafeSet(LandOrPropertyChosenAddressPage(srn, index), address)
      .unsafeSet(LandRegistryTitleNumberPage(srn, index), conditionalYesNoLRTN)
      .unsafeSet(LandOrPropertyTotalCostPage(srn, index), money)

  private def addNonPrePopRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addLOPBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Acquisition)
      .unsafeSet(IdentityTypePage(srn, index, LoanRecipient), Individual)

  private def addUncheckedRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addLOPBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Contribution)
      .unsafeSet(IdentityTypePage(srn, index, LoanRecipient), UKCompany)
      .unsafeSet(LandOrPropertyPrePopulated(srn, index), false)

  private def addCheckedRecord(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    addLOPBaseAnswers(index, userAnswers)
      .unsafeSet(WhyDoesSchemeHoldLandPropertyPage(srn, index), SchemeHoldLandProperty.Transfer)
      .unsafeSet(IdentityTypePage(srn, index, LoanRecipient), UKPartnership)
      .unsafeSet(LandOrPropertyPrePopulated(srn, index), true)

  "checkLoansRecord" - {

    "must be true" - {

      "when record is (unchecked)" in {
        val userAnswers = addUncheckedRecord(index1of5000, defaultUserAnswers)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe true
      }
    }

    "must be false" - {

      "when record is (checked)" in {
        val userAnswers = addCheckedRecord(index1of5000, defaultUserAnswers)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when record is (non-pre-pop)" in {
        val userAnswers = addNonPrePopRecord(index1of5000, defaultUserAnswers)

        checkLandOrPropertyRecord(userAnswers, srn, index1of5000) mustBe false
      }
    }

  }

}
