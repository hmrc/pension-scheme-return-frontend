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

package controllers.nonsipp.landorproperty

import services.SaveService
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max5000
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.Mode
import play.api.data.Form
import views.html.TextInputView
import models.SchemeId.Srn
import utils.IntUtils.{toInt, IntOpts}
import pages.nonsipp.landorproperty.LandPropertyIndividualSellersNamePage
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextInputViewModel}

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class LandPropertyIndividualSellersNameController @Inject()(
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

  private def form = LandPropertyIndividualSellersNameController.form(formProvider)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Ok(
        view(
          form.fromUserAnswers(LandPropertyIndividualSellersNamePage(srn, index.refined)),
          LandPropertyIndividualSellersNameController.viewModel(srn, index.refined, mode)
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
                view(formWithErrors, LandPropertyIndividualSellersNameController.viewModel(srn, index.refined, mode))
              )
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(LandPropertyIndividualSellersNamePage(srn, index.refined), answer))
              nextPage = navigator
                .nextPage(LandPropertyIndividualSellersNamePage(srn, index.refined), mode, updatedAnswers)
              updatedProgressAnswers <- saveProgress(srn, index.refined, updatedAnswers, nextPage)
              _ <- saveService.save(updatedProgressAnswers)
            } yield Redirect(nextPage)
          }
        )
  }
}

object LandPropertyIndividualSellersNameController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.name(
    "landPropertyIndividualSellersName.error.required",
    "landPropertyIndividualSellersName.error.tooLong",
    "landPropertyIndividualSellersName.error.invalid"
  )

  def viewModel(srn: Srn, index: Max5000, mode: Mode): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("landPropertyIndividualSellersName.title"),
      Message("landPropertyIndividualSellersName.heading"),
      TextInputViewModel(true),
      routes.LandPropertyIndividualSellersNameController.onSubmit(srn, index, mode)
    )
}
