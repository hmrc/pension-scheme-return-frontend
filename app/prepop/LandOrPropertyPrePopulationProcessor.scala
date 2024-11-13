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

import models.UserAnswers.SensitiveJsObject
import config.RefinedTypes.OneTo5000
import pages.nonsipp.landorpropertydisposal.Paths.disposalPropertyTransaction
import pages.nonsipp.landorproperty._
import pages.nonsipp.landorpropertydisposal.LandOrPropertyDisposalPage
import eu.timepit.refined.refineV
import play.api.libs.json.JsSuccess
import models.UserAnswers
import pages.nonsipp.landorproperty.Paths.landOrProperty
import models.SchemeId.Srn

import scala.util.{Success, Try}

import javax.inject.{Inject, Singleton}

@Singleton
class LandOrPropertyPrePopulationProcessor @Inject() {

  def clean(baseUA: UserAnswers, currentUA: UserAnswers)(srn: Srn): Try[UserAnswers] = {

    val baseUaJson = baseUA.data.decryptedValue
    val portionStillHeldOptMap =
      (baseUaJson \ "assets" \ "landOrProperty" \ "landOrPropertyTransactions" \ "disposedPropertyTransaction" \ "portionStillHeld")
        .asOpt[Map[String, Map[String, Boolean]]]

    val transformedResult = baseUaJson
      .transform(landOrProperty.json.pickBranch)
      .flatMap(_.transform(LandOrPropertyRecordVersionPage(srn).path.prune(_)))
      .flatMap(_.transform(LandOrPropertyHeldPage(srn).path.prune(_)))
      .flatMap(_.transform(LandOrPropertyDisposalPage(srn).path.prune(_)))
      .flatMap(_.transform(disposalPropertyTransaction.prune(_)))
      .flatMap(_.transform((Paths.heldPropertyTransactions \ "isLandOrPropertyResidential").prune(_)))
      .flatMap(_.transform((Paths.heldPropertyTransactions \ "landOrPropertyLeased").prune(_)))
      .flatMap(_.transform((Paths.heldPropertyTransactions \ "leaseDetails").prune(_)))
      .flatMap(_.transform((Paths.heldPropertyTransactions \ "totalIncomeOrReceipts").prune(_))) match {
      case JsSuccess(value, _) => Success(currentUA.copy(data = SensitiveJsObject(value)))
      case _ => Try(currentUA)
    }

    portionStillHeldOptMap.fold(transformedResult) { portionStillHeldMap =>
      portionStillHeldMap.foldLeft(transformedResult)((uaResult, portionStillHeldEntry) => {
        val isFullyDisposed = portionStillHeldEntry._2.exists(!_._2)
        if (isFullyDisposed) {
          portionStillHeldEntry._1.toIntOption.flatMap(i => refineV[OneTo5000](i + 1).toOption) match {
            case None => uaResult
            case Some(index) => uaResult.flatMap(_.remove(LandPropertyInUKPage(srn, index)))
          }
        } else {
          uaResult
        }
      })
    }
  }
}
