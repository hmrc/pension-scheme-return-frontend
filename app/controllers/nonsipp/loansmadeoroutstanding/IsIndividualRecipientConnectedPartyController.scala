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

import config.FrontendAppConfig
import config.Refined.Max5000
import controllers.actions._
import controllers.nonsipp.loansmadeoroutstanding.IsIndividualRecipientConnectedPartyController.viewModel
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.{IndividualRecipientNamePage, IsIndividualRecipientConnectedPartyPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, FurtherDetailsViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class IsIndividualRecipientConnectedPartyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form: Form[Boolean] =
    IsIndividualRecipientConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.usingAnswer(IndividualRecipientNamePage(srn, index)).sync { individualName =>
        Ok(
          view(
            form.fromUserAnswers(IsIndividualRecipientConnectedPartyPage(srn, index)),
            viewModel(srn, index, individualName, config.urls.incomeTaxAct, mode)
          )
        )
      }
  }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.usingAnswer(IndividualRecipientNamePage(srn, index)).async { individualName =>
              Future.successful(
                BadRequest(view(errors, viewModel(srn, index, individualName, config.urls.incomeTaxAct, mode)))
              )
            },
          success =>
            for {
              userAnswers <- Future
                .fromTry(request.userAnswers.set(IsIndividualRecipientConnectedPartyPage(srn, index), success))
              _ <- saveService.save(userAnswers)
            } yield {
              Redirect(navigator.nextPage(IsIndividualRecipientConnectedPartyPage(srn, index), mode, userAnswers))
            }
        )
  }
}
object IsIndividualRecipientConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "isIndividualRecipientConnectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    individualName: String,
    incomeTaxAct: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("isIndividualRecipientConnectedParty.title"),
      Message("isIndividualRecipientConnectedParty.heading", individualName),
      Option(
        FurtherDetailsViewModel(
          Message("isIndividualRecipientConnectedParty.content"),
          ParagraphMessage("isIndividualRecipientConnectedParty.paragraph1") ++
            ParagraphMessage("isIndividualRecipientConnectedParty.paragraph2") ++
            ParagraphMessage(
              "isIndividualRecipientConnectedParty.paragraph3",
              LinkMessage(
                "isIndividualRecipientConnectedParty.paragraph3.link",
                incomeTaxAct,
                Map("rel" -> "noreferrer noopener", "target" -> "_blank")
              )
            )
        )
      ),
      routes.IsIndividualRecipientConnectedPartyController.onSubmit(srn, index, mode)
    )
}
