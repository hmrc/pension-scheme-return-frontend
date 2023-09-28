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

import cats.implicits.catsSyntaxTuple2Semigroupal
import com.google.inject.Singleton
import models.NormalMode
import models.SchemeId.Srn
import models.requests.DataRequest
import models.requests.psr.{MinimalRequiredSubmission, ReportDetails, SchemeDesignatory}
import pages.nonsipp.WhichTaxYearPage
import pages.nonsipp.schemedesignatory._
import services.SchemeDateService

import javax.inject.Inject

@Singleton()
class MinimalRequiredSubmissionTransformer @Inject()(schemeDateService: SchemeDateService) {

  def transform(srn: Srn)(implicit request: DataRequest[_]): Option[MinimalRequiredSubmission] = {
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
}
