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

package controllers.testonly

import cats.syntax.bifunctor._
import cats.syntax.traverse._
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import models.SchemeId.Srn
import models.UserAnswers
import play.api.libs.json.Writes
import play.api.mvc.{Action, AnyContent}
import services.SaveService
import shapeless.PolyDefns.Case2
import shapeless.ops.hlist._
import shapeless.{HList, Poly}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait TestDataController[Index, SecondaryIndex] extends PSRController {

  // override
  type Pages <: HList
  val max: Refined[Int, SecondaryIndex]
  def pages(srn: Srn, index: RefinedIndex, secondaryIndex: RefinedSecondaryIndex): Pages

  // public inject
  val saveService: SaveService
  val identifyAndRequireData: IdentifyAndRequireData
  implicit val ec: ExecutionContext

  // implementation
  private type RefinedIndex = Refined[Int, Index]
  private type RefinedSecondaryIndex = Refined[Int, SecondaryIndex]

  private type UserAnswersRemover = LeftFolder.Aux[Pages, Try[UserAnswers], RemovePages.type, Try[UserAnswers]]
  private type UserAnswersSetter = LeftFolder.Aux[Pages, Try[UserAnswers], SetPages.type, Try[UserAnswers]]

  def addTestData(srn: Srn, index: RefinedIndex, num: RefinedSecondaryIndex)(
    implicit remover: UserAnswersRemover,
    setter: UserAnswersSetter,
    ev: Validate[Int, SecondaryIndex]
  ): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        removedUserAnswers <- Future.fromTry(removeAllPages(srn, index, request.userAnswers))
        updatedUserAnswers <- Future.fromTry(updateUserAnswers(num.value, srn, index, removedUserAnswers))
        _ <- saveService.save(updatedUserAnswers)
      } yield Ok(s"Added ${num.value} entries to UserAnswers for index ${index.value}")
    }

  private def buildSecondaryIndexes(
    num: Int
  )(implicit ev: Validate[Int, SecondaryIndex]): Try[List[RefinedSecondaryIndex]] =
    (1 to num).map(i => refineV[SecondaryIndex](i).leftMap(new Exception(_)).toTry).toList.sequence

  private def removeAllPages(srn: Srn, index: RefinedIndex, userAnswers: UserAnswers)(
    implicit remover: UserAnswersRemover,
    ev: Validate[Int, SecondaryIndex]
  ): Try[UserAnswers] =
    for {
      indexes <- buildSecondaryIndexes(max.value)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, disposalIndex) =>
          pages(srn, index, disposalIndex).foldLeft(ua)(RemovePages)
      }
    } yield updatedUserAnswers

  private def updateUserAnswers(
    num: Int,
    srn: Srn,
    index: RefinedIndex,
    userAnswers: UserAnswers
  )(
    implicit setter: UserAnswersSetter,
    ev: Validate[Int, SecondaryIndex]
  ): Try[UserAnswers] =
    for {
      indexes <- buildSecondaryIndexes(num)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, secondaryIndex) =>
          pages(srn, index, secondaryIndex).foldLeft(ua)(SetPages)
      }
    } yield updatedUserAnswers

  object RemovePages extends Poly {
    implicit def polyRemovePage[A: Writes]: Case2.Aux[this.type, Try[UserAnswers], PageWithValue[A], Try[UserAnswers]] =
      Case2.apply((ua, p) => ua.flatMap(_.remove(p.page)))
  }

  object SetPages extends Poly {
    implicit def polySetPage[A: Writes]: Case2.Aux[this.type, Try[UserAnswers], PageWithValue[A], Try[UserAnswers]] =
      Case2.apply((ua, p) => ua.flatMap(_.set(p.page, p.value)))
  }
}
