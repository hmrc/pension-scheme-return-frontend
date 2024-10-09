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

package controllers.nonsipp.otherassetsdisposal

import services.SaveService
import pages.nonsipp.otherassetsdisposal.IndividualNameOfAssetBuyerPage
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
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextInputViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class IndividualNameOfAssetBuyerController @Inject()(
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

  private def form = IndividualNameOfAssetBuyerController.form(formProvider)

  def onPageLoad(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex)),
          IndividualNameOfAssetBuyerController.viewModel(srn, assetIndex, disposalIndex, mode)
        )
      )
    }

  def onSubmit(srn: Srn, assetIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  IndividualNameOfAssetBuyerController.viewModel(srn, assetIndex, disposalIndex, mode)
                )
              )
            ),
          answer => {
            for {
              updatedAnswers <- Future.fromTry(
                request.userAnswers
                  .set(IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex), answer)
              )
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(
                IndividualNameOfAssetBuyerPage(srn, assetIndex, disposalIndex),
                mode,
                updatedAnswers
              )
            )
          }
        )
    }
}

object IndividualNameOfAssetBuyerController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.name(
    "assetDisposal.individualNameOfAssetBuyer.error.required",
    "assetDisposal.individualNameOfAssetBuyer.error.length",
    "assetDisposal.individualNameOfAssetBuyer.error.invalid.characters"
  )

  def viewModel(
    srn: Srn,
    assetIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("assetDisposal.individualNameOfAssetBuyer.title"),
      Message("assetDisposal.individualNameOfAssetBuyer.heading"),
      TextInputViewModel(true),
      routes.IndividualNameOfAssetBuyerController.onSubmit(srn, assetIndex, disposalIndex, mode)
    )
}
