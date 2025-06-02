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

package controllers.nonsipp.bonds

import pages.nonsipp.bonds.{CostOfBondsPage, NameOfBondsPage, WhyDoesSchemeHoldBondsPage}
import viewmodels.implicits._
import play.api.mvc._
import com.google.inject.Inject
import controllers.PSRController
import utils.IntUtils.toRefined5000
import controllers.actions._
import models._
import views.html.ContentTablePageView
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{ListMessage, Message, ParagraphMessage}
import controllers.nonsipp.bonds.BondsCheckAndUpdateController._
import viewmodels.models._

class BondsCheckAndUpdateController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentTablePageView
) extends PSRController {

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    (
      for {
        nameOfBonds <- requiredPage(NameOfBondsPage(srn, index))
        acquisitionType <- requiredPage(WhyDoesSchemeHoldBondsPage(srn, index))
        costOfBonds <- requiredPage(CostOfBondsPage(srn, index))
      } yield Ok(
        view(
          viewModel(
            srn,
            index,
            nameOfBonds,
            acquisitionType,
            costOfBonds
          )
        )
      )
    ).merge
  }

  def onSubmit(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { _ =>
    Redirect(routes.IncomeFromBondsController.onPageLoad(srn, index, NormalMode))
  }
}

object BondsCheckAndUpdateController {

  def viewModel(
    srn: Srn,
    index: Int,
    nameOfBonds: String,
    acquisitionType: SchemeHoldBond,
    costOfBonds: Money
  ): FormPageViewModel[ContentTablePageViewModel] = {

    val schemeHoldBond = acquisitionType match {
      case SchemeHoldBond.Acquisition => "bondsCheckAndUpdate.Acquisition"
      case SchemeHoldBond.Transfer => "bondsCheckAndUpdate.Transfer"
      case SchemeHoldBond.Contribution => "bondsCheckAndUpdate.Contribution"
    }

    val rows: List[(DisplayMessage, DisplayMessage)] = List(
      Message("bondsCheckAndUpdate.table.one") -> Message(nameOfBonds),
      Message("bondsCheckAndUpdate.table.two") -> Message(schemeHoldBond),
      Message("bondsCheckAndUpdate.table.three") -> Message(costOfBonds.displayAs)
    )

    FormPageViewModel(
      mode = NormalMode,
      title = "bondsCheckAndUpdate.title",
      heading = "bondsCheckAndUpdate.heading",
      description = None,
      page = ContentTablePageViewModel(
        inset = None,
        beforeTable = Some(ParagraphMessage("bondsCheckAndUpdate.paragraph")),
        afterTable = Some(
          ParagraphMessage("bondsCheckAndUpdate.bullet.paragraph") ++ ListMessage
            .Bullet(
              "bondsCheckAndUpdate.bullet.one",
              "bondsCheckAndUpdate.bullet.two",
              "bondsCheckAndUpdate.bullet.three"
            )
        ),
        rows = rows
      ),
      refresh = None,
      buttonText = "bondsCheckAndUpdate.button",
      details = None,
      onSubmit = routes.BondsCheckAndUpdateController.onSubmit(srn, index),
      optViewOnlyDetails = None
    )
  }
}
