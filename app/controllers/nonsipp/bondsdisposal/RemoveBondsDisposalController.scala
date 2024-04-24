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

package controllers.nonsipp.bondsdisposal

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.bonds.NameOfBondsPage
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import models.NormalMode
import views.html.YesNoPageView
import models.SchemeId.Srn
import controllers.nonsipp.bondsdisposal.RemoveBondsDisposalController._
import navigation.Navigator
import play.api.i18n.MessagesApi
import pages.nonsipp.bondsdisposal._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveBondsDisposalController @Inject()(
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

  private val form = RemoveBondsDisposalController.form(formProvider)

  def onPageLoad(srn: Srn, bondIndex: Max5000, disposalIndex: Max50): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(NameOfBondsPage(srn, bondIndex)).getOrRecoverJourney { nameOfBonds =>
        val preparedForm =
          request.userAnswers.fillForm(RemoveBondsDisposalPage(srn, bondIndex, disposalIndex), form)
        Ok(
          view(
            preparedForm,
            viewModel(srn, bondIndex, disposalIndex, nameOfBonds)
          )
        )
      }
    }

  def onSubmit(srn: Srn, bondIndex: Max5000, disposalIndex: Max50): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.userAnswers.get(NameOfBondsPage(srn, bondIndex)).getOrRecoverJourney { nameOfBonds =>
              Future.successful(
                BadRequest(
                  view(
                    errors,
                    viewModel(srn, bondIndex, disposalIndex, nameOfBonds)
                  )
                )
              )
            },
          value =>
            if (value) {
              for {
                removedUserAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .remove(HowWereBondsDisposedOfPage(srn, bondIndex, disposalIndex))
                      .remove(BondsDisposalProgress(srn, bondIndex, disposalIndex))
                      .remove(BondsDisposalCompleted(srn))
                  )

                _ <- saveService.save(removedUserAnswers)
                redirectTo <- psrSubmissionService
                  .submitPsrDetails(srn)(
                    implicitly,
                    implicitly,
                    request = DataRequest(request.request, removedUserAnswers)
                  )
                  .map {
                    case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    case Some(_) =>
                      Redirect(
                        navigator.nextPage(
                          RemoveBondsDisposalPage(srn, bondIndex, disposalIndex),
                          NormalMode,
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
                      RemoveBondsDisposalPage(srn, bondIndex, disposalIndex),
                      NormalMode,
                      request.userAnswers
                    )
                  )
                )
            }
        )
    }
}

object RemoveBondsDisposalController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "bondsDisposal.removeBondsDisposal.error.required"
  )

  def viewModel(
    srn: Srn,
    bondIndex: Max5000,
    disposalIndex: Max50,
    nameOfBonds: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      title = Message("bondsDisposal.removeBondsDisposal.title"),
      heading = Message("bondsDisposal.removeBondsDisposal.heading", nameOfBonds),
      onSubmit = routes.RemoveBondsDisposalController.onSubmit(srn, bondIndex, disposalIndex)
    )
}
