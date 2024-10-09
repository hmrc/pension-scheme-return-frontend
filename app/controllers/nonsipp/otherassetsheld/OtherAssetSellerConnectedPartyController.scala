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

package controllers.nonsipp.otherassetsheld

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld._
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
import viewmodels.DisplayMessage._
import viewmodels.models.{FormPageViewModel, FurtherDetailsViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class OtherAssetSellerConnectedPartyController @Inject()(
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

  private val form = OtherAssetSellerConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      recipientName(srn, index)
        .map { recipientName =>
          Ok(
            view(
              form.fromUserAnswers(OtherAssetSellerConnectedPartyPage(srn, index)),
              OtherAssetSellerConnectedPartyController
                .viewModel(srn, index, recipientName, config.urls.incomeTaxAct, mode)
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
                      OtherAssetSellerConnectedPartyController
                        .viewModel(srn, index, recipientName, config.urls.incomeTaxAct, mode)
                    )
                  )
                )
              }
              .getOrElse(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(OtherAssetSellerConnectedPartyPage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(OtherAssetSellerConnectedPartyPage(srn, index), mode, updatedAnswers)
            )
        )
  }

  private def recipientName(srn: Srn, index: Max5000)(implicit request: DataRequest[_]): Option[String] =
    request.userAnswers.get(IdentityTypePage(srn, index, IdentitySubject.OtherAssetSeller)).flatMap {
      case IdentityType.Individual => request.userAnswers.get(IndividualNameOfOtherAssetSellerPage(srn, index))
      case IdentityType.UKCompany => request.userAnswers.get(CompanyNameOfOtherAssetSellerPage(srn, index))
      case IdentityType.UKPartnership => request.userAnswers.get(PartnershipOtherAssetSellerNamePage(srn, index))
      case IdentityType.Other =>
        request.userAnswers.get(OtherRecipientDetailsPage(srn, index, IdentitySubject.OtherAssetSeller)).map(_.name)
      case _ => None
    }
}

object OtherAssetSellerConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "otherAssets.sellerConnectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    individualName: String,
    incomeTaxAct: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("otherAssets.sellerConnectedParty.title"),
      Message("otherAssets.sellerConnectedParty.heading", individualName),
      Option(
        FurtherDetailsViewModel(
          Message("otherAssets.sellerConnectedParty.content"),
          ParagraphMessage("otherAssets.sellerConnectedParty.paragraph1") ++
            ParagraphMessage("otherAssets.sellerConnectedParty.paragraph2") ++
            ParagraphMessage("otherAssets.sellerConnectedParty.paragraph3") ++
            ListMessage(
              ListType.Bullet,
              "otherAssets.sellerConnectedParty.bullet1",
              "otherAssets.sellerConnectedParty.bullet2"
            ) ++
            ParagraphMessage(
              "otherAssets.sellerConnectedParty.paragraph4",
              LinkMessage(
                "otherAssets.sellerConnectedParty.paragraph4.link",
                incomeTaxAct,
                Map("rel" -> "noreferrer noopener", "target" -> "_blank")
              )
            )
        )
      ),
      controllers.nonsipp.otherassetsheld.routes.OtherAssetSellerConnectedPartyController.onSubmit(srn, index, mode)
    )
}
