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

package controllers.nonsipp.landorpropertydisposal

import config.Refined.{Max50, Max5000}
import controllers.actions._
import controllers.PSRController
import forms.TextFormProvider
import models.Mode
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.landorpropertydisposal.CompanyBuyerNamePage
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import viewmodels.implicits._
import viewmodels.models._
import views.html.TextInputView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class CompanyBuyerNameController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextInputView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val form = CompanyBuyerNameController.form(formProvider)

  def onPageLoad(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      val preparedForm =
        request.userAnswers.fillForm(CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex), form)
      Ok(view(preparedForm, CompanyBuyerNameController.viewModel(srn, landOrPropertyIndex, disposalIndex, mode)))
    }

  def onSubmit(srn: Srn, landOrPropertyIndex: Max5000, disposalIndex: Max50, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  CompanyBuyerNameController.viewModel(srn, landOrPropertyIndex, disposalIndex, mode)
                )
              )
            ),
          value =>
            for {
              updatedAnswers <- Future
                .fromTry(request.userAnswers.set(CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(
              navigator.nextPage(CompanyBuyerNamePage(srn, landOrPropertyIndex, disposalIndex), mode, updatedAnswers)
            )
        )
    }
}

object CompanyBuyerNameController {
  def form(formProvider: TextFormProvider): Form[String] = formProvider.text(
    "companyBuyerName.error.required",
    "companyBuyerName.error.tooLong",
    "companyBuyerName.error.invalid"
  )

  def viewModel(
    srn: Srn,
    landOrPropertyIndex: Max5000,
    disposalIndex: Max50,
    mode: Mode
  ): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      "companyBuyerName.title",
      "companyBuyerName.heading",
      TextInputViewModel(isFixedLength = true),
      routes.CompanyBuyerNameController.onSubmit(srn, landOrPropertyIndex, disposalIndex, mode)
    )
}
