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

package controllers.nonsipp.sharesdisposal

import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.sharesdisposal.IsBuyerConnectedPartyController._
import forms.YesNoPageFormProvider
import models.{IdentityType, Mode}
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.sharesdisposal._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class IsBuyerConnectedPartyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = IsBuyerConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(SharesDisposalBuyerConnectedPartyPage(srn, shareIndex, disposalIndex), form)
      getBuyerName(srn, shareIndex, disposalIndex)
        .map(buyerName => Ok(view(preparedForm, viewModel(srn, shareIndex, disposalIndex, buyerName, mode))))
        .merge
    }

  private def getBuyerName(srn: Srn, shareIndex: Max5000, disposalIndex: Max50)(
    implicit request: DataRequest[_]
  ): Either[Result, String] =
    for {
      buyerType <- request.userAnswers
        .get(WhoWereTheSharesSoldToPage(srn, shareIndex, disposalIndex))
        .getOrRecoverJourney
      buyerName <- buyerType match {
        case IdentityType.Individual =>
          request.userAnswers
            .get(SharesIndividualBuyerNamePage(srn, shareIndex, disposalIndex))
            .getOrRecoverJourney
        case IdentityType.UKCompany =>
          request.userAnswers
            .get(CompanyBuyerNamePage(srn, shareIndex, disposalIndex))
            .getOrRecoverJourney
        case IdentityType.UKPartnership =>
          request.userAnswers
            .get(PartnershipBuyerNamePage(srn, shareIndex, disposalIndex))
            .getOrRecoverJourney
        case IdentityType.Other =>
          request.userAnswers
            .get(OtherBuyerDetailsPage(srn, shareIndex, disposalIndex))
            .getOrRecoverJourney
            .map(_.name)
      }
    } yield buyerName

  def onSubmit(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              getBuyerName(srn, shareIndex, disposalIndex)
                .map(
                  buyerName =>
                    BadRequest(view(formWithErrors, viewModel(srn, shareIndex, disposalIndex, buyerName, mode)))
                )
                .merge
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers.set(IsBuyerConnectedPartyPage(srn, shareIndex, disposalIndex), value)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                IsBuyerConnectedPartyPage(srn, shareIndex, disposalIndex),
                mode,
                updatedAnswers
              )
            )
        )
    }
}

object IsBuyerConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "sharesDisposal.isBuyerConnectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    buyerName: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "sharesDisposal.isBuyerConnectedParty.title",
      Message("sharesDisposal.isBuyerConnectedParty.heading", buyerName),
      controllers.nonsipp.sharesdisposal.routes.IsBuyerConnectedPartyController
        .onSubmit(srn, shareIndex, disposalIndex, mode)
    )
}
