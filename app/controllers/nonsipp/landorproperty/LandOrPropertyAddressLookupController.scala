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

package controllers.nonsipp.landorproperty

import cats.implicits._
import config.Refined.Max5000
import connectors.AddressLookupConnector
import controllers.PSRController
import controllers.actions._
import models.SchemeId.Srn
import models.{ALFAddress, Address, ManualAddress, Mode}
import navigation.Navigator
import pages.nonsipp.landorproperty.{LandOrPropertyChosenAddressPage, LandPropertyInUKPage}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class LandOrPropertyAddressLookupController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  addressLookupConnector: AddressLookupConnector,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val continueUrl = routes.LandOrPropertyAddressLookupController.onSubmit(srn, index).absoluteURL()
      val result = for {
        landOrPropertyInUK <- request.userAnswers.get(LandPropertyInUKPage(srn, index)).getOrRecoverJourneyT
        redirectUrl <- addressLookupConnector.init(continueUrl, landOrPropertyInUK).liftF
      } yield Redirect(redirectUrl)

      result.merge
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      val result = for {
        addressLookupId <- request.queryString.get("id").flatMap(_.headOption).getOrRecoverJourneyT
        maybeALFAddress <- addressLookupConnector.fetchAddress(addressLookupId).map(_.toOption).liftF
        alfAddress <- maybeALFAddress.getOrRecoverJourneyT
        address = addressFromALFAddress(alfAddress)
        updatedAnswers <- request.userAnswers.set(LandOrPropertyChosenAddressPage(srn, index), address).toFuture.liftF
        _ <- saveService.save(updatedAnswers).liftF
      } yield Redirect(navigator.nextPage(LandOrPropertyChosenAddressPage(srn, index), mode, updatedAnswers))

      result.merge
  }

  private def addressFromALFAddress(alfAddress: ALFAddress): Address =
    Address(
      "",
      alfAddress.firstLine,
      alfAddress.secondLine,
      alfAddress.thirdLine,
      alfAddress.town,
      Some(alfAddress.postcode),
      alfAddress.country.name,
      alfAddress.country.code,
      ManualAddress
    )
}
