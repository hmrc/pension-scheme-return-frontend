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
import models.UserAnswers.SensitiveJsObject
import models.SchemeId.Srn
import play.api.libs.json._
import models.UserAnswers
import pages.nonsipp.loansmadeoroutstanding._

import scala.util.{Success, Try}

import javax.inject.{Inject, Singleton}

@Singleton
class LoansPrePopulationProcessor @Inject() {

  def clean(baseUA: UserAnswers, currentUA: UserAnswers)(srn: Srn): Try[UserAnswers] = {

    val baseUaJson = baseUA.data.decryptedValue

    val indexesToDelete =
      (baseUaJson \ "loans" \ "loanTransactions" \ "recipientIdentityType" \ "identityTypes")
        .asOpt[Map[String, String]]
        .fold(Seq.empty[String])(_.keys.toSeq)

    val transformedResult = baseUaJson
      .transform(loans.json.pickBranch)
      .flatMap(_.transform(LoansRecordVersionPage(srn).path.prune(_)))
      .flatMap(_.transform(LoansMadeOrOutstandingPage(srn).path.prune(_)))
      .flatMap(_.transform(ArrearsPrevYearsMap(srn).path.prune(_)))
      .flatMap(_.transform(OutstandingArrearsOnLoanPages(srn).path.prune(_)))

    cleanUpOptionalFields(transformedResult, indexesToDelete) match {
      case JsSuccess(value, _) =>
        Success(currentUA.copy(data = SensitiveJsObject(value.deepMerge(currentUA.data.decryptedValue))))
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
