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

package controllers.nonsipp.memberdetails

import services.SaveService
import pages.nonsipp.memberdetails.{MemberDetailsPage, NoNINOPage}
import viewmodels.implicits._
import play.api.mvc._
import utils.IntUtils.{toInt, toRefined300}
import controllers.actions._
import navigation.Navigator
import forms.TextFormProvider
import models.{Mode, NameDOB}
import controllers.nonsipp.memberdetails.NoNINOController._
import config.RefinedTypes.Max300
import views.html.TextAreaView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.models.{FormPageViewModel, TextAreaViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class NoNINOController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: TextFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: TextAreaView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(memberFullName: String): Form[String] = NoNINOController.form(formProvider, memberFullName)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      withMemberDetails(srn, index) { memberDetails =>
        val preparedForm =
          request.userAnswers
            .get(NoNINOPage(srn, index))
            .fold(form(memberDetails.fullName))(form(memberDetails.fullName).fill)
        Future.successful(Ok(view(preparedForm, viewModel(srn, memberDetails.fullName, index, mode))))
      }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      withMemberDetails(srn, index) { memberDetails =>
        form(memberDetails.fullName)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, memberDetails.fullName, index, mode)))),
            value =>
              for {
                updatedAnswers <- request.userAnswers
                  .set(NoNINOPage(srn, index), value)
                  .mapK
                nextPage = navigator.nextPage(NoNINOPage(srn, index), mode, updatedAnswers)
                updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
                _ <- saveService.save(updatedProgressAnswers)
              } yield Redirect(nextPage)
          )
      }
  }

  private def withMemberDetails(srn: Srn, index: Max300)(
    f: NameDOB => Future[Result]
  )(implicit request: DataRequest[?]): Future[Result] =
    request.userAnswers.get(MemberDetailsPage(srn, index)) match {
      case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(memberDetails) => f(memberDetails)
    }
}

object NoNINOController {
  def form(formProvider: TextFormProvider, memberFullName: String): Form[String] = formProvider.textArea(
    "noNINO.error.required",
    "noNINO.error.length",
    "error.textarea.invalid",
    memberFullName
  )

  def viewModel(srn: Srn, memberFullName: String, index: Max300, mode: Mode): FormPageViewModel[TextAreaViewModel] =
    FormPageViewModel(
      "noNINO.title",
      "noNINO.heading" -> memberFullName,
      TextAreaViewModel(),
      routes.NoNINOController.onSubmit(srn, index, mode)
    )
}
