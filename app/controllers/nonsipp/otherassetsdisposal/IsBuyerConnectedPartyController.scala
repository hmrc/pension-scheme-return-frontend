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

package controllers.nonsipp.otherassetsdisposal

import services.SaveService
import pages.nonsipp.otherassetsdisposal._
import viewmodels.implicits._
import play.api.mvc._
import utils.IntUtils.{toInt, IntOpts}
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.otherassetsdisposal.IsBuyerConnectedPartyController._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{IdentityType, Mode}
import play.api.i18n.MessagesApi
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

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

  def onPageLoad(srn: Srn, assetIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(IsBuyerConnectedPartyPage(srn, assetIndex.refined, disposalIndex.refined), form)
      getBuyerName(srn, assetIndex.refined, disposalIndex.refined)
        .map(
          buyerName =>
            Ok(view(preparedForm, viewModel(srn, assetIndex.refined, disposalIndex.refined, buyerName, mode)))
        )
        .merge
    }

  private def getBuyerName(srn: Srn, assetIndex: Max5000, disposalIndex: Max50)(
    implicit request: DataRequest[_]
  ): Either[Result, String] =
    for {
      buyerType <- request.userAnswers
        .get(TypeOfAssetBuyerPage(srn, assetIndex, disposalIndex))
        .getOrRecoverJourney
      buyerName <- buyerType match {
        case IdentityType.Individual =>
          request.userAnswers
            .get(IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex))
            .getOrRecoverJourney
        case IdentityType.UKCompany =>
          request.userAnswers
            .get(CompanyNameOfAssetBuyerPage(srn, assetIndex, disposalIndex))
            .getOrRecoverJourney
        case IdentityType.UKPartnership =>
          request.userAnswers
            .get(PartnershipBuyerNamePage(srn, assetIndex, disposalIndex))
            .getOrRecoverJourney
        case IdentityType.Other =>
          request.userAnswers
            .get(OtherBuyerDetailsPage(srn, assetIndex, disposalIndex))
            .getOrRecoverJourney
            .map(_.name)
      }
    } yield buyerName

  def onSubmit(srn: Srn, assetIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              getBuyerName(srn, assetIndex.refined, disposalIndex.refined)
                .map(
                  buyerName =>
                    BadRequest(
                      view(formWithErrors, viewModel(srn, assetIndex.refined, disposalIndex.refined, buyerName, mode))
                    )
                )
                .merge
            ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers
                  .set(IsBuyerConnectedPartyPage(srn, assetIndex.refined, disposalIndex.refined), value)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                IsBuyerConnectedPartyPage(srn, assetIndex.refined, disposalIndex.refined),
                mode,
                updatedAnswers
              )
            )
        )
    }
}

object IsBuyerConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "otherAssetsDisposal.isBuyerConnectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    buyerName: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "otherAssetsDisposal.isBuyerConnectedParty.title",
      Message("otherAssetsDisposal.isBuyerConnectedParty.heading", buyerName),
      controllers.nonsipp.otherassetsdisposal.routes.IsBuyerConnectedPartyController
        .onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
