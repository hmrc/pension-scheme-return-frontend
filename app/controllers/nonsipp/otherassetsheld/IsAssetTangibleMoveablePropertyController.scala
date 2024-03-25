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

package controllers.nonsipp.otherassetsheld

import config.FrontendAppConfig
import config.Refined.Max5000
import controllers.PSRController
import controllers.actions._
import forms.YesNoPageFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.otherassetsheld.IsAssetTangibleMoveablePropertyPage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.{FormPageViewModel, FurtherDetailsViewModel, YesNoPageViewModel}
import views.html.YesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class IsAssetTangibleMoveablePropertyController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = IsAssetTangibleMoveablePropertyController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(IsAssetTangibleMoveablePropertyPage(srn, index)),
          IsAssetTangibleMoveablePropertyController
            .viewModel(srn, index, config.urls.tangibleMoveableProperty, mode)
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
              BadRequest(
                view(
                  formWithErrors,
                  IsAssetTangibleMoveablePropertyController
                    .viewModel(srn, index, config.urls.tangibleMoveableProperty, mode)
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(IsAssetTangibleMoveablePropertyPage(srn, index), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(IsAssetTangibleMoveablePropertyPage(srn, index), mode, updatedAnswers)
            )
        )
  }
}

object IsAssetTangibleMoveablePropertyController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "otherAssets.tangibleMoveableProperty.error.required"
  )

  def viewModel(
    srn: Srn,
    index: Max5000,
    tangibleMoveableProperty: String,
    mode: Mode
  ): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("otherAssets.tangibleMoveableProperty.title"),
      Message("otherAssets.tangibleMoveableProperty.heading"),
      Option(
        FurtherDetailsViewModel(
          Message("otherAssets.tangibleMoveableProperty.content"),
          ParagraphMessage(
            LinkMessage(
              "otherAssets.tangibleMoveableProperty.paragraph.link",
              tangibleMoveableProperty,
              Map("rel" -> "noreferrer noopener", "target" -> "_blank")
            ),
            "otherAssets.tangibleMoveableProperty.paragraph"
          )
        )
      ),
      controllers.nonsipp.otherassetsheld.routes.IsAssetTangibleMoveablePropertyController
        .onSubmit(srn, index, mode)
    )
}