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

import pages.nonsipp.shares._
import models.UserAnswers.SensitiveJsObject
import config.RefinedTypes.OneTo5000
import models.SchemeId.Srn
import eu.timepit.refined.refineV
import pages.nonsipp.shares.Paths.shares
import pages.nonsipp.sharesdisposal.SharesDisposalPage
import play.api.libs.json._
import models.UserAnswers
import pages.nonsipp.sharesdisposal.Paths.disposedSharesTransaction

import scala.util.{Success, Try}

import javax.inject.{Inject, Singleton}

@Singleton
class SharesPrePopulationProcessor @Inject() {

  def clean(baseUA: UserAnswers, currentUA: UserAnswers)(srn: Srn): Try[UserAnswers] = {

    val baseUaJson = baseUA.data.decryptedValue

    val totalSharesNowHeldOptMap =
      (baseUaJson \ "shares" \ "shareTransactions" \ "disposedSharesTransaction" \ "totalSharesNowHeld")
        .asOpt[Map[String, Map[String, Int]]]

    val transformedResult = baseUaJson
      .transform(shares.json.pickBranch)
      .flatMap(_.transform(SharesRecordVersionPage(srn).path.prune(_)))
      .flatMap(_.transform(DidSchemeHoldAnySharesPage(srn).path.prune(_)))
      .flatMap(_.transform(SharesDisposalPage(srn).path.prune(_)))
      .flatMap(_.transform(disposedSharesTransaction.prune(_)))
      .flatMap(_.transform(SharesTotalIncomePages(srn).path.prune(_))) match {
      case JsSuccess(value, _) =>
        Success(currentUA.copy(data = SensitiveJsObject(value.deepMerge(currentUA.data.decryptedValue))))
      case _ => Try(currentUA)
    }

    totalSharesNowHeldOptMap.fold(transformedResult) { totalSharesNowHeldMap =>
      totalSharesNowHeldMap.foldLeft(transformedResult)((uaResult, totalSharesNowHeldEntry) => {
        val isFullyDisposed = totalSharesNowHeldEntry._2.exists(_._2 == 0)
        if (isFullyDisposed) {
          totalSharesNowHeldEntry._1.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
            case None => uaResult
            case Some(index) => uaResult.flatMap(_.remove(sharesPages(srn, index, isLastRecord = true)))
          }
        } else {
          uaResult
        }
      })
    }

  }
}
