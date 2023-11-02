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

import cats.implicits._
import config.Refined.{Max300, OneTo300}
import controllers.actions.IdentifyAndRequireData
import controllers.testonly.MemberDetailsMongoController.buildRandomNameDOB
import eu.timepit.refined._
import models.SchemeId.Srn
import models.{NameDOB, UserAnswers}
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsNinoPage, MemberDetailsPage, NoNINOPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Random, Try}

class MemberDetailsMongoController @Inject()(
  sessionRepository: SessionRepository,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val max: Max300 = refineMV(99)

  def addMemberDetails(srn: Srn, num: Max300): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      for {
        removedUserAnswers <- Future.fromTry(removeAllMemberDetails(srn, request.userAnswers))
        updatedUserAnswers <- Future.fromTry(updateUserAnswersWithMemberDetails(num.value, srn, removedUserAnswers))
        _ <- sessionRepository.set(updatedUserAnswers)
      } yield Ok(s"Added ${num.value} member details to UserAnswers")
  }

  private def buildIndexes(num: Int): Try[List[Max300]] =
    (1 to num).map(i => refineV[OneTo300](i).leftMap(new Exception(_)).toTry).toList.sequence

  private def removeAllMemberDetails(srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(max.value)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, index) =>
          ua.flatMap(_.remove(MemberDetailsPage(srn, index)))
            .flatMap(_.remove(DoesMemberHaveNinoPage(srn, index)))
      }
    } yield updatedUserAnswers

  private def updateUserAnswersWithMemberDetails(num: Int, srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(num)
      memberDetails = indexes.map { index =>
        MemberDetailsPage(srn, index) -> buildRandomNameDOB()
      }
      hasNinoPages = indexes.map(index => DoesMemberHaveNinoPage(srn, index) -> false)
      noNinoReasonPages = indexes.map(index => NoNINOPage(srn, index) -> "test reason")

      ua1 <- memberDetails.foldLeft(Try(userAnswers)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
//      ua2 <- hasNinoPages.foldLeft(Try(ua1)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
//      ua3 <- noNinoReasonPages.foldLeft(Try(ua2)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
    } yield ua1
}

object MemberDetailsMongoController {

  private val firstNames = List(
    "Nathalia",
    "Kyro",
    "Jesse",
    "Mia",
    "Bradley",
    "Bonnie",
    "Wesley",
    "Alistair",
    "Fiona",
    "Victor"
  )

  private val lastNames = List(
    "Vazquez",
    "McMahon",
    "Davis",
    "Howell",
    "Willis",
    "Benjamin",
    "Mathews",
    "Sawyer",
    "Payne",
    "Gonzales"
  )

  def buildRandomNameDOB(): NameDOB = NameDOB(
    firstNames(Random.nextInt(firstNames.size)),
    lastNames(Random.nextInt(lastNames.size)),
    LocalDate.of(1990, 12, 12)
  )
}
