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

package pages

import config.Refined.{Max300, Max5, Max50}
import controllers.TestValues
import eu.timepit.refined.refineMV
import models.NameDOB
import pages.behaviours.PageBehaviours
import pages.nonsipp.employercontributions.EmployerNamePage
import pages.nonsipp.membercontributions.TotalMemberContributionPage
import pages.nonsipp.memberdetails._
import pages.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsPage
import pages.nonsipp.memberreceivedpcls.PensionCommencementLumpSumAmountPage
import pages.nonsipp.membersurrenderedbenefits.SurrenderedBenefitsAmountPage
import pages.nonsipp.membertransferout.ReceivingSchemeNamePage
import pages.nonsipp.receivetransfer.TransferringSchemeNamePage
import utils.UserAnswersUtils.UserAnswersOps

class MemberDetailsPageSpec extends PageBehaviours with TestValues {

  private val memberIndex = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)
  private val secondaryIndexMax5 = refineMV[Max5.Refined](1)

  beRetrievable[NameDOB](MemberDetailsPage(srn, memberIndex))
  beSettable[NameDOB](MemberDetailsPage(srn, memberIndex))
  beRemovable[NameDOB](MemberDetailsPage(srn, memberIndex))

  "Remove data when member details page is removed" in {

    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, memberIndex), memberDetails)
      .unsafeSet(MemberDetailsNinoPage(srn, memberIndex), nino)
      .unsafeSet(NoNINOPage(srn, memberIndex), "test reason")
      .unsafeSet(DoesMemberHaveNinoPage(srn, memberIndex), true)

    val result = userAnswers.remove(MemberDetailsPage(srn, memberIndex)).success.value
    result.get(MemberDetailsPage(srn, memberIndex)) must be(empty)
    result.get(DoesMemberHaveNinoPage(srn, memberIndex)) must be(empty)
    result.get(MemberDetailsNinoPage(srn, memberIndex)) must be(empty)
    result.get(NoNINOPage(srn, memberIndex)) must be(empty)
  }

  "Retain data when member details page is modified" in {
    val srn = srnGen.sample.value
    val nino = ninoGen.sample.value

    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, memberIndex), memberDetails)
      .unsafeSet(DoesMemberHaveNinoPage(srn, memberIndex), true)
      .unsafeSet(MemberDetailsNinoPage(srn, memberIndex), nino)
      .unsafeSet(NoNINOPage(srn, memberIndex), "test reason")

    val result = userAnswers.set(MemberDetailsPage(srn, memberIndex), memberDetails).success.value
    result.get(MemberDetailsPage(srn, memberIndex)) mustBe Some(memberDetails)
    result.get(DoesMemberHaveNinoPage(srn, memberIndex)) mustBe Some(true)
    result.get(MemberDetailsNinoPage(srn, memberIndex)) mustBe Some(nino)
    result.get(NoNINOPage(srn, memberIndex)) mustBe Some("test reason")
  }

  "cleanup member payments when member index 1 is removed" in {
    val userAnswers = defaultUserAnswers
      .unsafeSet(MemberDetailsPage(srn, memberIndex), memberDetails)
      .unsafeSet(EmployerNamePage(srn, memberIndex, secondaryIndex), employerName)
      .unsafeSet(TotalMemberContributionPage(srn, memberIndex), money)
      .unsafeSet(TransferringSchemeNamePage(srn, memberIndex, secondaryIndexMax5), schemeName)
      .unsafeSet(ReceivingSchemeNamePage(srn, memberIndex, secondaryIndexMax5), schemeName)
      .unsafeSet(PensionCommencementLumpSumAmountPage(srn, memberIndex), pensionCommencementLumpSumGen.sample.value)
      .unsafeSet(TotalAmountPensionPaymentsPage(srn, memberIndex), money)
      .unsafeSet(SurrenderedBenefitsAmountPage(srn, memberIndex), money)

    val result = userAnswers.remove(MemberDetailsPage(srn, memberIndex)).success.value

    result.get(EmployerNamePage(srn, memberIndex, secondaryIndex)) must be(empty)
    result.get(TotalMemberContributionPage(srn, memberIndex)) must be(empty)
    result.get(TransferringSchemeNamePage(srn, memberIndex, secondaryIndexMax5)) must be(empty)
    result.get(ReceivingSchemeNamePage(srn, memberIndex, secondaryIndexMax5)) must be(empty)
    result.get(PensionCommencementLumpSumAmountPage(srn, memberIndex)) must be(empty)
    result.get(TotalAmountPensionPaymentsPage(srn, memberIndex)) must be(empty)
    result.get(SurrenderedBenefitsAmountPage(srn, memberIndex)) must be(empty)
  }
}
