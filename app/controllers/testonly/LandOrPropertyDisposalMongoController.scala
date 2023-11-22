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

import cats.implicits.{toBifunctorOps, toTraverseOps}
import config.Refined.{Max50, Max5000}
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined._
import models.SchemeId.Srn
import models.{RecipientDetails, UserAnswers}
import pages.nonsipp.landorpropertydisposal.{
  LandOrPropertyDisposalBuyerConnectedPartyPage,
  LandOrPropertyStillHeldPage,
  LandPropertyDisposalCompletedPage,
  OtherBuyerDetailsPage
}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.models.SectionCompleted

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class LandOrPropertyDisposalMongoController @Inject()(
  saveService: SaveService,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val max: Max50 = refineMV(50)

  def addLandOrPropertyDisposal(srn: Srn, index: Max5000, num: Max50): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        removedUserAnswers <- Future.fromTry(removeAllLandOrPropertyDisposals(srn, index, request.userAnswers))
        updatedUserAnswers <- Future.fromTry(
          updateUserAnswersWithLandOrProperties(num.value, srn, index, removedUserAnswers)
        )
        _ <- saveService.save(updatedUserAnswers)
      } yield Ok(
        s"Added ${num.value} land or property disposals to UserAnswers for land or property index ${index.value}\n${Json
          .prettyPrint(updatedUserAnswers.data.decryptedValue)}"
      )
    }

  private def buildIndexes(num: Int): Try[List[Max50]] =
    (1 to num).map(i => refineV[Max50.Refined](i).leftMap(new Exception(_)).toTry).toList.sequence

  private def removeAllLandOrPropertyDisposals(srn: Srn, index: Max5000, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(max.value)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, disposalIndex) =>
          ua.flatMap(_.remove(LandOrPropertyStillHeldPage(srn, index, disposalIndex)))
            .flatMap(_.remove(OtherBuyerDetailsPage(srn, index, disposalIndex)))
      }
    } yield updatedUserAnswers

  private val recipientDetails = RecipientDetails("test name", "test description")

  private def updateUserAnswersWithLandOrProperties(
    num: Int,
    srn: Srn,
    index: Max5000,
    userAnswers: UserAnswers
  ): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(num)
      otherBuyer = indexes.map(disposalIndex => OtherBuyerDetailsPage(srn, index, disposalIndex) -> recipientDetails)
      stillHeld = indexes.map(disposalIndex => LandOrPropertyStillHeldPage(srn, index, disposalIndex) -> true)
      connectedParty = indexes.map(
        disposalIndex => LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndex) -> true
      )
      sectionCompleted = indexes.map(
        disposalIndex => LandPropertyDisposalCompletedPage(srn, index, disposalIndex) -> SectionCompleted
      )
      ua1 <- otherBuyer.foldLeft(Try(userAnswers)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua2 <- stillHeld.foldLeft(Try(ua1)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua3 <- connectedParty.foldLeft(Try(ua2)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua4 <- sectionCompleted.foldLeft(Try(ua3)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
    } yield ua4
}
