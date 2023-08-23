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
import controllers.PSRController
import controllers.actions._
import controllers.nonsipp.common.IdentityTypeController
import controllers.nonsipp.common.IdentityTypeController.viewModel
import forms.RadioListFormProvider
import models.SchemeId.Srn
import models.{IdentitySubject, IdentityType, Mode, NormalMode, SchemeHoldLandProperty}
import navigation.Navigator
import pages.nonsipp.common.IdentityTypePage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import utils.FormUtils.FormOps
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.RadioListView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class PropertyAcquiredFromController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  formProvider: RadioListFormProvider,
  view: RadioListView
)(implicit ec: ExecutionContext)
    extends PSRController {

  val form = PropertyAcquiredFromController.form(formProvider)

  def onPageLoad(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    subject: IdentitySubject
  ): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    val form = IdentityTypeController.form(formProvider, subject)
    Ok(
      view(
        form.fromUserAnswers(IdentityTypePage(srn, index, subject)),
        viewModel(srn, index, mode, subject, request.userAnswers)
      )
    )
  }

  def onSubmit(
    srn: Srn,
    index: Max5000,
    mode: Mode,
    subject: IdentitySubject
  ): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    val form = IdentityTypeController.form(formProvider, subject)
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future
            .successful(BadRequest(view(formWithErrors, viewModel(srn, index, mode, subject, request.userAnswers)))),
        answer => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(IdentityTypePage(srn, index, subject), answer))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(IdentityTypePage(srn, index, subject), NormalMode, updatedAnswers))
        }
      )
  }
}

object PropertyAcquiredFromController {

  def form(formProvider: RadioListFormProvider): Form[IdentityType] =
    formProvider("whyDoesSchemeHoldLandProperty.error.required")

  def viewModel(
    srn: Srn,
    index: Max5000,
    schemeName: String,
    addressLine1: String,
    mode: Mode
  ): FormPageViewModel[RadioListViewModel] =
    RadioListViewModel(
      "landOrPropertyAcquiredFrom.title",
      Message("landOrPropertyAcquiredFrom.heading", schemeName, addressLine1),
      List(
        RadioListRowViewModel(
          Message("whyDoesSchemeHoldLandProperty.option1"),
          SchemeHoldLandProperty.Acquisition.name,
          Message("whyDoesSchemeHoldLandProperty.option1.hint")
        ),
        RadioListRowViewModel("whyDoesSchemeHoldLandProperty.option2", SchemeHoldLandProperty.Contribution.name),
        RadioListRowViewModel("whyDoesSchemeHoldLandProperty.option3", SchemeHoldLandProperty.Transfer.name)
      ),
      routes.WhyDoesSchemeHoldLandPropertyController.onSubmit(srn, index, mode)
    )
}
