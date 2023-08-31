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

package controllers.nonsipp.landorproperty

import config.Refined.Max5000
import controllers.actions._
import forms.TextFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.landorproperty.LandPropertyIndividualSellersNamePage
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

  def onPageLoad(srn: Srn, index: Max5000, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Ok(
        view(
          form.fromUserAnswers(LandPropertyIndividualSellersNamePage(srn, index)),
          LandPropertyIndividualSellersNameController.viewModel(srn, index, mode)
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
              BadRequest(view(formWithErrors, LandPropertyIndividualSellersNameController.viewModel(srn, index, mode)))
            ),
          answer => {
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(LandPropertyIndividualSellersNamePage(srn, index), answer))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(LandPropertyIndividualSellersNamePage(srn, index), mode, updatedAnswers)
            )
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