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

import cats.implicits.{toBifunctorOps, toTraverseOps}
import config.Refined.{Max300, Max50, Max5000, OneTo50, OneTo5000}
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import models.{NameDOB, UserAnswers}
import models.SchemeId.Srn
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps

import scala.util.Try

trait Transformer {

  protected def refinedMembers(srn: Srn, userAnswers: UserAnswers): List[(Max300, NameDOB)] =
    userAnswers
      .membersDetails(srn)
      .zipWithIndex
      .flatMap {
        case (details, index) =>
          refineIndex[Max300.Refined](index).map(_ -> details).toList
      }

  protected def keysToIndex[A: Validate[Int, *]](map: Map[String, _]): List[Refined[Int, A]] =
    map.keys.toList.flatMap(refineIndex[A])

  protected def refineIndex[A: Validate[Int, *]](index: String): Option[Refined[Int, A]] =
    index.toIntOption.flatMap(i => refineV[A](i + 1).leftMap(new Exception(_)).toOption)

  protected def refineIndex[A: Validate[Int, *]](index: Int): Option[Refined[Int, A]] =
    refineV[A](index + 1).leftMap(new Exception(_)).toOption

  protected def buildIndexesForMax5000(num: Int): Try[List[Max5000]] =
    (1 to num).map(i => refineV[OneTo5000](i).leftMap(new Exception(_)).toTry).toList.sequence

  protected def buildIndexesForMax50(num: Int): Try[List[Max50]] =
    (1 to num).map(i => refineV[OneTo50](i).leftMap(new Exception(_)).toTry).toList.sequence

}
