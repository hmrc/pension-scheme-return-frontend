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

package prepop

import pages.nonsipp.loansmadeoroutstanding.Paths.loans
import config.RefinedTypes.Max5000
import models.SchemeId.Srn
import models.UserAnswers
import pages.nonsipp.common.LoanIdentityTypePages
import pages.nonsipp.loansmadeoroutstanding._
import utils.JsonUtils.JsResultOps
import utils.ListUtils.ListOps
import models.UserAnswers.SensitiveJsObject
import play.api.Logger
import play.api.libs.json._

import scala.util.Try

import javax.inject.{Inject, Singleton}

@Singleton
class LoansPrePopulationProcessor @Inject()() {

  private val logger = Logger(getClass)

  def clean(baseUA: UserAnswers, currentUA: UserAnswers)(srn: Srn): Try[UserAnswers] = {

    val baseUaJson = baseUA.data.decryptedValue

    val indexesToDelete =
      (baseUaJson \ "loans" \ "loanTransactions" \ "recipientIdentityType" \ "identityTypes")
        .asOpt[Map[String, String]]
        .fold(Seq.empty[String])(_.keys.toSeq)

    val isLoansEmpty = !baseUA.get(LoanIdentityTypePages(srn)).exists(_.nonEmpty)

    val transformedResult = baseUaJson
      .transform(loans.json.pickBranch)
      .prune(LoansRecordVersionPage(srn).path)
      .pruneIf(LoansMadeOrOutstandingPage(srn).path, isLoansEmpty)
      .prune(ArrearsPrevYearsMap(srn).path)
      .prune(OutstandingArrearsOnLoanPages(srn).path)

    cleanUpOptionalFields(transformedResult, indexesToDelete) match {
      case JsSuccess(value, _) =>
        val uaWithLoansData = currentUA.copy(data = SensitiveJsObject(value.deepMerge(currentUA.data.decryptedValue)))

        val updatedUA = uaWithLoansData
          .get(LoanIdentityTypePages(srn))
          .map(_.keys.toList)
          .toList
          .flatten
          .refine[Max5000.Refined]
          .map(index => LoanPrePopulated(srn, index))
          .foldLeft(Try(uaWithLoansData)) {
            case (ua, loanPrePopulated) => {
              ua.flatMap(_.set(loanPrePopulated, false))
            }
          }

        updatedUA
      case _ => Try(currentUA)
    }
  }

  private def cleanUpOptionalFields(
    initialJsResult: JsResult[JsObject],
    indexesToDelete: Seq[String]
  ): JsResult[JsObject] =
    indexesToDelete.foldLeft(initialJsResult)(
      (accJsResult, index) =>
        accJsResult
          .flatMap(_.transform((Paths.loanTransactions \ "loanAmountPage" \ index \ "optCapRepaymentCY").prune(_)))
          .flatMap(_.transform((Paths.loanTransactions \ "loanAmountPage" \ index \ "optAmountOutstanding").prune(_)))
          .flatMap(_.transform((Paths.loanTransactions \ "loanInterestPage" \ index \ "optIntReceivedCY").prune(_)))
    )
}
