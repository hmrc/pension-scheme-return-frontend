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

package controllers.nonsipp.bonds

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.bonds.{NameOfBondsPage, RemoveBondsPage}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
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

class RemoveBondsController @Inject()(
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

  private val form = RemoveBondsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          nameOfBonds <- request.userAnswers.get(NameOfBondsPage(srn, index)).getOrRedirectToTaskList(srn)
        } yield {
          val preparedForm =
            request.userAnswers.fillForm(RemoveBondsPage(srn, index), form)
          Ok(
            view(
              preparedForm,
              RemoveBondsController
                .viewModel(srn, index, nameOfBonds, mode)
            )
          )
        }
      ).merge
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.userAnswers.get(NameOfBondsPage(srn, index)).getOrRecoverJourney { nameOfBonds =>
              Future.successful(
                BadRequest(
                  view(
                    errors,
                    RemoveBondsController
                      .viewModel(srn, index, nameOfBonds, mode)
                  )
                )
              )
            },
          value =>
            if (value) {
              for {
                removedUserAnswers <- Future
                  .fromTry(
                    // Remove the first page in the journey only
                    request.userAnswers.remove(NameOfBondsPage(srn, index))
                  )

                _ <- saveService.save(removedUserAnswers)
                redirectTo <- psrSubmissionService
                  .submitPsrDetailsWithUA(
                    srn,
                    removedUserAnswers,
                    fallbackCall = controllers.nonsipp.bonds.routes.BondsListController.onPageLoad(srn, 1, mode)
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
                          RemoveBondsPage(srn, index),
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
                      RemoveBondsPage(srn, index),
                      mode,
                      request.userAnswers
                    )
                  )
                )
            }
        )
    }

}

object RemoveBondsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "bonds.removeBond.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    nameOfBonds: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "bonds.removeBond.title",
      Message("bonds.removeBond.heading", nameOfBonds),
      routes.RemoveBondsController.onSubmit(srn, index, mode)
    )
}
