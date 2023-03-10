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

package controllers

import config.Refined.Max99
import controllers.MemberDetailsController._
import controllers.actions._
import forms.NameDOBFormProvider
import forms.mappings.DateFormErrors
import models.SchemeId.Srn
import models.{Mode, NameDOB}
import navigation.Navigator
import pages.MemberDetailsPage
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils._
import viewmodels.DisplayMessage.SimpleMessage
import viewmodels.models.NameDOBViewModel
import views.html.NameDOBView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MemberDetailsController @Inject()(
                                         override val messagesApi: MessagesApi,
                                         navigator: Navigator,
                                         identifyAndRequireData: IdentifyAndRequireData,
                                         saveService: SaveService,
                                         formProvider: NameDOBFormProvider,
                                         view: NameDOBView,
                                         val controllerComponents: MessagesControllerComponents
                                       )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private val form = MemberDetailsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max99, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      Ok(view(form.fromUserAnswers(MemberDetailsPage(srn, index)), viewModel(srn, index, mode)))
  }

  def onSubmit(srn: Srn, index: Max99, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      form.bindFromRequest().fold(
        formWithErrors => Future.successful(
          BadRequest(view(formWithErrors, viewModel(srn, index, mode)))
        ),
        answer => {
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(MemberDetailsPage(srn, index), answer))
            _ <- saveService.save(updatedAnswers)
          } yield Redirect(navigator.nextPage(MemberDetailsPage(srn, index), mode, updatedAnswers))
        }
      )
  }
}

object MemberDetailsController {
  def form(formProvider: NameDOBFormProvider): Form[NameDOB] = formProvider(
    "memberDetails.firstName.error.required",
    "memberDetails.firstName.error.invalid",
    "memberDetails.firstName.error.length",
    "memberDetails.lastName.error.required",
    "memberDetails.lastName.error.invalid",
    "memberDetails.lastName.error.length",
    DateFormErrors(
      "memberDetails.dateOfBirth.error.required.all",
      "memberDetails.dateOfBirth.error.required.day",
      "memberDetails.dateOfBirth.error.required.month",
      "memberDetails.dateOfBirth.error.required.year",
      "memberDetails.dateOfBirth.error.required.two",
      "memberDetails.dateOfBirth.error.invalid.date",
      "memberDetails.dateOfBirth.error.invalid.characters",
    )
  )

  def viewModel(srn: Srn, index: Max99, mode: Mode): NameDOBViewModel = NameDOBViewModel(
    SimpleMessage("memberDetails.title"),
    SimpleMessage("memberDetails.heading"),
    SimpleMessage("memberDetails.firstName"),
    SimpleMessage("memberDetails.lastName"),
    SimpleMessage("memberDetails.dateOfBirth"),
    SimpleMessage("memberDetails.dateOfBirth.hint"),
    controllers.routes.MemberDetailsController.onSubmit(srn, index, mode)
  )
}