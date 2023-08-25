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
import forms.TextFormProvider
import models.{CheckMode, Mode, NormalMode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{CompanyRecipientNamePage, LoansCYAPage, PartnershipRecipientNamePage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextInputViewModel}
import views.html.TextInputView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class PartnershipRecipientNameController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form = PartnershipRecipientNameController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Ok(
        view(
          form.fromUserAnswers(PartnershipRecipientNamePage(srn, index, mode)),
          PartnershipRecipientNameController.viewModel(srn, index, mode)
        )
      )
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(formWithErrors, PartnershipRecipientNameController.viewModel(srn, index, mode)))
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(PartnershipRecipientNamePage(srn, index, mode), answer))
              _ <- saveService.save(updatedAnswers)
            } yield {

              mode match {
                case CheckMode => {
                  (
                    updatedAnswers.get(PartnershipRecipientNamePage(srn, index, mode)),
                    request.userAnswers.get(PartnershipRecipientNamePage(srn, index, mode))
                  ) match {
                    case (Some(newAnswer), Some(previousAnswer)) => {
                      if (newAnswer == previousAnswer) {
                        Redirect(navigator.nextPage(LoansCYAPage(srn, index, mode), mode, updatedAnswers))
                      } else {
                        Redirect(
                          navigator.nextPage(PartnershipRecipientNamePage(srn, index, mode), CheckMode, updatedAnswers)
                        )
                      }
                    }
                  }
                }
                case NormalMode =>
                  Redirect(navigator.nextPage(PartnershipRecipientNamePage(srn, index, mode), mode, updatedAnswers))
              }

            }

          }
        )
  }
}

object PartnershipRecipientNameController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.textArea(
    "partnershipRecipientName.error.required",
    "partnershipRecipientName.error.length",
    "partnershipRecipientName.error.invalid.characters"
  )

  def viewModel(srn: Srn, index: Max5000, mode: Mode): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("partnershipRecipientName.title"),
      Message("partnershipRecipientName.heading"),
      TextInputViewModel(true),
      routes.PartnershipRecipientNameController.onSubmit(srn, index, mode)
    )
}
