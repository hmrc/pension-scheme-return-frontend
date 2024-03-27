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

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.nonsipp.moneyborrowed.IsLenderConnectedPartyController.viewModel
import config.Refined.Max5000
import config.FrontendAppConfig
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.data.Form
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import pages.nonsipp.moneyborrowed.{IsLenderConnectedPartyPage, LenderNamePage}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, FurtherDetailsViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}
import scala.Option

import javax.inject.{Inject, Named}

class IsLenderConnectedPartyController @Inject()(
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
    IsLenderConnectedPartyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      request.usingAnswer(LenderNamePage(srn, index)).sync { lenderName =>
        Ok(
          view(
            form.fromUserAnswers(IsLenderConnectedPartyPage(srn, index)),
            viewModel(srn, index, lenderName, config.urls.incomeTaxAct, mode)
          )
        )
      }
    }

  def onSubmit(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          errors =>
            request.usingAnswer(LenderNamePage(srn, index)).async { lenderName =>
              Future.successful(
                BadRequest(view(errors, viewModel(srn, index, lenderName, config.urls.incomeTaxAct, mode)))
              )
            },
          success =>
            for {
              userAnswers <- Future
                .fromTry(request.userAnswers.set(IsLenderConnectedPartyPage(srn, index), success))
              _ <- saveService.save(userAnswers)
            } yield {
              Redirect(navigator.nextPage(IsLenderConnectedPartyPage(srn, index), mode, userAnswers))
            }
        )
    }
}
object IsLenderConnectedPartyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "isLenderConnectedParty.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    lenderName: String,
    incomeTaxAct: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("isLenderConnectedParty.title"),
      Message("isLenderConnectedParty.heading", lenderName),
      Option(
        FurtherDetailsViewModel(
          Message("isLenderConnectedParty.content"),
          ParagraphMessage("isLenderConnectedParty.paragraph1") ++
            ParagraphMessage("isLenderConnectedParty.paragraph2") ++
            ParagraphMessage(
              "isLenderConnectedParty.paragraph3",
              LinkMessage(
                "isLenderConnectedParty.paragraph3.link",
                incomeTaxAct,
                Map("rel" -> "noreferrer noopener", "target" -> "_blank")
              )
            )
        )
      ),
      routes.IsLenderConnectedPartyController.onSubmit(srn, index, mode)
    )
}
