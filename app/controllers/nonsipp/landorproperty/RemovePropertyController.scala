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

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.landorproperty._
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemovePropertyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = RemovePropertyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      if (request.userAnswers.get(LandOrPropertyPrePopulated(srn, index)).isDefined)
        Redirect(controllers.routes.UnauthorisedController.onPageLoad())
      else
        (
          for {
            address <- request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRedirectToTaskList(srn)
          } yield {
            val preparedForm = request.userAnswers.fillForm(RemovePropertyPage(srn, index), form)
            Ok(view(preparedForm, RemovePropertyController.viewModel(srn, index, mode, address.addressLine1)))
          }
        ).merge
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, index)).getOrRecoverJourney { address =>
              Future.successful(
                BadRequest(
                  view(errors, RemovePropertyController.viewModel(srn, index, mode, address.addressLine1))
                )
              )
            },
          value =>
            if (value) {
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.remove(LandPropertyInUKPage(srn, index)))
                _ <- saveService.save(updatedAnswers)
                redirectTo <- psrSubmissionService
                  .submitPsrDetailsWithUA(
                    srn,
                    updatedAnswers,
                    fallbackCall =
                      controllers.nonsipp.landorproperty.routes.LandOrPropertyListController.onPageLoad(srn, 1, mode)
                  )(implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                  .map {
                    case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    case Some(_) => Redirect(navigator.nextPage(RemovePropertyPage(srn, index), mode, updatedAnswers))
                  }
              } yield redirectTo
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
    YesNoPageViewModel(
      "removeLandOrProperty.title",
      Message("removeLandOrProperty.heading", addressLine1),
      routes.RemovePropertyController.onSubmit(srn, index, mode)
    )
}
