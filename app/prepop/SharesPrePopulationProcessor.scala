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
import models.UserAnswers
import pages.nonsipp.sharesdisposal.Paths.disposedSharesTransaction
import utils.JsonUtils.JsResultOps
import play.api.Logger
import play.api.libs.json._

import scala.util.{Success, Try}

import javax.inject.{Inject, Singleton}

@Singleton
class SharesPrePopulationProcessor @Inject()() {

  private val logger = Logger(getClass)

  def clean(baseUA: UserAnswers, currentUA: UserAnswers)(srn: Srn): Try[UserAnswers] = {

    val baseUaJson = baseUA.data.decryptedValue

    val totalSharesNowHeldOptMap: Option[Map[String, Map[String, Int]]] =
      (baseUaJson \ "shares" \ "shareTransactions" \ "disposedSharesTransaction" \ "totalSharesNowHeld")
        .asOpt[Map[String, Map[String, Int]]]

    val sharesJson: JsResult[JsObject] = baseUaJson.transform(shares.json.pickBranch)
    val isSharesEmpty = !baseUA.get(TypeOfSharesHeldPages(srn)).exists(_.nonEmpty)

    val transformedResult: Try[UserAnswers] = sharesJson
      .prune(SharesRecordVersionPage(srn).path)
      .pruneIf(DidSchemeHoldAnySharesPage(srn).path, isSharesEmpty)
      .prune(SharesDisposalPage(srn).path)
      .prune(disposedSharesTransaction)
      .prune(SharesTotalIncomePages(srn).path) match {
      case JsSuccess(value, _) =>
        Success(currentUA.copy(data = SensitiveJsObject(value.deepMerge(currentUA.data.decryptedValue))))
      case _ => Try(currentUA)
    }

    for {
      transformedAnswers <- transformedResult

      sharesPagesToRemove = for {
        totalSharesNowHeldMap <- totalSharesNowHeldOptMap.toList
        (index, totalSharesNowHeld) <- totalSharesNowHeldMap.toList
        isFullyDisposed = totalSharesNowHeld.exists(_._2 == 0)
        refinedIndex <- index.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption).toList
        sharesPagesToRemove <- if (isFullyDisposed) sharesPages(srn, refinedIndex, isLastRecord = false) else Nil
      } yield sharesPagesToRemove

      cleanedAnswers <- transformedAnswers.remove(sharesPagesToRemove)

      // TODO: check if any shares, if not, prune DidSchemeHoldAnySharesPage
      prePopFlagsToAdd = for {
        typeOfSharesPages <- cleanedAnswers.get(TypeOfSharesHeldPages(srn)).toList
        index <- typeOfSharesPages.keys
        refinedIndex <- index.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption).toList
        prePopFlagsToAdd = SharePrePopulated(srn, refinedIndex)
      } yield prePopFlagsToAdd

      prePopAnswers <- prePopFlagsToAdd.foldLeft(Try(cleanedAnswers)) { (answers, toAdd) =>
        answers.flatMap(_.set(toAdd, false))
      }

    } yield prePopAnswers
  }
}
