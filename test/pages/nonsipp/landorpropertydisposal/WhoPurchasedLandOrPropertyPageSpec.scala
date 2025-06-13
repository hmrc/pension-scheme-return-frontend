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

package pages.nonsipp.landorpropertydisposal

import controllers.TestValues
import utils.IntUtils.given
import utils.UserAnswersUtils.UserAnswersOps
import models.{ConditionalYesNo, Crn, IdentityType}
import pages.behaviours.PageBehaviours

class WhoPurchasedLandOrPropertyPageSpec extends PageBehaviours with TestValues {
  private val index = 1
  private val disposalIndexOne = 1
  private val disposalIndexTwo = 2

  private val conditionalCrnYes: ConditionalYesNo[String, Crn] = ConditionalYesNo.yes(crn)

  "Land or property disposal - WhoPurchasedLandOrPropertyPage" - {

    beRetrievable[IdentityType](WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne))

    beSettable[IdentityType](WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne))

    beRemovable[IdentityType](WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne))

    "cleanup with list size 1" - {
      val userAnswers = defaultUserAnswers
        .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne), IdentityType.UKCompany)
        .unsafeSet(CompanyBuyerNamePage(srn, index, disposalIndexOne), companyName)
        .unsafeSet(CompanyBuyerCrnPage(srn, index, disposalIndexOne), conditionalCrnYes)
        .unsafeSet(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndexOne), false)

      s"remove dependant values when current answer is None (removal) and existing answers are present" in {

        val result = WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne)
          .cleanup(None, userAnswers)
          .toOption
          .value

        result.get(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne)) must not be None
        result.get(CompanyBuyerNamePage(srn, index, disposalIndexOne)) mustBe None
        result.get(CompanyBuyerCrnPage(srn, index, disposalIndexOne)) mustBe None
        result.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndexOne)) mustBe None
      }

      s"remove dependant values when current answer is Partnership and existing answer is UKCompany (update)" in {

        val result = WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne)
          .cleanup(Some(IdentityType.UKPartnership), userAnswers)
          .toOption
          .value

        result.get(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne)) must not be None
        result.get(CompanyBuyerNamePage(srn, index, disposalIndexOne)) mustBe None
        result.get(CompanyBuyerCrnPage(srn, index, disposalIndexOne)) mustBe None
        result.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndexOne)) mustBe None
      }
    }

    "cleanup with list size more than 1" - {
      val userAnswers = defaultUserAnswers
        .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne), IdentityType.UKCompany)
        .unsafeSet(CompanyBuyerNamePage(srn, index, disposalIndexOne), companyName)
        .unsafeSet(CompanyBuyerCrnPage(srn, index, disposalIndexOne), conditionalCrnYes)
        .unsafeSet(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndexOne), false)
        .unsafeSet(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexTwo), IdentityType.UKCompany)
        .unsafeSet(CompanyBuyerNamePage(srn, index, disposalIndexTwo), companyName)
        .unsafeSet(CompanyBuyerCrnPage(srn, index, disposalIndexTwo), conditionalCrnYes)
        .unsafeSet(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndexTwo), false)

      s"remove dependant values when current answer is None (removal) and existing answers are present" in {

        val result = WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne)
          .cleanup(None, userAnswers)
          .toOption
          .value

        result.get(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne)) must not be None
        result.get(CompanyBuyerNamePage(srn, index, disposalIndexOne)) mustBe None
        result.get(CompanyBuyerCrnPage(srn, index, disposalIndexOne)) mustBe None
        result.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndexOne)) mustBe None

        result.get(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexTwo)) must not be None
        result.get(CompanyBuyerNamePage(srn, index, disposalIndexTwo)) must not be None
        result.get(CompanyBuyerCrnPage(srn, index, disposalIndexTwo)) must not be None
        result.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndexTwo)) must not be None
      }

      s"remove dependant values when current answer is Partnership and existing answer is UKCompany (update)" in {

        val result = WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne)
          .cleanup(Some(IdentityType.UKPartnership), userAnswers)
          .toOption
          .value

        result.get(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexOne)) must not be None
        result.get(CompanyBuyerNamePage(srn, index, disposalIndexOne)) mustBe None
        result.get(CompanyBuyerCrnPage(srn, index, disposalIndexOne)) mustBe None
        result.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndexOne)) mustBe None

        result.get(WhoPurchasedLandOrPropertyPage(srn, index, disposalIndexTwo)) must not be None
        result.get(CompanyBuyerNamePage(srn, index, disposalIndexTwo)) must not be None
        result.get(CompanyBuyerCrnPage(srn, index, disposalIndexTwo)) must not be None
        result.get(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndexTwo)) must not be None

      }
    }
  }
}
