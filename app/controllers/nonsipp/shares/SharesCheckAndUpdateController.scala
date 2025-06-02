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

package controllers.nonsipp.shares

import controllers.nonsipp.shares.SharesCheckAndUpdateController._
import viewmodels.implicits._
import com.google.inject.Inject
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import models._
import pages.nonsipp.shares.{ClassOfSharesPage, CostOfSharesPage, TypeOfSharesHeldPage}
import play.api.mvc._
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.ContentTablePageView
import models.TypeOfShares.{ConnectedParty, SponsoringEmployer, Unquoted}
import models.SchemeId.Srn
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{ListMessage, Message, ParagraphMessage}
import viewmodels.models._

class SharesCheckAndUpdateController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: ContentTablePageView
) extends PSRController {

  def onPageLoad(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    (
      for {
        typeOfShares <- request.userAnswers.get(TypeOfSharesHeldPage(srn, index)).getOrRecoverJourney
        classOfShares <- request.userAnswers.get(ClassOfSharesPage(srn, index)).getOrRecoverJourney
        costOfShares <- request.userAnswers.get(CostOfSharesPage(srn, index)).getOrRecoverJourney
      } yield Ok(
        view(
          viewModel(
            srn,
            index,
            typeOfShares,
            classOfShares,
            costOfShares
          )
        )
      )
    ).merge
  }

  def onSubmit(srn: Srn, index: Int): Action[AnyContent] = identifyAndRequireData(srn) { _ =>
    Redirect(routes.SharesTotalIncomeController.onPageLoad(srn, index, NormalMode))
  }
}

object SharesCheckAndUpdateController {

  def viewModel(
    srn: Srn,
    index: Max5000,
    typeOfShares: TypeOfShares,
    classOfShares: String,
    costOfShares: Money
  ): FormPageViewModel[ContentTablePageViewModel] = {

    val typeOfSharesName = typeOfShares match {
      case SponsoringEmployer => "sharesCheckAndUpdate.SponsoringEmployer"
      case Unquoted => "sharesCheckAndUpdate.Unquoted"
      case ConnectedParty => "sharesCheckAndUpdate.ConnectedParty"
    }

    val rows: List[(DisplayMessage, DisplayMessage)] = List(
      Message("sharesCheckAndUpdate.table.one") -> Message(typeOfSharesName),
      Message("sharesCheckAndUpdate.table.two") -> Message(classOfShares),
      Message("sharesCheckAndUpdate.table.three") -> Message(costOfShares.displayAs)
    )

    FormPageViewModel(
      mode = NormalMode,
      title = "sharesCheckAndUpdate.title",
      heading = "sharesCheckAndUpdate.heading",
      description = None,
      page = ContentTablePageViewModel(
        inset = None,
        beforeTable = Some(ParagraphMessage("sharesCheckAndUpdate.paragraph")),
        afterTable = Some(
          ParagraphMessage("sharesCheckAndUpdate.bullet.paragraph") ++ ListMessage
            .Bullet(
              "sharesCheckAndUpdate.bullet.one",
              "sharesCheckAndUpdate.bullet.two",
              "sharesCheckAndUpdate.bullet.three"
            )
        ),
        rows = rows
      ),
      refresh = None,
      buttonText = "sharesCheckAndUpdate.button",
      details = None,
      onSubmit = routes.SharesCheckAndUpdateController.onSubmit(srn, index),
      optViewOnlyDetails = None
    )
  }
}
