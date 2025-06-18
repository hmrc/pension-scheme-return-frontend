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

package transformations

import config.RefinedTypes._
import models.SchemeId.Srn
import cats.implicits.{toBifunctorOps, toTraverseOps}
import config.Constants.defaultFbVersion
import eu.timepit.refined.refineV
import models.UserAnswers
import eu.timepit.refined.api.{Refined, Validate}
import pages.nonsipp.FbVersionPage
import play.api.Logger

import scala.util.Try

trait Transformer {

  protected def keysToIndex[A](map: Map[String, ?])(using Validate[Int, A]): List[Refined[Int, A]] =
    map.keys.toList.flatMap(refineIndex[A])

  protected def refineIndex[A](index: String)(using Validate[Int, A]): Option[Refined[Int, A]] =
    index.toIntOption.flatMap(i => refineV[A](i + 1).toOption)

  protected def refineIndex[A](index: Int)(using Validate[Int, A]): Option[Refined[Int, A]] =
    refineV[A](index + 1).toOption

  protected def buildIndexesForMax5000(num: Int): Try[List[Max5000]] =
    (1 to num).map(i => refineV[OneTo5000](i).leftMap(new Exception(_)).toTry).toList.sequence

  protected def buildIndexesForMax50(num: Int): Try[List[Max50]] =
    (1 to num).map(i => refineV[OneTo50](i).leftMap(new Exception(_)).toTry).toList.sequence

}

object Transformer {
  def shouldDefaultToZeroIfMissing(
    userAnswers: UserAnswers,
    srn: Srn,
    index: Max5000,
    transactionPrepopulated: Option[Boolean],
    nameToLog: String
  )(implicit logger: Logger): Boolean = {
    val isFbVersionGreaterThan1 = userAnswers.get(FbVersionPage(srn)).getOrElse(defaultFbVersion).toInt > 1
    (transactionPrepopulated, isFbVersionGreaterThan1) match {
      case (Some(false), false) =>
        logger.info(
          s"index: $index, name: $nameToLog - entity with prePopulated field," +
            s" fbVersion less than 1, not yet checked - should NOT default to zero if missing"
        )
        false
      case (Some(true), false) =>
        // return pre-populated in the past, fbVersion less than 1, already checked, should default to zero
        logger.info(
          s"index: $index, name: $nameToLog - entity with prePopulated field," +
            s" fbVersion less than 1, already checked - should default to zero if missing"
        )
        true
      case (Some(_), true) =>
        logger.info(
          s"index: $index, name: $nameToLog - return with fbVersion greater than 1 - should default to zero if missing"
        )
        true
      case (None, _) =>
        logger.info(
          s"index: $index, name: $nameToLog - record without prePopulated field - should default to zero if missing"
        )
        true
    }
  }
}
