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

import pages.nonsipp.bonds._
import pages.nonsipp.bonds.Paths.bonds
import models.UserAnswers.SensitiveJsObject
import config.RefinedTypes.OneTo5000
import pages.nonsipp.bondsdisposal.Paths.bondsDisposed
import models.SchemeId.Srn
import eu.timepit.refined.refineV
import play.api.libs.json.{JsObject, JsSuccess}
import models.UserAnswers
import pages.nonsipp.bondsdisposal.BondsDisposalPage

import scala.util.{Success, Try}

import javax.inject.{Inject, Singleton}

@Singleton
class BondsPrePopulationProcessor @Inject() {

  def clean(baseUA: UserAnswers, currentUA: UserAnswers)(srn: Srn): Try[UserAnswers] = {

    val baseUaJson: JsObject = baseUA.data.decryptedValue

    val totalBondsNowHeldOptMap: Option[Map[String, Map[String, Int]]] =
      (baseUaJson \ "bonds" \ "bondTransactions" \ "bondsDisposed" \ "totalNowHeld")
        .asOpt[Map[String, Map[String, Int]]]

    val transformedResult: Try[UserAnswers] = baseUaJson
      .transform(bonds.json.pickBranch)
      .flatMap(_.transform(BondsRecordVersionPage(srn).path.prune(_)))
      .flatMap(_.transform(UnregulatedOrConnectedBondsHeldPage(srn).path.prune(_)))
      .flatMap(_.transform(BondsDisposalPage(srn).path.prune(_)))
      .flatMap(_.transform(bondsDisposed.prune(_)))
      .flatMap(_.transform((Paths.bondTransactions \ "totalIncomeOrReceipts").prune(_))) match {
      case JsSuccess(value, _) =>
        Success(currentUA.copy(data = SensitiveJsObject(value.deepMerge(currentUA.data.decryptedValue))))
      case _ => Try(currentUA)
    }

    totalBondsNowHeldOptMap.fold(transformedResult) { totalBondsNowHeldMap =>
      totalBondsNowHeldMap.foldLeft(transformedResult)((uaResult, totalBondsNowHeldEntry) => {
        val isFullyDisposed = totalBondsNowHeldEntry._2.exists(_._2 == 0)
        if (isFullyDisposed) {
          totalBondsNowHeldEntry._1.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
            case None => uaResult
            case Some(index) => uaResult.flatMap(_.remove(NameOfBondsPage(srn, index)))
          }
        } else {
          uaResult
        }
      })
    }
  }
}
