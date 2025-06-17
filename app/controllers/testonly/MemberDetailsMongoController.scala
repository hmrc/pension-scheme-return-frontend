/*
 * Copyright 2025 HM Revenue & Customs
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

///*
// * Copyright 2024 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package controllers.testonly
//
//import pages.nonsipp.memberdetails._
//import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
//import config.RefinedTypes.{Max300, OneTo300}
//import models.SchemeId.Srn
//import utils.IntUtils.toRefined300
//import cats.implicits._
//import repositories.SessionRepository
//import models.UserAnswers
//import viewmodels.models.{MemberState, SectionCompleted}
//import controllers.actions.IdentifyAndRequireData
//import eu.timepit.refined._
//import play.api.i18n.I18nSupport
//import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
//
//import scala.concurrent.{ExecutionContext, Future}
//import scala.util.Try
//
//import javax.inject.Inject
//
//class MemberDetailsMongoController @Inject() (
//  sessionRepository: SessionRepository,
//  identifyAndRequireData: IdentifyAndRequireData,
//  val controllerComponents: MessagesControllerComponents
//)(implicit ec: ExecutionContext)
//    extends FrontendBaseController
//    with I18nSupport {
//
//  private val max: Max300 = 300
//
//  def addMemberDetails(srn: Srn, num: Int): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
//    for {
//      removedUserAnswers <- Future.fromTry(removeAllMemberDetails(srn, request.userAnswers))
//      updatedUserAnswers <- Future.fromTry(updateUserAnswersWithMemberDetails(num.value, srn, removedUserAnswers))
//      _ <- sessionRepository.set(updatedUserAnswers)
//    } yield Ok(s"Added ${num.value} member details to UserAnswers")
//  }
//
//  private def buildIndexes(num: Int): Try[List[Max300]] =
//    (1 to num).map(i => refineV[OneTo300](i).leftMap(new Exception(_)).toTry).toList.sequence
//
//  private def removeAllMemberDetails(srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
//    for {
//      indexes <- buildIndexes(max.value)
//      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) { case (ua, index) =>
//        ua.flatMap(_.remove(MemberDetailsPage(srn, index)))
//          .flatMap(_.remove(DoesMemberHaveNinoPage(srn, index)))
//          .flatMap(_.remove(MemberDetailsNinoPage(srn, index)))
//          .flatMap(_.remove(NoNINOPage(srn, index)))
//          .flatMap(_.remove(MemberStatus(srn, index)))
//          .flatMap(_.remove(MemberDetailsCompletedPage(srn, index)))
//      }
//    } yield updatedUserAnswers
//
//  private def updateUserAnswersWithMemberDetails(num: Int, srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
//    for {
//      indexes <- buildIndexes(num)
//      memberDetails = indexes.map { index =>
//        MemberDetailsPage(srn, index) -> buildRandomNameDOB()
//      }
//      hasNinoPages = indexes.map(index => DoesMemberHaveNinoPage(srn, index) -> false)
//      noNinoReasonPages = indexes.map(index => NoNINOPage(srn, index) -> "test reason")
//      memberStates = indexes.map(index => MemberStatus(srn, index))
//      completed = indexes.map(index => MemberDetailsCompletedPage(srn, index))
//
//      ua1 <- memberDetails.foldLeft(Try(userAnswers)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
//      ua2 <- hasNinoPages.foldLeft(Try(ua1)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
//      ua3 <- noNinoReasonPages.foldLeft(Try(ua2)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
//      ua4 <- memberStates.foldLeft(Try(ua3)) { case (ua, page) => ua.flatMap(_.set(page, MemberState.New)) }
//      ua5 <- completed.foldLeft(Try(ua4)) { case (ua, page) => ua.flatMap(_.set(page, SectionCompleted)) }
//    } yield ua5
//}
