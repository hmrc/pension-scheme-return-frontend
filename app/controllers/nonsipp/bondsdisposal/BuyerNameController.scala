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

package controllers.nonsipp.bondsdisposal

import services.SaveService
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.{Max50, Max5000}
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.data.Form
import views.html.TextInputView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import pages.nonsipp.bondsdisposal.BuyerNamePage
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextInputViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class BuyerNameController @Inject()(
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

  private def form = BuyerNameController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(BuyerNamePage(srn, index, disposalIndex)),
          BuyerNameController.viewModel(srn, index, disposalIndex, mode)
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
                  BuyerNameController.viewModel(srn, index, disposalIndex, mode)
                )
              )
            ),
          answer => {
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers
                  .set(BuyerNamePage(srn, index, disposalIndex), answer)
              )
              nextPage = navigator.nextPage(
                BuyerNamePage(srn, index, disposalIndex),
                mode,
                updatedAnswers
              )
              updatedProgressAnswers <- saveProgress(srn, index, disposalIndex, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
          }
        )
    }
}

object BuyerNameController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.name(
    "bondsDisposal.buyerName.error.required",
    "bondsDisposal.buyerName.error.length",
    "bondsDisposal.buyerName.error.invalid.characters"
  )

  def viewModel(srn: Srn, index: Max5000, disposalIndex: Max50, mode: Mode): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("bondsDisposal.buyerName.title"),
      Message("bondsDisposal.buyerName.heading"),
      TextInputViewModel(true),
      routes.BuyerNameController.onSubmit(srn, index, disposalIndex, mode)
    )
}
