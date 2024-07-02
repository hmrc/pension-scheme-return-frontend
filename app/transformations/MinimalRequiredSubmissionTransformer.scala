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

package transformations

import services.SchemeDateService
import pages.nonsipp.schemedesignatory._
import pages.nonsipp.accountingperiod.Paths.accountingPeriodDetails
import com.google.inject.Singleton
import models.SchemeId.Srn
import cats.implicits.catsSyntaxTuple2Semigroupal
import models.requests.psr._
import pages.nonsipp.accountingperiod.{AccountingPeriodRecordVersionPage, AccountingPeriods}
import pages.nonsipp._
import models._
import pages.nonsipp.schemedesignatory.Paths.schemeDesignatory
import models.requests.DataRequest

import scala.util.Try

import javax.inject.Inject

@Singleton()
class MinimalRequiredSubmissionTransformer @Inject()(schemeDateService: SchemeDateService) {

  def transformToEtmp(srn: Srn, initialUA: UserAnswers)(
    implicit request: DataRequest[_]
  ): Option[MinimalRequiredSubmission] = {

    val currentUA = request.userAnswers
    val reasonForNoBankAccount = currentUA.get(WhyNoBankAccountPage(srn))
    val taxYear = currentUA.get(WhichTaxYearPage(srn))
    val valueOfAssets = currentUA.get(ValueOfAssetsPage(srn, NormalMode))
    val howMuchCash = currentUA.get(HowMuchCashPage(srn, NormalMode))
    val feesCommissionsWagesSalaries = currentUA.get(FeesCommissionsWagesSalariesPage(srn, NormalMode))

    val accountingPeriodsSame = currentUA.get(accountingPeriodDetails) == initialUA.get(accountingPeriodDetails)
    val schemeDesignatorySame = currentUA.get(schemeDesignatory) == initialUA.get(schemeDesignatory)

    (
      schemeDateService.returnPeriods(srn),
      currentUA.get(HowManyMembersPage(srn, request.pensionSchemeId))
    ).mapN { (returnPeriods, schemeMemberNumbers) =>
      MinimalRequiredSubmission(
        reportDetails = ReportDetails(
          fbVersion = None,
          fbstatus = None,
          pstr = request.schemeDetails.pstr,
          periodStart = taxYear.get.from,
          periodEnd = taxYear.get.to,
          compilationOrSubmissionDate = None
        ),
        accountingPeriodDetails = AccountingPeriodDetails(
          if (accountingPeriodsSame) currentUA.get(AccountingPeriodRecordVersionPage(srn)) else None,
          returnPeriods.map(range => range.from -> range.to)
        ),
        schemeDesignatory = SchemeDesignatory(
          if (schemeDesignatorySame) currentUA.get(SchemeDesignatoryRecordVersionPage(srn)) else None,
          reasonForNoBankAccount.isEmpty,
          reasonForNoBankAccount,
          schemeMemberNumbers.noOfActiveMembers,
          schemeMemberNumbers.noOfDeferredMembers,
          schemeMemberNumbers.noOfPensionerMembers,
          valueOfAssets.map(_.moneyAtStart.value),
          valueOfAssets.map(_.moneyAtEnd.value),
          howMuchCash.map(_.moneyAtStart.value),
          howMuchCash.map(_.moneyAtEnd.value),
          feesCommissionsWagesSalaries.map(_.value)
        )
      )
    }
  }

