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
import config.Refined.{Max5000, OneTo5000}
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined._
import models.SchemeId.Srn
import models.{Address, UserAnswers}
import pages.nonsipp.landorproperty.{LandOrPropertyAddressLookupPage, LandPropertyInUKPage}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class LandOrPropertyMongoController @Inject()(
  saveService: SaveService,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val max: Max5000 = refineMV(5000)

  private val address = Address(
    addressLine1 = "1 test street",
    addressLine2 = "test line 2",
    addressLine3 = None,
    town = Some("test town"),
    postCode = Some("ZZ1 1ZZ"),
    country = "United Kingdom"
  )

  def addLandOrProperty(srn: Srn, num: Max5000): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      for {
        removedUserAnswers <- Future.fromTry(removeAllLandOrProperties(srn, request.userAnswers))
        updatedUserAnswers <- Future.fromTry(updateUserAnswersWithLandOrProperties(num.value, srn, removedUserAnswers))
        _ <- saveService.save(updatedUserAnswers)
      } yield Ok(s"Added ${num.value} loan details to UserAnswers")
  }

  private def buildIndexes(num: Int): Try[List[Max5000]] =
    (1 to num).map(i => refineV[OneTo5000](i).leftMap(new Exception(_)).toTry).toList.sequence

  private def removeAllLandOrProperties(srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(max.value)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, index) =>
          ua.flatMap(_.remove(LandPropertyInUKPage(srn, index)))
            .flatMap(_.remove(LandOrPropertyAddressLookupPage(srn, index)))
      }
    } yield updatedUserAnswers

  private def updateUserAnswersWithLandOrProperties(num: Int, srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(num)
      landOrPropertyInUK = indexes.map(index => LandPropertyInUKPage(srn, index) -> true)
      addressLookup = indexes.map(index => LandOrPropertyAddressLookupPage(srn, index) -> address)
      ua1 <- landOrPropertyInUK.foldLeft(Try(userAnswers)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua2 <- addressLookup.foldLeft(Try(ua1)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
    } yield ua2

}
