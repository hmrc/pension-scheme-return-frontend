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

package controllers.nonsipp.sharesdisposal

import config.Refined.{Max50, Max5000}
import controllers.PSRController
import controllers.actions._
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import pages.nonsipp.sharesdisposal.{HowWereSharesDisposedPage, RemoveShareDisposalPage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrSubmissionService, SaveService}
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class RemoveShareDisposalController @Inject()(
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

  private val form = RemoveShareDisposalController.form(formProvider)

  def onPageLoad(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.userAnswers.get(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex)).getOrRecoverJourney { _ =>
        request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney {
          nameOfSharesCompany =>
            val preparedForm =
              request.userAnswers.fillForm(RemoveShareDisposalPage(srn, shareIndex, disposalIndex), form)
            Ok(
              view(
                preparedForm,
                RemoveShareDisposalController
                  .viewModel(srn, shareIndex, disposalIndex, nameOfSharesCompany, mode)
              )
            )
        }
      }
    }

  def onSubmit(srn: Srn, shareIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.userAnswers.get(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex)).getOrRecoverJourney {
              _ =>
                request.userAnswers.get(CompanyNameRelatedSharesPage(srn, shareIndex)).getOrRecoverJourney {
                  nameOfSharesCompany =>
                    Future.successful(
                      BadRequest(
                        view(
                          errors,
                          RemoveShareDisposalController
                            .viewModel(srn, shareIndex, disposalIndex, nameOfSharesCompany, mode)
                        )
                      )
                    )
                }
            },
          value =>
            if (value) {
              for {
                removedUserAnswers <- Future
                  .fromTry(
                    // remove the first page in the journey only
                    request.userAnswers.remove(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex))
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
                          RemoveShareDisposalPage(srn, shareIndex, disposalIndex),
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
                      RemoveShareDisposalPage(srn, shareIndex, disposalIndex),
                      mode,
                      request.userAnswers
                    )
                  )
                )
            }
        )
    }

}

object RemoveShareDisposalController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "sharesDisposal.removeShareDisposal.required"
  )

  def viewModel(
    srn: Srn,
    shareIndex: Max5000,
    disposalIndex: Max50,
    companyName: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "sharesDisposal.removeShareDisposal.title",
      Message("sharesDisposal.removeShareDisposal.heading", companyName),
      routes.RemoveShareDisposalController.onSubmit(srn, shareIndex, disposalIndex, mode)
    )
}