  def transformFromEtmp(
    userAnswers: UserAnswers,
    srn: Srn,
    pensionSchemeId: PensionSchemeId,
    minimalRequiredSubmission: MinimalRequiredSubmission
  ): Try[UserAnswers] =
    for {
      ua0 <- userAnswers.set(
        WhichTaxYearPage(srn),
        DateRange(
          minimalRequiredSubmission.reportDetails.periodStart,
          minimalRequiredSubmission.reportDetails.periodEnd
        )
      )
      ua1 <- ua0.set(
        CheckReturnDatesPage(srn),
        minimalRequiredSubmission.accountingPeriodDetails.accountingPeriods.size == 1 &&
          minimalRequiredSubmission.accountingPeriodDetails.accountingPeriods.head._1
            .isEqual(minimalRequiredSubmission.reportDetails.periodStart) &&
          minimalRequiredSubmission.accountingPeriodDetails.accountingPeriods.head._2
            .isEqual(minimalRequiredSubmission.reportDetails.periodEnd)
      )
      ua2 <- ua1.set(
        AccountingPeriods(srn),
        minimalRequiredSubmission.accountingPeriodDetails.accountingPeriods.toList
          .map(x => DateRange(x._1, x._2))
      )
      openBankAccount = minimalRequiredSubmission.schemeDesignatory.openBankAccount
      ua3 <- ua2.set(
        ActiveBankAccountPage(srn),
        openBankAccount
      )
      ua4 <- {
        if (openBankAccount) {
          Try(ua3)
        } else {
          ua3.set(
            WhyNoBankAccountPage(srn),
            minimalRequiredSubmission.schemeDesignatory.reasonForNoBankAccount.getOrElse("")
          )
        }
      }
      ua5 <- {
        if (minimalRequiredSubmission.schemeDesignatory.totalAssetValueStart.isEmpty ||
          minimalRequiredSubmission.schemeDesignatory.totalAssetValueEnd.isEmpty) {
          Try(ua4)
        } else {
          ua4.set(
            ValueOfAssetsPage(srn, NormalMode),
            MoneyInPeriod(
              Money(minimalRequiredSubmission.schemeDesignatory.totalAssetValueStart.get),
              Money(minimalRequiredSubmission.schemeDesignatory.totalAssetValueEnd.get)
            )
          )
        }
      }
      ua6 <- {
        if (minimalRequiredSubmission.schemeDesignatory.totalCashStart.isEmpty ||
          minimalRequiredSubmission.schemeDesignatory.totalCashEnd.isEmpty) {
          Try(ua5)
        } else {
          ua5.set(
            HowMuchCashPage(srn, NormalMode),
            MoneyInPeriod(
              Money(minimalRequiredSubmission.schemeDesignatory.totalCashStart.get),
              Money(minimalRequiredSubmission.schemeDesignatory.totalCashEnd.get)
            )
          )
        }
      }
      ua7 <- {
        if (minimalRequiredSubmission.schemeDesignatory.totalPayments.isEmpty) {
          Try(ua6)
        } else {
          ua6.set(
            FeesCommissionsWagesSalariesPage(srn, NormalMode),
            Money(minimalRequiredSubmission.schemeDesignatory.totalPayments.get)
          )
        }
      }
      ua8 <- ua7.set(
        HowManyMembersPage(srn, pensionSchemeId),
        SchemeMemberNumbers(
          minimalRequiredSubmission.schemeDesignatory.activeMembers,
          minimalRequiredSubmission.schemeDesignatory.deferredMembers,
          minimalRequiredSubmission.schemeDesignatory.pensionerMembers
        )
      )
      ua9 <- minimalRequiredSubmission.accountingPeriodDetails.recordVersion
        .map(rv => ua8.set(AccountingPeriodRecordVersionPage(srn), rv))
        .getOrElse(Try(ua8))
      ua10 <- minimalRequiredSubmission.schemeDesignatory.recordVersion
        .map(rv => ua9.set(SchemeDesignatoryRecordVersionPage(srn), rv))
        .getOrElse(Try(ua9))
      ua11 <- minimalRequiredSubmission.reportDetails.fbVersion
        .map(rv => ua10.set(FbVersionPage(srn), rv))
        .getOrElse(Try(ua10))
      ua12 <- minimalRequiredSubmission.reportDetails.fbstatus
        .map(rv => ua11.set(FbStatus(srn), rv))
        .getOrElse(Try(ua11))
      ua13 <- minimalRequiredSubmission.reportDetails.compilationOrSubmissionDate
        .map(rv => ua12.set(CompilationOrSubmissionDatePage(srn), rv))
        .getOrElse(Try(ua12))
    } yield {
      ua13
    }
}
