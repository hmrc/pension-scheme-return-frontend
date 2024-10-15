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

package pages.nonsipp.membertransferout

import pages.nonsipp.memberdetails.MemberDetailsPage
import eu.timepit.refined.refineMV
import utils.UserAnswersUtils.UserAnswersOps
import eu.timepit.refined.api.Refined
import pages.behaviours.PageBehaviours
import config.RefinedTypes._
import controllers.TestValues

class ReceivingSchemeNamePageSpec extends PageBehaviours with TestValues {

  private val memberIndex = refineMV[Max300.Refined](1)
  val index: Refined[Int, Max5.Refined] = refineMV[Max5.Refined](1)

  "ReceivingSchemeNamePage" - {

    val index = refineMV[OneTo300](1)
    val transferIndex = refineMV[OneTo5](1)
    val srnSample = srnGen.sample.value

    beRetrievable[String](ReceivingSchemeNamePage(srnSample, index, transferIndex))

    beSettable[String](ReceivingSchemeNamePage(srnSample, index, transferIndex))

    beRemovable[String](ReceivingSchemeNamePage(srnSample, index, transferIndex))
  }

  "cleanup other fields when removed with index-1" in {
    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, memberIndex), memberDetails)
      .unsafeSet(WhenWasTransferMadePage(srn, memberIndex, index), localDate)
      .unsafeSet(ReportAnotherTransferOutPage(srn, memberIndex, index), false)

    val result = userAnswers.remove(ReceivingSchemeNamePage(srn, memberIndex, index)).success.value

    result.get(WhenWasTransferMadePage(srn, memberIndex, index)) must be(empty)
    result.get(ReportAnotherTransferOutPage(srn, memberIndex, index)) must be(empty)
    result.get(ReceivingSchemeTypePage(srn, memberIndex, index)) must be(empty)
  }
}
