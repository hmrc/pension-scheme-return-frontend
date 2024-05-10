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

package controllers.nonsipp.landorpropertydisposal

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import pages.nonsipp.landorproperty.LandOrPropertyChosenAddressPage
import pages.nonsipp.landorpropertydisposal._
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveLandPropertyDisposalController @Inject()(
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

  private val form = RemoveLandPropertyDisposalController.form(formProvider)

  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
        address =>
          val preparedForm =
            request.userAnswers.fillForm(RemoveLandPropertyDisposalPage(srn, landOrPropertyIndex, disposalIndex), form)
          Ok(
            view(
              preparedForm,
              RemoveLandPropertyDisposalController
                .viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, mode)
            )
          )
      }
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.userAnswers.get(LandOrPropertyChosenAddressPage(srn, landOrPropertyIndex)).getOrRecoverJourney {
              address =>
                Future.successful(
                  BadRequest(
                    view(
                      errors,
                      RemoveLandPropertyDisposalController
                        .viewModel(srn, landOrPropertyIndex, disposalIndex, address.addressLine1, mode)
                    )
                  )
                )
            },
          value =>
            if (value) {
              for {
                removedUserAnswers <- Future
                  .fromTry(
                    // remove the first page in the journey only
                    request.userAnswers.remove(HowWasPropertyDisposedOfPage(srn, landOrPropertyIndex, disposalIndex))
                  )

                _ <- saveService.save(removedUserAnswers)
                redirectTo <- psrSubmissionService
                  .submitPsrDetails(
                    srn,
                    fallbackCall =
                      controllers.nonsipp.landorpropertydisposal.routes.LandOrPropertyDisposalListController
                        .onPageLoad(srn, 1)
                  )(
                    implicitly,
                    implicitly,
                    request = DataRequest(request.request, removedUserAnswers)
                  )
                  .map {
                    case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    case Some(_) =>
                      Redirect(
                        navigator.nextPage(
                          RemoveLandPropertyDisposalPage(srn, landOrPropertyIndex, disposalIndex),
                          mode,
                          removedUserAnswers
                        )
                      )
                  }
              } yield redirectTo
            } else {
              Future
                .successful(
                  Redirect(
                    navigator.nextPage(
                      RemoveLandPropertyDisposalPage(srn, landOrPropertyIndex, disposalIndex),
                      mode,
                      request.userAnswers
                    )
                  )
                )
            }
        )
    }

}

object RemoveLandPropertyDisposalController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeLandPropertyDisposal.error.required"
  )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    addressLine1: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "removeLandPropertyDisposal.title",
      Message("removeLandPropertyDisposal.heading", addressLine1),
      routes.RemoveLandPropertyDisposalController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
