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

import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{IdentitySubject, IdentityType, Mode}
import navigation.Navigator
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}
import pages.nonsipp.landorproperty.{
  CompanySellerNamePage,
  LandOrPropertySellerConnectedPartyPage,
  LandPropertyIndividualSellersNamePage,
  PartnershipSellerNamePage
}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class LandOrPropertySellerConnectedPartyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandOrPropertySellerConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      recipientName(srn, index)
        .map { recipientName =>
          Ok(
            view(
              form.fromUserAnswers(LandOrPropertySellerConnectedPartyPage(srn, index)),
              LandOrPropertySellerConnectedPartyController.viewModel(srn, index, recipientName, mode)
            )
          )
        }
        .getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            recipientName(srn, index)
              .map { recipientName =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      LandOrPropertySellerConnectedPartyController.viewModel(srn, index, recipientName, mode)
                    )
                  )
                )
              }
              .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(LandOrPropertySellerConnectedPartyPage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(LandOrPropertySellerConnectedPartyPage(srn, index), mode, updatedAnswers)
            )
        )
  }

  private def recipientName(srn: Srn, index: Max5000)(implicit request: DataRequest[_]): Option[String] =
    request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller)).flatMap {
      case IdentityType.Individual => request.userAnswers.get(LandPropertyIndividualSellersNamePage(srn, index))
      case IdentityType.UKCompany => request.userAnswers.get(CompanySellerNamePage(srn, index))
      case IdentityType.UKPartnership => request.userAnswers.get(PartnershipSellerNamePage(srn, index))
      case IdentityType.Other =>
        request.userAnswers.get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LandOrPropertySeller)).map(_.name)
      case _ => None
    }
}

object LandOrPropertySellerConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "landOrPropertySellerConnectedParty.error.required"
  )

  def viewModel(srn: Srn, index: Max5000, individualName: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "landOrPropertySellerConnectedParty.title",
      Message("landOrPropertySellerConnectedParty.heading", individualName),
      controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController.onSubmit(srn, index, mode)
    )
}
