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

package controllers

import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, SchemeDesignatoryRecordVersionPage}
import pages.nonsipp.memberdetails._
import config.Refined.{Max3, Max300}
import viewmodels.models.MemberState.New
import eu.timepit.refined.refineMV
import pages.nonsipp.accountingperiod.{AccountingPeriodPage, AccountingPeriodRecordVersionPage}
import models.{DateRange, NormalMode}
import pages.nonsipp.declaration.PensionSchemeDeclarationPage
import viewmodels.models.{DeclarationViewModel, SectionCompleted, Submitted}
import pages.nonsipp._
import org.scalatest.OptionValues

import java.time.{LocalDate, LocalDateTime}
import models.UserAnswers

trait TestUserAnswers extends ControllerBaseSpec with TestValues {
  _: OptionValues =>
  // Test data for various UserAnswers
  val currentReturnTaxYear: DateRange = DateRange(
    from = LocalDate.of(2022, 4, 6),
    to = LocalDate.of(2023, 4, 5)
  )

  val previousReturnTaxYear: DateRange = DateRange(
    from = LocalDate.of(2021, 4, 6),
    to = LocalDate.of(2022, 4, 5)
  )

  val index1of3: Max3 = refineMV(1)
  val recordVersion = "001"
  val currentReturnTaxYearSubmissionDate: LocalDateTime = LocalDateTime.of(2023, 4, 5, 0, 0, 0)
  val declarationData: DeclarationViewModel = DeclarationViewModel("PSP", "20000008", Some("A0000001"))
  val index1of300: Max300 = refineMV(1)

  // UserAnswers for the current return from the current tax year
  val currentTaxYearUserAnswersWithFewMembers: UserAnswers = defaultUserAnswers
    .unsafeSet(WhichTaxYearPage(srn), currentReturnTaxYear)
    .unsafeSet(ActiveBankAccountPage(srn), true)
    .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersUnderThreshold)

  val currentTaxYearUserAnswersWithManyMembers: UserAnswers = currentTaxYearUserAnswersWithFewMembers
    .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)

  val noTaxYearUserAnswers: UserAnswers = currentTaxYearUserAnswersWithManyMembers
    .unsafeRemove(WhichTaxYearPage(srn))

  // UserAnswers for the previous return from the current tax year
  val skippedUserAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(WhichTaxYearPage(srn), currentReturnTaxYear)
    .unsafeSet(CheckReturnDatesPage(srn), true)
    .unsafeSet(AccountingPeriodPage(srn, index1of3, NormalMode), currentReturnTaxYear)
    .unsafeSet(AccountingPeriodRecordVersionPage(srn), recordVersion)
    .unsafeSet(ActiveBankAccountPage(srn), true)
    .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)
    .unsafeSet(SchemeDesignatoryRecordVersionPage(srn), recordVersion)
    .unsafeSet(FbVersionPage(srn), recordVersion)
    .unsafeSet(FbStatus(srn), Submitted)
    .unsafeSet(CompilationOrSubmissionDatePage(srn), currentReturnTaxYearSubmissionDate)
    .unsafeSet(PensionSchemeDeclarationPage(srn), declarationData)

  val fullUserAnswers: UserAnswers = skippedUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index1of300), false)
    .unsafeSet(NoNINOPage(srn, index1of300), noninoReason)
    .unsafeSet(MemberStatus(srn, index1of300), New)
    .unsafeSet(MemberDetailsCompletedPage(srn, index1of300), SectionCompleted)

  val previousTaxYearUserAnswersWithFewMembersWithMemberDetails: UserAnswers = defaultUserAnswers
    .unsafeSet(WhichTaxYearPage(srn), previousReturnTaxYear)
    .unsafeSet(ActiveBankAccountPage(srn), true)
    .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersUnderThreshold)
    .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
    .unsafeSet(DoesMemberHaveNinoPage(srn, index1of300), false)
    .unsafeSet(NoNINOPage(srn, index1of300), noninoReason)
    .unsafeSet(MemberStatus(srn, index1of300), New)
    .unsafeSet(MemberDetailsCompletedPage(srn, index1of300), SectionCompleted)

  val previousTaxYearUserAnswersWithManyMembers: UserAnswers = defaultUserAnswers
    .unsafeSet(WhichTaxYearPage(srn), previousReturnTaxYear)
    .unsafeSet(ActiveBankAccountPage(srn), true)
    .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)

  val previousTaxYearUserAnswersWithManyMembersWithMemberDetails: UserAnswers =
    previousTaxYearUserAnswersWithManyMembers
      .unsafeSet(MemberDetailsPage(srn, index1of300), memberDetails)
      .unsafeSet(DoesMemberHaveNinoPage(srn, index1of300), false)
      .unsafeSet(NoNINOPage(srn, index1of300), noninoReason)
      .unsafeSet(MemberStatus(srn, index1of300), New)
      .unsafeSet(MemberDetailsCompletedPage(srn, index1of300), SectionCompleted)
}
