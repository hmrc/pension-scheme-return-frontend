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

package utils.nonsipp.check

import config.RefinedTypes.{Max5000, OneTo5000}
import models.SchemeId.Srn
import pages.nonsipp.landorproperty._
import eu.timepit.refined.refineV
import models._

object LandOrPropertyCheckStatusUtils {

  /**
   * This method determines whether or not the Land or Property section needs to be checked. A section needs to be
   * checked if 1 or more records in that section need to be checked.
   * @param userAnswers the answers provided by the user, from which we get each Land or Property record
   * @param srn the Scheme Reference Number, used for the .get calls
   * @return true if any record requires checking, else false
   */
  def checkLandOrPropertySection(
    userAnswers: UserAnswers,
    srn: Srn
  ): Boolean = {
    val landOrPropertyHeld = userAnswers.get(LandOrPropertyHeldPage(srn))
    val journeysStartedList = userAnswers.get(LandPropertyInUKPages(srn)).getOrElse(Map.empty).keys.toList

    landOrPropertyHeld match {
      case Some(false) => false
      case _ =>
        journeysStartedList.exists(index => {
          refineV[OneTo5000](index.toInt + 1).fold(
            _ => false,
            refinedIndex => checkLandOrPropertyRecord(userAnswers, srn, refinedIndex)
          )
        })
    }
  }

  /**
   * This method determines whether or not a Land or Property record needs to be checked. A record only needs to be checked if its
   * BondsPrePopulated field is false.
   * @param userAnswers the answers provided by the user, from which we get the Land or Property record
   * @param srn the Scheme Reference Number, used for the .get calls
   * @param recordIndex the index of the record being checked
   * @return true if the record requires checking, else false
   */
  def checkLandOrPropertyRecord(
    userAnswers: UserAnswers,
    srn: Srn,
    recordIndex: Max5000
  ): Boolean =
    userAnswers.get(LandOrPropertyPrePopulated(srn, recordIndex)) match {
      case Some(checked) => !checked
      case None => false // non-pre-pop
    }
}
