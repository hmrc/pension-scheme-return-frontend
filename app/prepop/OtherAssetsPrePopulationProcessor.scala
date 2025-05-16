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

import pages.nonsipp.otherassetsdisposal.{OtherAssetsDisposalPage, OtherAssetsDisposalProgress}
import pages.nonsipp.otherassetsheld._
import config.RefinedTypes.{Max5000, OneTo5000}
import models.SchemeId.Srn
import pages.nonsipp.otherassetsdisposal.Paths.assetsDisposed
import eu.timepit.refined.refineV
import models.UserAnswers
import utils.ListUtils.ListOps
import models.UserAnswers.SensitiveJsObject
import pages.nonsipp.otherassetsheld.Paths.otherAssets
import play.api.libs.json.{JsObject, JsSuccess}
import utils.JsonUtils.JsResultOps
import viewmodels.models.SectionJourneyStatus

import scala.util.{Success, Try}

import javax.inject.{Inject, Singleton}

@Singleton
class OtherAssetsPrePopulationProcessor @Inject()() {

  def clean(baseUA: UserAnswers, currentUA: UserAnswers)(srn: Srn): Try[UserAnswers] = {

    val baseUaJson: JsObject = baseUA.data.decryptedValue

    val anyPartAssetStillHeldOptMap: Option[Map[String, Map[String, Boolean]]] =
      (baseUaJson \ "assets" \ "otherAssets" \ "otherAssetTransactions" \ "assetsDisposed" \ "anyPartAssetStillHeld")
        .asOpt[Map[String, Map[String, Boolean]]]

    val isOtherAssetsEmpty = !baseUA.get(WhatIsOtherAssetPages(srn)).exists(_.nonEmpty)

    val transformedResult: Try[UserAnswers] = baseUaJson
      .transform(otherAssets.json.pickBranch)
      .prune(OtherAssetsRecordVersionPage(srn).path)
      .prune(OtherAssetsHeldPage(srn).path)
      .prune(OtherAssetsDisposalPage(srn).path)
      .prune(assetsDisposed)
      .prune(OtherAssetsDisposalProgress.all(srn).path)
      .prune(Paths.otherAssetsTransactions \ "movableSchedule29A")
      .prune(Paths.otherAssetsTransactions \ "totalIncomeOrReceipts") match {
      case JsSuccess(value, _) =>
        Success(currentUA.copy(data = SensitiveJsObject(value.deepMerge(currentUA.data.decryptedValue))))
      case _ => Try(currentUA)
    }

    val cleanedUA = anyPartAssetStillHeldOptMap.fold(transformedResult) { anyPartStillHeldMap =>
      anyPartStillHeldMap.foldLeft(transformedResult)((uaResult, anyPartStillHeldEntry) => {
        val isFullyDisposed = anyPartStillHeldEntry._2.exists(!_._2)
        if (isFullyDisposed) {
          anyPartStillHeldEntry._1.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
            case None => uaResult
            case Some(index) => uaResult.flatMap(_.remove(WhatIsOtherAssetPage(srn, index)))
          }
        } else {
          uaResult
        }
      })
    }

    cleanedUA.flatMap { ua =>
      ua.get(WhatIsOtherAssetPages(srn))
        .map(_.keys.toList)
        .toList
        .flatten
        .refine[Max5000.Refined]
        .map(
          index =>
            (
              OtherAssetsPrePopulated(srn, index),
              OtherAssetsProgress(srn, index)
            )
        )
        .foldLeft(Try(ua)) {
          case (ua, (otherAssetsPrePopulated, otherAssetsProgress)) =>
            ua.flatMap(_.set(otherAssetsPrePopulated, false))
              .flatMap(_.set(otherAssetsProgress, SectionJourneyStatus.Completed))

        }
    }
  }
}
