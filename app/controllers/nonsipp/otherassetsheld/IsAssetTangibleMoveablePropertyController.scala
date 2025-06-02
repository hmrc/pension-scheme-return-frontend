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

package controllers.nonsipp.otherassetsheld

import services.SaveService
import viewmodels.implicits._
import utils.FormUtils.FormOps
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import pages.nonsipp.otherassetsheld._
import config.FrontendAppConfig
import utils.IntUtils.{toInt, toRefined5000}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.Mode
import play.api.i18n.MessagesApi
import play.api.data.Form
import config.RefinedTypes.Max5000
import controllers.PSRController
import views.html.YesNoPageView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.{LinkMessage, Message, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

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

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      Ok(
        view(
          form.fromUserAnswers(IsAssetTangibleMoveablePropertyPage(srn, index)),
          IsAssetTangibleMoveablePropertyController
            .viewModel(srn, index, config.urls.tangibleMoveableProperty, mode)
        )
      )
    }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
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
              nextPage = navigator.nextPage(IsAssetTangibleMoveablePropertyPage(srn, index), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield (
              isPrePopulation,
              request.userAnswers.get(WhyDoesSchemeHoldAssetsPage(srn, index)),
              request.userAnswers.get(CostOfOtherAssetPage(srn, index)),
              request.userAnswers.get(IncomeFromAssetPage(srn, index))
            ) match {
              case (true, Some(_), Some(_), None) =>
                Redirect(
                  controllers.nonsipp.otherassetsheld.routes.IncomeFromAssetController
                    .onPageLoad(srn, index, mode)
                )
              case _ =>
                Redirect(nextPage)
            }
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
    FormPageViewModel(
      Message("otherAssets.tangibleMoveableProperty.title"),
      Message("otherAssets.tangibleMoveableProperty.heading"),
      YesNoPageViewModel(
        legend = Some(Message("otherAssets.tangibleMoveableProperty.label"))
      ),
      onSubmit =
        controllers.nonsipp.otherassetsheld.routes.IsAssetTangibleMoveablePropertyController.onSubmit(srn, index, mode)
    ).withDescription(
      ParagraphMessage(
        LinkMessage(
          "otherAssets.tangibleMoveableProperty.paragraph.link",
          tangibleMoveableProperty,
          Map("rel" -> "noreferrer noopener", "target" -> "_blank")
        ),
        "otherAssets.tangibleMoveableProperty.paragraph"
      )
    )
}
