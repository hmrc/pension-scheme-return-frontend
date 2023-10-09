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
import models.{Address, ConditionalYesNo, Money, SchemeHoldLandProperty, UserAnswers}
import pages.nonsipp.landorproperty.{
  IsLandOrPropertyResidentialPage,
  IsLandPropertyLeasedPage,
  LandOrPropertyAddressLookupPage,
  LandOrPropertyHeldPage,
  LandOrPropertyTotalCostPage,
  LandOrPropertyTotalIncomePage,
  LandPropertyInUKPage,
  LandRegistryTitleNumberPage,
  WhyDoesSchemeHoldLandPropertyPage
}
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
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

  private def address(index: Int) = Address(
    addressLine1 = s"${index.toString} test street",
    addressLine2 = "test line 2",
    addressLine3 = None,
    town = Some("test town"),
    postCode = Some("ZZ1 1ZZ"),
    country = "United Kingdom",
    countryCode = "GB"
  )

  def addLandOrProperty(srn: Srn, num: Max5000): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      for {
        removedUserAnswers <- Future.fromTry(removeAllLandOrProperties(srn, request.userAnswers))
        updatedUserAnswers <- Future.fromTry(updateUserAnswersWithLandOrProperties(num.value, srn, removedUserAnswers))
        _ <- saveService.save(updatedUserAnswers)
      } yield Ok(
        s"Added ${num.value} land or property to UserAnswers \n${Json.prettyPrint(updatedUserAnswers.data.decryptedValue)}"
      )
  }

  private def buildIndexes(num: Int): Try[List[Max5000]] =
    (1 to num).map(i => refineV[OneTo5000](i).leftMap(new Exception(_)).toTry).toList.sequence

  private def removeAllLandOrProperties(srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(max.value)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, index) =>
          ua.flatMap(_.remove(LandOrPropertyHeldPage(srn)))
            .flatMap(_.remove(LandPropertyInUKPage(srn, index)))
            .flatMap(_.remove(LandOrPropertyAddressLookupPage(srn, index)))
            .flatMap(_.remove(LandRegistryTitleNumberPage(srn, index)))
            .flatMap(_.remove(WhyDoesSchemeHoldLandPropertyPage(srn, index)))
            .flatMap(_.remove(LandOrPropertyTotalCostPage(srn, index)))
            .flatMap(_.remove(IsLandOrPropertyResidentialPage(srn, index)))
            .flatMap(_.remove(IsLandPropertyLeasedPage(srn, index)))
            .flatMap(_.remove(LandOrPropertyTotalIncomePage(srn, index)))
      }
    } yield updatedUserAnswers

  private def updateUserAnswersWithLandOrProperties(num: Int, srn: Srn, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(num)
      ua0 <- userAnswers.set(LandOrPropertyHeldPage(srn), true)
      landOrPropertyInUK = indexes.map(index => LandPropertyInUKPage(srn, index) -> true)
      addressLookup = indexes.map(index => LandOrPropertyAddressLookupPage(srn, index) -> address(index.value))
      titleNumber = indexes.map(
        index => LandRegistryTitleNumberPage(srn, index) -> ConditionalYesNo.no[String, String]("reason")
      )
      whyHeld = indexes.map(index => WhyDoesSchemeHoldLandPropertyPage(srn, index) -> SchemeHoldLandProperty.Transfer)
      cost = indexes.map(index => LandOrPropertyTotalCostPage(srn, index) -> Money(123.45))
      residential = indexes.map(index => IsLandOrPropertyResidentialPage(srn, index) -> false)
      leased = indexes.map(index => IsLandPropertyLeasedPage(srn, index) -> false)
      income = indexes.map(index => LandOrPropertyTotalIncomePage(srn, index) -> Money(45.67))

      ua1 <- landOrPropertyInUK.foldLeft(Try(ua0)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua2 <- addressLookup.foldLeft(Try(ua1)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua3 <- titleNumber.foldLeft(Try(ua2)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua4 <- whyHeld.foldLeft(Try(ua3)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua5 <- cost.foldLeft(Try(ua4)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua6 <- residential.foldLeft(Try(ua5)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua7 <- leased.foldLeft(Try(ua6)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua8 <- income.foldLeft(Try(ua7)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
    } yield ua8

}
