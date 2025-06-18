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
import controllers.nonsipp.memberdetails.DoesSchemeMemberHaveNINOController._
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, MemberDetailsPage}
import viewmodels.implicits._
import utils.FormUtils._
import play.api.mvc._
import config.RefinedTypes.Max300
import utils.IntUtils.{toInt, toRefined300}
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{Mode, NameDOB}
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class DoesSchemeMemberHaveNINOController @Inject() (
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(memberName: String): Form[Boolean] =
    DoesSchemeMemberHaveNINOController.form(formProvider, memberName)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      withMemberDetails(srn, index)(memberDetails =>
        Future.successful(
          Ok(
            view(
              form(memberDetails.fullName).fromUserAnswers(DoesMemberHaveNinoPage(srn, index)),
              viewModel(index, memberDetails.fullName, srn, mode)
            )
          )
        )
      )
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      withMemberDetails(srn, index)(memberDetails =>
        form(memberDetails.fullName)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(view(formWithErrors, viewModel(index, memberDetails.fullName, srn, mode)))
              ),
            value =>
              for {
                updatedAnswers <- request.userAnswers
                  .set(DoesMemberHaveNinoPage(srn, index), value)
                  .mapK
                nextPage = navigator.nextPage(DoesMemberHaveNinoPage(srn, index), mode, updatedAnswers)
                updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
                _ <- saveService.save(updatedProgressAnswers)
              } yield Redirect(nextPage)
          )
      )

  }

  private def withMemberDetails(srn: Srn, index: Max300)(
    f: NameDOB => Future[Result]
  )(implicit request: DataRequest[?]): Future[Result] =
    request.userAnswers.get(MemberDetailsPage(srn, index)) match {
      case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(memberDetails) => f(memberDetails)
    }
}

object DoesSchemeMemberHaveNINOController {
  def form(formProvider: YesNoPageFormProvider, memberName: String): Form[Boolean] = formProvider(
    "nationalInsuranceNumber.error.required",
    List(memberName)
  )

  def viewModel(index: Max300, memberName: String, srn: Srn, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    YesNoPageViewModel(
      Message("nationalInsuranceNumber.title"),
      Message("nationalInsuranceNumber.heading", memberName),
      routes.DoesSchemeMemberHaveNINOController.onSubmit(srn, index, mode)
    )
}
