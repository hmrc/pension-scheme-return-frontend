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

package pages.nonsipp.receivetransfer

import config.Refined.{Max300, Max5}
import controllers.TestValues
import eu.timepit.refined.refineMV
import eu.timepit.refined.api.Refined
import utils.UserAnswersUtils.UserAnswersOps
import pages.behaviours.PageBehaviours
import pages.nonsipp.memberdetails.MemberDetailsPage
import viewmodels.models.{SectionCompleted, SectionStatus}

class TransferringSchemeNamePageSpec extends PageBehaviours with TestValues {

  private val memberIndex = refineMV[Max300.Refined](1)
  val index: Refined[Int, Max5.Refined] = refineMV[Max5.Refined](1)

  "TransferringSchemeNamePage" - {

    beRetrievable[String](TransferringSchemeNamePage(srnGen.sample.value, memberIndex, index))

    beSettable[String](TransferringSchemeNamePage(srnGen.sample.value, memberIndex, index))

    beRemovable[String](TransferringSchemeNamePage(srnGen.sample.value, memberIndex, index))
  }

  "cleanup other fields when removed with index-1" in {
    val userAnswers = defaultUserAnswers
      .unsafeSet(DidSchemeReceiveTransferPage(srn), true)
      .unsafeSet(MemberDetailsPage(srn, memberIndex), memberDetails)
      .unsafeSet(TransferringSchemeNamePage(srn, memberIndex, index), schemeName)
      .unsafeSet(TotalValueTransferPage(srn, memberIndex, index), money)
      .unsafeSet(WhenWasTransferReceivedPage(srn, memberIndex, index), localDate)
      .unsafeSet(DidTransferIncludeAssetPage(srn, memberIndex, index), true)
      .unsafeSet(ReportAnotherTransferInPage(srn, memberIndex, index), false)
      .unsafeSet(TransfersInSectionCompleted(srn, memberIndex, index), SectionCompleted)

    val result = userAnswers.remove(TransferringSchemeNamePage(srn, memberIndex, index)).success.value

    result.get(TransferringSchemeTypePage(srn, memberIndex, index)) must be(empty)
    result.get(TotalValueTransferPage(srn, memberIndex, index)) must be(empty)
    result.get(WhenWasTransferReceivedPage(srn, memberIndex, index)) must be(empty)
    result.get(DidTransferIncludeAssetPage(srn, memberIndex, index)) must be(empty)
    result.get(ReportAnotherTransferInPage(srn, memberIndex, index)) must be(empty)
    result.get(TransfersInSectionCompleted(srn, memberIndex, index)) must be(empty)
  }
}
