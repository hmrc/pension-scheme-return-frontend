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

package controllers.nonsipp.loansmadeoroutstanding

import config.Refined.Max5000
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.OtherRecipientDetailsController.viewModel
import forms.RecipientDetailsFormProvider
import models.SchemeId.Srn
import models.{CheckMode, Mode, NormalMode, RecipientDetails}
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{LoansCYAPage, OtherRecipientDetailsPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, RecipientDetailsViewModel}
import views.html.RecipientDetailsView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class OtherRecipientDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: RecipientDetailsFormProvider,
  view: RecipientDetailsView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = OtherRecipientDetailsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Ok(view(form.fromUserAnswers(OtherRecipientDetailsPage(srn, index, mode)), viewModel(srn, index, mode)))
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, viewModel(srn, index, mode)))
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(OtherRecipientDetailsPage(srn, index, mode), answer))
              _ <- saveService.save(updatedAnswers)
            } yield {
              Redirect(navigator.nextPage(OtherRecipientDetailsPage(srn, index, mode), mode, updatedAnswers))

            }
          }
        )
  }
}

object OtherRecipientDetailsController {
  def form(formProvider: RecipientDetailsFormProvider): Form[RecipientDetails] = formProvider(
    "otherRecipientDetails.name.error.required",
    "otherRecipientDetails.name.error.invalid",
    "otherRecipientDetails.name.error.length",
    "otherRecipientDetails.description.error.required",
    "otherRecipientDetails.description.error.invalid",
    "otherRecipientDetails.description.error.length"
  )

  def viewModel(srn: Srn, index: Max5000, mode: Mode): FormPageViewModel[RecipientDetailsViewModel] =
    FormPageViewModel(
      Message("otherRecipientDetails.title"),
      Message("otherRecipientDetails.heading"),
      RecipientDetailsViewModel(
        Message("otherRecipientDetails.name"),
        Message("otherRecipientDetails.description")
      ),
      routes.OtherRecipientDetailsController.onSubmit(srn, index, mode)
    )
}
