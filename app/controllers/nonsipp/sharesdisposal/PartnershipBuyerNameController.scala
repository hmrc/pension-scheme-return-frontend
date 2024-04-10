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

import services.SaveService
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.Refined.{Max50, Max5000}
import controllers.actions._
import pages.nonsipp.sharesdisposal.PartnershipBuyerNamePage
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.data.Form
import views.html.TextInputView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextInputViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class PartnershipBuyerNameController @Inject()(
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

  private def form = PartnershipBuyerNameController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(PartnershipBuyerNamePage(srn, index, disposalIndex)),
          PartnershipBuyerNameController.viewModel(srn, index, disposalIndex, mode)
        )
      )
    }

  def onSubmit(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  PartnershipBuyerNameController.viewModel(srn, index, disposalIndex, mode)
                )
              )
            ),
          answer => {
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers
                  .set(PartnershipBuyerNamePage(srn, index, disposalIndex), answer)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                PartnershipBuyerNamePage(srn, index, disposalIndex),
                mode,
                updatedAnswers
              )
            )
          }
        )
    }
}

object PartnershipBuyerNameController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.name(
    "sharesDisposal.partnershipBuyerName.error.required",
    "sharesDisposal.partnershipBuyerName.error.length",
    "sharesDisposal.partnershipBuyerName.error.invalid.characters"
  )

  def viewModel(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("sharesDisposal.partnershipBuyerName.title"),
      Message("sharesDisposal.partnershipBuyerName.heading"),
      TextInputViewModel(true),
      routes.PartnershipBuyerNameController.onSubmit(srn, index, disposalIndex, mode)
    )
}
