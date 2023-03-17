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

import controllers.actions._
import forms.TextFormProvider

import javax.inject.Inject
import models.{Mode, NameDOB}
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import viewmodels.models.TextAreaViewModel
import viewmodels.implicits._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.TextAreaView
import services.SaveService
import pages.{MemberDetailsPage, NoNINOPage}
import NoNINOController._
import config.Refined.Max99
import models.requests.DataRequest

import scala.concurrent.{ExecutionContext, Future}

class NoNINOController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextAreaView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(memberFullName: String): Form[String] = NoNINOController.form(formProvider, memberFullName)

  def onPageLoad(srn: Srn, index: Max99, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      withMemberDetails(srn, index) { memberDetails =>
        val preparedForm =
          request.userAnswers
            .get(NoNINOPage(srn, index))
            .fold(form(memberDetails.fullName))(form(memberDetails.fullName).fill)
        Future.successful(Ok(view(preparedForm, viewModel(srn, memberDetails.fullName, index, mode))))
      }
  }

  def onSubmit(srn: Srn, index: Max99, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      withMemberDetails(srn, index) { memberDetails =>
        form(memberDetails.fullName)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, memberDetails.fullName, index, mode)))),
            value =>
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(NoNINOPage(srn, index), value))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(NoNINOPage(srn, index), mode, updatedAnswers))
          )
      }
  }

  private def withMemberDetails(srn: Srn, index: Max99)(
    f: NameDOB => Future[Result]
  )(implicit request: DataRequest[_]): Future[Result] =
    request.userAnswers.get(MemberDetailsPage(srn, index)) match {
      case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(memberDetails) => f(memberDetails)
    }
}

object NoNINOController {
  def form(formProvider: TextFormProvider, memberFullName: String): Form[String] = formProvider.textArea(
    "noNINO.error.required",
    "noNINO.error.length",
    "noNINO.error.invalid",
    memberFullName
  )

  def viewModel(srn: Srn, memberFullName: String, index: Max99, mode: Mode): TextAreaViewModel = TextAreaViewModel(
    "noNINO.title",
    "noNINO.heading" -> memberFullName,
    controllers.routes.NoNINOController.onSubmit(srn, index, mode)
  )
}
