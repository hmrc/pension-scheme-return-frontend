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

import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.landorpropertydisposal.LandOrPropertyDisposalSellerConnectedPartyController._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{IdentityType, Mode}
import navigation.Navigator
import pages.nonsipp.landorpropertydisposal.{
  CompanyBuyerNamePage,
  LandOrPropertyDisposalSellerConnectedPartyPage,
  LandOrPropertyIndividualBuyerNamePage,
  PartnershipBuyerNamePage,
  WhoPurchasedLandOrPropertyPage
}
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

class LandOrPropertyDisposalSellerConnectedPartyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandOrPropertyDisposalSellerConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(LandOrPropertyDisposalSellerConnectedPartyPage(srn, index, disposalIndex), form)
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
                    .set(LandOrPropertyDisposalSellerConnectedPartyPage(srn, index, disposalIndex), value)
                )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                LandOrPropertyDisposalSellerConnectedPartyPage(srn, index, disposalIndex),
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
          Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      }
    } yield buyerName
}

object LandOrPropertyDisposalSellerConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "landOrPropertyDisposalSellerConnectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    buyersName: String,
    index: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] = YesNoPageViewModel(
    "landOrPropertyDisposalSellerConnectedParty.title",
    Message("landOrPropertyDisposalSellerConnectedParty.heading", buyersName),
    controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalSellerConnectedPartyController
      .onSubmit(srn, index, disposalIndex, mode)
  )
}
