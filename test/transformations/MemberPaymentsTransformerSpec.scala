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

package transformations

import cats.implicits.catsSyntaxEitherId
import com.softwaremill.diffx.generic.AutoDerivation
import com.softwaremill.diffx.scalatest.DiffShouldMatcher
import config.Refined.{Max300, Max50}
import controllers.TestValues
import eu.timepit.refined.refineMV
import models.{ConditionalYesNo, IdentityType}
import models.requests.psr._
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import pages.nonsipp.employercontributions._
import pages.nonsipp.memberdetails._
import pages.nonsipp.memberpayments.{
  EmployerContributionsPage,
  UnallocatedEmployerAmountPage,
  UnallocatedEmployerContributionsPage
}
import utils.UserAnswersUtils.UserAnswersOps
import viewmodels.models.{MemberState, SectionCompleted, SectionStatus}

import scala.util.Try

class MemberPaymentsTransformerSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with TestValues
    with DiffShouldMatcher
    with AutoDerivation {

  private val transformer = new MemberPaymentsTransformer()

  private val memberPayments = MemberPayments(
    memberDetails = List(
      MemberDetails(
        personalDetails = MemberPersonalDetails(
          firstName = memberDetails.firstName,
          lastName = memberDetails.lastName,
          nino = Some(nino.value),
          reasonNoNINO = None,
          dateOfBirth = memberDetails.dob
        ),
        employerContributions = List(
          EmployerContributions(
            employerName = employerName,
            employerType = EmployerType.UKCompany(Right(crn.value)),
            totalTransferValue = money.value
          )
        )
      )
    ),
    employerContributionsCompleted = true,
    unallocatedContribsMade = true,
    unallocatedContribAmount = Some(money.value)
  )

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max50.Refined](1)

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index), true)
    .unsafeSet(MemberDetailsNinoPage(srn, index), nino)
    .unsafeSet(MemberStatus(srn, index), MemberState.Active)
    .unsafeSet(EmployerContributionsSectionStatus(srn), SectionStatus.Completed)
    .unsafeSet(EmployerNamePage(srn, index, secondaryIndex), employerName)
    .unsafeSet(EmployerTypeOfBusinessPage(srn, index, secondaryIndex), IdentityType.UKCompany)
    .unsafeSet(EmployerCompanyCrnPage(srn, index, secondaryIndex), ConditionalYesNo(crn.asRight[String]))
    .unsafeSet(TotalEmployerContributionPage(srn, index, secondaryIndex), money)
    .unsafeSet(EmployerContributionsCompleted(srn, index, secondaryIndex), SectionCompleted)
    .unsafeSet(EmployerContributionsPage(srn), true)
    .unsafeSet(UnallocatedEmployerContributionsPage(srn), true)
    .unsafeSet(UnallocatedEmployerAmountPage(srn), money)
    .unsafeSet(EmployerContributionsMemberListPage(srn), true)

  "MemberPaymentsTransformer - To Etmp" - {
    "should return empty List when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn, defaultUserAnswers)
      result shouldMatchTo None
    }

    "should return member payments" in {

      val result = transformer.transformToEtmp(srn, userAnswers)
      result shouldMatchTo Some(memberPayments)
    }
  }

  "MemberPaymentsTransformer - From Etmp" - {
    "should return correct user answers" in {

      val result = transformer.transformFromEtmp(defaultUserAnswers, srn, memberPayments)
      result shouldMatchTo Try(userAnswers)
    }
  }
}
