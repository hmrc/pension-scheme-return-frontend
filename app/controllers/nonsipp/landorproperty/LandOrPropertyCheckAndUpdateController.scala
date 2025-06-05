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

package controllers.nonsipp.landorproperty

import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import controllers.actions._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.ContentTablePageView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined5000}
import pages.nonsipp.landorproperty.{
  LandOrPropertyChosenAddressPage,
  LandOrPropertyTotalCostPage,
  WhyDoesSchemeHoldLandPropertyPage
}
import models._
import controllers.nonsipp.landorproperty.LandOrPropertyCheckAndUpdateController._
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{ListMessage, Message, ParagraphMessage}
import viewmodels.models._

class LandOrPropertyCheckAndUpdateController @Inject() (
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentTablePageView
) extends PSRController {

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    (
      for {
        address <- request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney
        whyLandIsHeld <- request.userAnswers.get(WhyDoesSchemeHoldLandPropertyPage(srn, index)).getOrRecoverJourney
        totalCost <- request.userAnswers.get(LandOrPropertyTotalCostPage(srn, index)).getOrRecoverJourney
      } yield Ok(
        view(
          viewModel(
            srn,
            index,
            address,
            whyLandIsHeld,
            totalCost
          )
        )
      )
    ).merge
  }

  def onSubmit(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { _ =>
    Redirect(routes.IsLandOrPropertyResidentialController.onPageLoad(srn, index, NormalMode))
  }
}

object LandOrPropertyCheckAndUpdateController {

  def viewModel(
    srn: Srn,
    index: Max5000,
    address: Address,
    whyLandIsHeld: SchemeHoldLandProperty,
    totalCost: Money
  ): FormPageViewModel[ContentTablePageViewModel] = {

    val rows: List[(DisplayMessage, DisplayMessage)] = List(
      Message("landOrPropertyCheckAndUpdate.table.one") -> Message(address.addressLine1),
      Message("landOrPropertyCheckAndUpdate.table.two") -> Message(whyLandIsHeld.name),
      Message("landOrPropertyCheckAndUpdate.table.three") -> Message(totalCost.displayAs)
    )

    FormPageViewModel(
      mode = NormalMode,
      title = "landOrPropertyCheckAndUpdate.title",
      heading = "landOrPropertyCheckAndUpdate.heading",
      description = None,
      page = ContentTablePageViewModel(
        inset = None,
        beforeTable = Some(ParagraphMessage("landOrPropertyCheckAndUpdate.paragraph")),
        afterTable = Some(
          ParagraphMessage("landOrPropertyCheckAndUpdate.bullet.paragraph") ++ ListMessage
            .Bullet(
              "landOrPropertyCheckAndUpdate.bullet.one",
              "landOrPropertyCheckAndUpdate.bullet.two",
              "landOrPropertyCheckAndUpdate.bullet.three"
            )
        ),
        rows = rows
      ),
      refresh = None,
      buttonText = "landOrPropertyCheckAndUpdate.button",
      details = None,
      onSubmit = routes.LandOrPropertyCheckAndUpdateController.onSubmit(srn, index),
      optViewOnlyDetails = None
    )
  }
}
