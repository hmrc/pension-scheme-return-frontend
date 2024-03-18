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

package controllers.nonsipp.landorpropertydisposal

import services.SaveService
import viewmodels.implicits._
import play.api.mvc._
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalBuyerConnectedPartyController._
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import pages.nonsipp.landorpropertydisposal._
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{IdentityType, Mode}
import play.api.i18n.MessagesApi
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandOrPropertyDisposalBuyerConnectedPartyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandOrPropertyDisposalBuyerConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndex), form)
      getBuyersName(srn, index, disposalIndex)
        .map(buyersName => Ok(view(preparedForm, viewModel(srn, buyersName, index, disposalIndex, mode))))
        .merge
    }

  def onSubmit(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              getBuyersName(srn, index, disposalIndex)
                .map(
                  buyersName => BadRequest(view(formWithErrors, viewModel(srn, buyersName, index, disposalIndex, mode)))
                )
                .merge
            ),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(
                  request.userAnswers
                    .set(LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndex), value)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                LandOrPropertyDisposalBuyerConnectedPartyPage(srn, index, disposalIndex),
                mode,
                updatedAnswers
              )
            )
        )
    }

  private def getBuyersName(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50)(
    implicit request: DataRequest[_]
  ): Either[Result, String] =
    for {
      buyerType <- request.userAnswers
        .get(WhoPurchasedLandOrPropertyPage(srn, landOrPropertyIndex, disposalIndex))
        .getOrRecoverJourney
      buyerName <- buyerType match {
        case IdentityType.Individual =>
          request.userAnswers
            .get(LandOrPropertyIndividualBuyerNamePage(srn, landOrPropertyIndex, disposalIndex))
            .getOrRecoverJourney
        case IdentityType.UKCompany =>
          request.userAnswers.get(CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)).getOrRecoverJourney
        case IdentityType.UKPartnership =>
          request.userAnswers.get(PartnershipBuyerNamePage(srn, landOrPropertyIndex, disposalIndex)).getOrRecoverJourney
        case IdentityType.Other =>
          request.userAnswers
            .get(OtherBuyerDetailsPage(srn, landOrPropertyIndex, disposalIndex))
            .getOrRecoverJourney
            .map(_.name)
      }
    } yield buyerName
}

object LandOrPropertyDisposalBuyerConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "landOrPropertyDisposalBuyerConnectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    buyersName: String,
    index: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] = YesNoPageViewModel(
    "landOrPropertyDisposalBuyerConnectedParty.title",
    Message("landOrPropertyDisposalBuyerConnectedParty.heading", buyersName),
    controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalBuyerConnectedPartyController
      .onSubmit(srn, index, disposalIndex, mode)
  )
}
