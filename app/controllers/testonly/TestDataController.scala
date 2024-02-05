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
import org.slf4j.LoggerFactory
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, AnyContent}
import services.SaveService
import shapeless.PolyDefns.Case2
import shapeless.ops.hlist._
import shapeless.{HList, Poly}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait TestDataSingleIndexController[Index] extends TestDataController {

  // override
  val max: Refined[Int, Index]
  def pages(srn: Srn, index: RefinedIndex): Pages

  // implementation
  protected type RefinedIndex = Refined[Int, Index]

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)

  def addTestData(srn: Srn, num: RefinedIndex)(
    implicit remover: UserAnswersRemover,
    setter: UserAnswersSetter,
    ev: Validate[Int, Index]
  ): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      (for {
        removedUserAnswers <- Future.fromTry(removeAllPages(srn, request.userAnswers))
        updatedUserAnswers <- Future.fromTry(updateUserAnswers(srn, num.value, removedUserAnswers))
        _ <- saveService.save(updatedUserAnswers)
      } yield Ok(
        s"Added ${num.value} entries to UserAnswers for index ${num.value}\n${Json.prettyPrint(updatedUserAnswers.data.decryptedValue)}"
      )).recover { err =>
        logger.error(s"Error when calling test endpoint: ${err.getMessage}")
        throw err
      }
    }

  private def removeAllPages(srn: Srn, userAnswers: UserAnswers)(
    implicit remover: UserAnswersRemover,
    ev: Validate[Int, Index]
  ): Try[UserAnswers] =
    for {
      indexes <- buildIndexes[Index](max.value)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, index) =>
          pages(srn, index).foldLeft(ua)(RemovePages)
      }
    } yield updatedUserAnswers

  private def updateUserAnswers(
    srn: Srn,
    num: Int,
    userAnswers: UserAnswers
  )(
    implicit setter: UserAnswersSetter,
    ev: Validate[Int, Index]
  ): Try[UserAnswers] =
    for {
      indexes <- buildIndexes[Index](num)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, index) => pages(srn, index).foldLeft(ua)(SetPages)
      }
    } yield updatedUserAnswers
}

trait TestDataDoubleIndexController[Index, SecondaryIndex] extends TestDataController {

  // override
  val max: Refined[Int, SecondaryIndex]
  def pages(srn: Srn, index: RefinedIndex, secondaryIndex: RefinedSecondaryIndex): Pages

  // implementation
  private type RefinedIndex = Refined[Int, Index]
  private type RefinedSecondaryIndex = Refined[Int, SecondaryIndex]

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
      } yield Ok(
        s"Added ${num.value} entries to UserAnswers for index ${index.value}\n${Json.prettyPrint(updatedUserAnswers.data.decryptedValue)}"
      )
    }

  private def removeAllPages(srn: Srn, index: RefinedIndex, userAnswers: UserAnswers)(
    implicit remover: UserAnswersRemover,
    ev: Validate[Int, SecondaryIndex]
  ): Try[UserAnswers] =
    for {
      indexes <- buildIndexes[SecondaryIndex](max.value)
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
      indexes <- buildIndexes[SecondaryIndex](num)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, secondaryIndex) =>
          pages(srn, index, secondaryIndex).foldLeft(ua)(SetPages)
      }
    } yield updatedUserAnswers
}

trait TestDataController extends PSRController {

  // override
  type Pages <: HList

  // public inject
  val saveService: SaveService
  val identifyAndRequireData: IdentifyAndRequireData
  implicit val ec: ExecutionContext

  protected type UserAnswersRemover = LeftFolder.Aux[Pages, Try[UserAnswers], RemovePages.type, Try[UserAnswers]]
  protected type UserAnswersSetter = LeftFolder.Aux[Pages, Try[UserAnswers], SetPages.type, Try[UserAnswers]]

  protected def buildIndexes[A](
    num: Int
  )(implicit ev: Validate[Int, A]): Try[List[Refined[Int, A]]] =
    (1 to num).map(i => refineV[A](i).leftMap(new Exception(_)).toTry).toList.sequence

  object RemovePages extends Poly {
    implicit def polyRemovePage[A: Writes]: Case2.Aux[this.type, Try[UserAnswers], PageWithValue[A], Try[UserAnswers]] =
      Case2.apply((ua, p) => ua.flatMap(_.removeOnly(p.page)))
  }

  object SetPages extends Poly {
    implicit def polySetPage[A: Writes]: Case2.Aux[this.type, Try[UserAnswers], PageWithValue[A], Try[UserAnswers]] =
      Case2.apply((ua, p) => ua.flatMap(_.setOnly(p.page, p.value)))
  }
}
