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

package controllers.nonsipp.memberpayments

import com.google.inject.Inject
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import forms.YesNoPageFormProvider
import models.SchemeId.Srn
import models.{Mode, UserAnswers}
import navigation.Navigator
import pages.nonsipp.memberpayments.{RemoveUnallocatedAmountPage, UnallocatedEmployerAmountPage}
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.Named
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class RemoveUnallocatedAmountController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = RemoveUnallocatedAmountController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    (for {
      unallocatedAmount <- request.userAnswers.get(UnallocatedEmployerAmountPage(srn)).getOrRecoverJourney
    } yield {
      val preparedForm = request.userAnswers.fillForm(RemoveUnallocatedAmountPage(srn), form)
      Ok(
        view(
          preparedForm,
          RemoveUnallocatedAmountController.viewModel(srn, mode, unallocatedAmount.displayAs)
        )
      )
    }).merge
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        errors => {
          Future.successful {
            (
              for {
                unallocatedAmount <- request.userAnswers.get(UnallocatedEmployerAmountPage(srn)).getOrRecoverJourney
              } yield {
                BadRequest(
                  view(
                    errors,
                    RemoveUnallocatedAmountController.viewModel(srn, mode, unallocatedAmount.displayAs)
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
                .fromTry(removeUnallocatedAmountPage(srn, request.userAnswers))
              _ <- saveService.save(updatedAnswers)
            } yield {
              Redirect(navigator.nextPage(RemoveUnallocatedAmountPage(srn), mode, updatedAnswers))
            }
          } else {
            Future
              .successful(
                Redirect(navigator.nextPage(RemoveUnallocatedAmountPage(srn), mode, request.userAnswers))
              )
          }
      )
  }

  private def removeUnallocatedAmountPage(
    srn: Srn,
    userAnswers: UserAnswers
  ): Try[UserAnswers] =
    userAnswers
      .remove(UnallocatedEmployerAmountPage(srn))
}

object RemoveUnallocatedAmountController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeBorrowInstances.error.required"
  )

  def viewModel(
    srn: Srn,
    mode: Mode,
    amount: String
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      "removeBorrowInstances.title",
      Message("removeBorrowInstances.heading", amount),
      controllers.nonsipp.memberpayments.routes.RemoveUnallocatedAmountController.onSubmit(srn, mode)
    )
}
