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
import controllers.nonsipp.landorproperty.RemovePropertyController
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{IdentitySubject, IdentityType, Mode}
import navigation.Navigator
import pages.nonsipp.common.{IdentityTypePage, OtherRecipientDetailsPage}

import pages.nonsipp.landorproperty.{
  CompanySellerNamePage,
  IndividualSellerNiPage,
  LandOrPropertyAddressLookupPage,
  PartnershipSellerNamePage,
  RemovePropertyPage
}

import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemovePropertyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = RemovePropertyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney { address =>
        val preparedForm = request.userAnswers.fillForm(RemovePropertyPage(srn, index), form)
        Ok(view(preparedForm, RemovePropertyController.viewModel(srn, index, mode, address.addressLine1)))
      }
  }

  private def getResult(srn: Srn, index: Max5000, mode: Mode, form: Form[Boolean], error: Boolean = false)(
    implicit request: DataRequest[_]
  ) = {
    val whoReceivedLandPage = request.userAnswers
      .get(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller))
    whoReceivedLandPage match {
      case Some(who) => {
        val sellerNinOrName =
          who match {
            case IdentityType.Individual =>
              request.userAnswers.get(IndividualSellerNiPage(srn, index)).getOrRecoverJourney
            case IdentityType.UKCompany =>
              request.userAnswers.get(CompanySellerNamePage(srn, index)).getOrRecoverJourney
            case IdentityType.UKPartnership =>
              request.userAnswers.get(PartnershipSellerNamePage(srn, index)).getOrRecoverJourney
            case IdentityType.Other =>
              request.userAnswers
                .get(OtherRecipientDetailsPage(srn, index, IdentitySubject.LandOrPropertySeller))
                .map(_.name)
                .getOrRecoverJourney
          }
        sellerNinOrName.fold(
          l => l,
          address => {
            val propertyAddress =
              request.userAnswers.get(LandOrPropertyAddressLookupPage(srn, index)).getOrRecoverJourney

            propertyAddress.fold(
              l => l,
              address =>
                if (error) {
                  BadRequest(view(form, RemovePropertyController.viewModel(srn, index, mode, address.addressLine1)))
                } else {
                  Ok(view(form, RemovePropertyController.viewModel(srn, index, mode, address.addressLine1)))
                }
            )
          }
        )
      }
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(getResult(srn, index, mode, formWithErrors, true)),
          value =>
            if (value) {
              for {
                updatedAnswers <- Future
                  .fromTry(
                    request.userAnswers.remove(IdentityTypePage(srn, index, IdentitySubject.LandOrPropertySeller))
                  )
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(RemovePropertyPage(srn, index), mode, updatedAnswers))
            } else {
              Future
                .successful(Redirect(navigator.nextPage(RemovePropertyPage(srn, index), mode, request.userAnswers)))
            }
        )
  }
}

object RemovePropertyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeLandOrProperty.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    addressLine1: String
  ): FormPageViewModel[YesNoPageViewModel] =
    (
      YesNoPageViewModel(
        "removeLandOrProperty.title",
        Message("removeLandOrProperty.heading", addressLine1),
        routes.RemovePropertyController.onSubmit(srn, index, mode)
      )
    )
}
