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

import pages.nonsipp.shares.{SharePrePopulated, _}
import utils.nonsipp.check.SharesCheckStatusUtils.checkSharesSection
import org.scalatest.OptionValues
import models._
import models.IdentitySubject._
import org.scalatest.matchers.must.Matchers
import models.ConditionalYesNo._
import config.RefinedTypes.Max5000
import controllers.{ControllerBaseSpec, ControllerBehaviours}

class SharesCheckStatusUtilsSpec extends ControllerBaseSpec with ControllerBehaviours with Matchers with OptionValues {

  private val didSchemeHoldAnySharesTrue = defaultUserAnswers.unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
  private val didSchemeHoldAnySharesFalse = defaultUserAnswers.unsafeSet(DidSchemeHoldAnySharesPage(srn), false)

  private def addSharesBaseAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(CompanyNameRelatedSharesPage(srn, index), name)
      .unsafeSet(SharesCompanyCrnPage(srn, index), conditionalYesNoCrn)
      .unsafeSet(ClassOfSharesPage(srn, index), classOfShares)
      .unsafeSet(HowManySharesPage(srn, index), totalShares)
      .unsafeSet(CostOfSharesPage(srn, index), money)
      .unsafeSet(SharesIndependentValuationPage(srn, index), true)

  private def addSharesUnquotedAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.Unquoted)
      .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)

  private def addSharesConnectedPartyAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty)

  private def addSharesContributionAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Contribution)
      .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)

  private def addSharesTransferAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Transfer)

  private def addSharesPrePopAnswersChecked(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(SharesTotalIncomePage(srn, index), money)
      .unsafeSet(SharePrePopulated(srn, index), true)

  private def addSharesPrePopAnswersNotChecked(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(SharesTotalIncomePage(srn, index), money)
      .unsafeSet(SharePrePopulated(srn, index), false)

  "checkSharesSection" - {

    "must be true when sharePrePopuleted flag is set to false (not checked)" in {
      val userAnswers =
        addSharesBaseAnswers(
          index1of5000,
          addSharesUnquotedAnswers(
            index1of5000,
            addSharesTransferAnswers(
              index1of5000,
              addSharesBaseAnswers(
                index2of5000,
                addSharesConnectedPartyAnswers(
                  index2of5000,
                  addSharesContributionAnswers(
                    index2of5000,
                    addSharesPrePopAnswersNotChecked(
                      index2of5000,
                      didSchemeHoldAnySharesTrue
                    )
                  )
                )
              )
            )
          )
        )

      checkSharesSection(userAnswers, srn) mustBe true
    }

  }

  "must be false when sharePrePopuleted flag is set to true (checked)" in {
    val userAnswers =
      addSharesBaseAnswers(
        index1of5000,
        addSharesUnquotedAnswers(
          index1of5000,
          addSharesTransferAnswers(
            index1of5000,
            addSharesBaseAnswers(
              index2of5000,
              addSharesConnectedPartyAnswers(
                index2of5000,
                addSharesContributionAnswers(
                  index2of5000,
                  addSharesPrePopAnswersChecked(
                    index2of5000,
                    didSchemeHoldAnySharesTrue
                  )
                )
              )
            )
          )
        )
      )

    checkSharesSection(userAnswers, srn) mustBe false
  }
}
