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

package controllers.nonsipp.shares

import config.FrontendAppConfig
import controllers.actions._
import controllers.nonsipp.shares.DidSchemeHoldAnySharesController.viewModel
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import models.requests.DataRequest
import navigation.Navigator
import pages.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingPage
import pages.nonsipp.shares.DidSchemeHoldAnySharesPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{PsrSubmissionService, SaveService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.{LinkMessage, ListMessage, ListType, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class DidSchemeHoldAnySharesController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  psrSubmissionService: PsrSubmissionService,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = DidSchemeHoldAnySharesController.form(formProvider)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm = request.userAnswers.fillForm(DidSchemeHoldAnySharesPage(srn), form)
      Ok(view(preparedForm, viewModel(srn, request.schemeDetails.schemeName, config.urls.incomeTaxAct, mode)))
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(formWithErrors, viewModel(srn, request.schemeDetails.schemeName, config.urls.incomeTaxAct, mode))
              )
            ),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(DidSchemeHoldAnySharesPage(srn), value))
              _ <- saveService.save(updatedAnswers)
              redirectTo <- if (value) {
                Future.successful(Redirect(navigator.nextPage(LoansMadeOrOutstandingPage(srn), mode, updatedAnswers)))
              } else {
                psrSubmissionService
                  .submitPsrDetails(srn)(implicitly, implicitly, request = DataRequest(request.request, updatedAnswers))
                  .map {
                    case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
                    case Some(_) => Redirect(navigator.nextPage(LoansMadeOrOutstandingPage(srn), mode, updatedAnswers))
                  }
              }
            } yield redirectTo
        )
    }
}

object DidSchemeHoldAnySharesController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "didSchemeHoldAnyShares.error.required"
  )

  def viewModel(srn: Srn, schemeName: String, incomeTaxAct: String, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      "didSchemeHoldAnyShares.title",
      Message("didSchemeHoldAnyShares.heading", schemeName),
      YesNoPageViewModel(
        legend = Some(Message("didSchemeHoldAnyShares.legend"))
      ),
      controllers.nonsipp.shares.routes.DidSchemeHoldAnySharesController.onSubmit(srn, mode)
    ).withDescription(
      ParagraphMessage("didSchemeHoldAnyShares.paragraph1") ++
        ListMessage(
          ListType.Bullet,
          "didSchemeHoldAnyShares.listItem1",
          "didSchemeHoldAnyShares.listItem2",
          "didSchemeHoldAnyShares.listItem3"
        ) ++
        ParagraphMessage("didSchemeHoldAnyShares.paragraph2") ++
        ParagraphMessage(
          "didSchemeHoldAnyShares.paragraph3",
          LinkMessage(
            "didSchemeHoldAnyShares.paragraph3.link",
            incomeTaxAct,
            Map("rel" -> "noreferrer noopener", "target" -> "_blank")
          )
        )
    )
}
