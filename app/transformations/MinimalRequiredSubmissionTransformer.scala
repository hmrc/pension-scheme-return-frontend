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

import services.SchemeDateService
import pages.nonsipp.schemedesignatory._
import com.google.inject.Singleton
import models.SchemeId.Srn
import cats.implicits.catsSyntaxTuple2Semigroupal
import models.requests.psr.{MinimalRequiredSubmission, ReportDetails, SchemeDesignatory}
import pages.nonsipp.accountingperiod.AccountingPeriods
import pages.nonsipp.{CheckReturnDatesPage, WhichTaxYearPage}
import models._
import models.requests.DataRequest

import scala.util.Try

import javax.inject.Inject

@Singleton()
class MinimalRequiredSubmissionTransformer @Inject()(schemeDateService: SchemeDateService) {

  def transformToEtmp(srn: Srn)(implicit request: DataRequest[_]): Option[MinimalRequiredSubmission] = {
    val reasonForNoBankAccount = request.userAnswers.get(WhyNoBankAccountPage(srn))
    val taxYear = request.userAnswers.get(WhichTaxYearPage(srn))
    val valueOfAssets = request.userAnswers.get(ValueOfAssetsPage(srn, NormalMode))
    val howMuchCash = request.userAnswers.get(HowMuchCashPage(srn, NormalMode))
    val feesCommissionsWagesSalaries = request.userAnswers.get(FeesCommissionsWagesSalariesPage(srn, NormalMode))

    (
      schemeDateService.returnPeriods(srn),
      request.userAnswers.get(HowManyMembersPage(srn, request.pensionSchemeId))
    ).mapN { (returnPeriods, schemeMemberNumbers) =>
      MinimalRequiredSubmission(
        ReportDetails(request.schemeDetails.pstr, taxYear.get.from, taxYear.get.to),
        returnPeriods.map(range => range.from -> range.to),
        SchemeDesignatory(
          openBankAccount = reasonForNoBankAccount.isEmpty,
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
        minimalRequiredSubmission.accountingPeriods.size == 1 &&
          minimalRequiredSubmission.accountingPeriods.head._1
            .isEqual(minimalRequiredSubmission.reportDetails.periodStart) &&
          minimalRequiredSubmission.accountingPeriods.head._2.isEqual(minimalRequiredSubmission.reportDetails.periodEnd)
      )
      ua2 <- ua1.set(
        AccountingPeriods(srn),
        minimalRequiredSubmission.accountingPeriods.toList
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
    } yield {
      ua8
    }
}
