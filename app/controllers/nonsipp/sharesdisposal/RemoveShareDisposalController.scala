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

package controllers.nonsipp.sharesdisposal

import services.{PsrSubmissionService, SaveService}
import viewmodels.implicits._
import utils.IntUtils.{toInt, toRefined50, toRefined5000}
import controllers.actions._
import pages.nonsipp.sharesdisposal._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import pages.nonsipp.shares.CompanyNameRelatedSharesPage
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.{Max50, Max5000}
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import controllers.nonsipp.sharesdisposal.RemoveShareDisposalController._
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveShareDisposalController @Inject() (
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

  def onPageLoad(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          _ <- request.userAnswers
            .get(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex))
            .getOrRedirectToTaskList(srn)
          nameOfSharesCompany <- request.userAnswers
            .get(CompanyNameRelatedSharesPage(srn, shareIndex))
            .getOrRedirectToTaskList(srn)
        } yield Ok(view(form, viewModel(srn, shareIndex, disposalIndex, nameOfSharesCompany, mode)))
      ).merge
    }

  def onSubmit(srn: Srn, shareIndex: Int, disposalIndex: Int, mode: Mode): Action[AnyContent] =
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
          removeDisposal =>
            if (removeDisposal) {
              for {
                removedUserAnswers <- Future
                  .fromTry(
                    request.userAnswers
                      .remove(HowWereSharesDisposedPage(srn, shareIndex, disposalIndex))
                      .remove(SharesDisposalCompleted(srn))
                      .remove(SharesDisposalProgress(srn, shareIndex, disposalIndex))
                  )
                _ <- saveService.save(removedUserAnswers)
                submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
                  srn,
                  removedUserAnswers,
                  fallbackCall =
                    controllers.nonsipp.sharesdisposal.routes.ReportedSharesDisposalListController.onPageLoad(srn, 1)
                )
              } yield submissionResult.getOrRecoverJourney(_ =>
                Redirect(navigator.nextPage(RemoveShareDisposalPage(srn), mode, removedUserAnswers))
              )
            } else {
              Future.successful(
                Redirect(navigator.nextPage(RemoveShareDisposalPage(srn), mode, request.userAnswers))
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
      title = "sharesDisposal.removeShareDisposal.title",
      heading = Message("sharesDisposal.removeShareDisposal.heading", companyName),
      onSubmit = routes.RemoveShareDisposalController.onSubmit(srn, shareIndex, disposalIndex, mode)
    )
}
