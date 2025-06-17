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

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.FrontendAppConfig
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{IdentitySubject, IdentityType, Mode}
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}
import play.api.i18n.MessagesApi
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, toRefined5000}
import pages.nonsipp.landorproperty._
import viewmodels.DisplayMessage._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandOrPropertySellerConnectedPartyController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = LandOrPropertySellerConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      recipientName(srn, index)
        .map { recipientName =>
          Ok(
            view(
              form.fromUserAnswers(LandOrPropertySellerConnectedPartyPage(srn, index)),
              LandOrPropertySellerConnectedPartyController
                .viewModel(srn, index, recipientName, config.urls.incomeTaxAct, mode)
            )
          )
        }
        .getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
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
                      LandOrPropertySellerConnectedPartyController
                        .viewModel(srn, index, recipientName, config.urls.incomeTaxAct, mode)
                    )
                  )
                )
              }
              .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(LandOrPropertySellerConnectedPartyPage(srn, index), value))
              nextPage = navigator.nextPage(LandOrPropertySellerConnectedPartyPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
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

  def viewModel(
    srn: Srn,
    index: Max5000,
    individualName: String,
    incomeTaxAct: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      Message("landOrPropertySellerConnectedParty.title"),
      Message("landOrPropertySellerConnectedParty.heading"),
      YesNoPageViewModel(
        legend = Some(Message("landOrPropertySellerConnectedParty.label", individualName))
      ),
      onSubmit = controllers.nonsipp.landorproperty.routes.LandOrPropertySellerConnectedPartyController
        .onSubmit(srn, index, mode)
    ).withDescription(
      ParagraphMessage("landOrPropertySellerConnectedParty.paragraph1") ++
        ParagraphMessage("landOrPropertySellerConnectedParty.paragraph2") ++
        ParagraphMessage("landOrPropertySellerConnectedParty.paragraph3") ++
        ListMessage(
          ListType.Bullet,
          "landOrPropertySellerConnectedParty.bullet1",
          "landOrPropertySellerConnectedParty.bullet2"
        ) ++
        ParagraphMessage(
          "landOrPropertySellerConnectedParty.paragraph4",
          LinkMessage(
            "landOrPropertySellerConnectedParty.paragraph4.link",
            incomeTaxAct,
            Map("rel" -> "noreferrer noopener", "target" -> "_blank")
          )
        )
    )
}
