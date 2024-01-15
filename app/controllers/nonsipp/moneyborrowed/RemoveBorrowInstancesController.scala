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

package controllers.nonsipp.moneyborrowed

import com.google.inject.Inject
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.nonsipp.moneyborrowed._
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrSubmissionService, SaveService}
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class RemoveBorrowInstancesController @Inject()(
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

  private val form = RemoveBorrowInstancesController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      (for {
        borrows <- request.userAnswers.get(BorrowedAmountAndRatePage(srn, index)).getOrRecoverJourney
        lenderName <- request.userAnswers.get(LenderNamePage(srn, index)).getOrRecoverJourney
      } yield {
        val preparedForm = request.userAnswers.fillForm(RemoveBorrowInstancesPage(srn, index), form)
        Ok(
          view(
            preparedForm,
            RemoveBorrowInstancesController.viewModel(srn, index, mode, borrows._1.displayAs, lenderName)
          )
        )
      }).merge
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors => {
            Future.successful {
              (
                for {
                  borrows <- request.userAnswers.get(BorrowedAmountAndRatePage(srn, index)).getOrRecoverJourney
                  lenderName <- request.userAnswers.get(LenderNamePage(srn, index)).getOrRecoverJourney
                } yield {
                  BadRequest(
                    view(
                      errors,
                      RemoveBorrowInstancesController.viewModel(srn, index, mode, borrows._1.displayAs, lenderName)
                    )
                  )
                }
              ).merge
            }
          },
          value =>
            if (value) {
              for {
                updatedAnswers <- Future
                  .fromTry(removeAllMoneyBorrowedPages(srn, index, request.userAnswers))
                _ <- saveService.save(updatedAnswers)
                redirectTo <- psrSubmissionService
                  .submitPsrDetails(srn)(implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                  .map {
                    case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    case Some(_) =>
                      Redirect(navigator.nextPage(RemoveBorrowInstancesPage(srn, index), mode, updatedAnswers))
                  }
              } yield redirectTo
            } else {
              Future
                .successful(
                  Redirect(navigator.nextPage(RemoveBorrowInstancesPage(srn, index), mode, request.userAnswers))
                )
            }
        )
  }

  private def removeAllMoneyBorrowedPages(
    srn: Srn,
    index: Max5000,
    userAnswers: UserAnswers
  ): Try[UserAnswers] = {

    val mustRemovedUa = userAnswers
      .remove(LenderNamePage(srn, index))
      .flatMap(_.remove(IsLenderConnectedPartyPage(srn, index)))
      .flatMap(_.remove(BorrowedAmountAndRatePage(srn, index)))
      .flatMap(_.remove(WhenBorrowedPage(srn, index)))
      .flatMap(_.remove(ValueOfSchemeAssetsWhenMoneyBorrowedPage(srn, index)))
      .flatMap(_.remove(WhySchemeBorrowedMoneyPage(srn, index)))

    if (userAnswers.map(LenderNamePages(srn)).size == 1) {
      mustRemovedUa.flatMap(_.remove(MoneyBorrowedPage(srn)))
    } else {
      mustRemovedUa
    }
  }
}

object RemoveBorrowInstancesController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeBorrowInstances.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    amount: String,
    lendersName: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "removeBorrowInstances.title",
      Message("removeBorrowInstances.heading", amount, lendersName),
      routes.RemoveBorrowInstancesController.onSubmit(srn, index, mode)
    )
}
