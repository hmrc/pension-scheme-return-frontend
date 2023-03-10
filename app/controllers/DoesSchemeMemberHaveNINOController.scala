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
import forms.YesNoPageFormProvider

import javax.inject.Inject
import models.{Mode, NameDOB}
import models.SchemeId.Srn
import navigation.Navigator
import pages.{MemberDetailsPage, NationalInsuranceNumberPage}
import play.api.data.Form
import viewmodels.models.YesNoPageViewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YesNoPageView
import DoesSchemeMemberHaveNINOController._
import models.requests.DataRequest
import services.SaveService
import viewmodels.DisplayMessage.SimpleMessage

import scala.concurrent.{ExecutionContext, Future}
import utils.FormUtils._

class DoesSchemeMemberHaveNINOController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   saveService: SaveService,
                                                   navigator: Navigator,
                                                   identifyAndRequireData: IdentifyAndRequireData,
                                                   formProvider: YesNoPageFormProvider,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: YesNoPageView
                                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  private def form(memberName: String): Form[Boolean] = DoesSchemeMemberHaveNINOController.form(formProvider, memberName)

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      withMemberDetails(srn)(memberDetails => Future.successful(
        Ok(view(form(memberDetails.fullName).fromUserAnswers(NationalInsuranceNumberPage(srn)), viewModel(memberDetails.fullName, srn, mode)))
      ))
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      withMemberDetails(srn)(memberDetails =>
        form(memberDetails.fullName).bindFromRequest().fold(
          formWithErrors => Future.successful(
            BadRequest(view(formWithErrors, viewModel(memberDetails.fullName, srn, mode)))
          ),
          value =>
            for {
              updatedAnswers <- Future.fromTry(request.userAnswers.set(NationalInsuranceNumberPage(srn), value))
              _ <- saveService.save(updatedAnswers)
            } yield Redirect(navigator.nextPage(NationalInsuranceNumberPage(srn), mode, updatedAnswers))
        )
      )
  }

  private def withMemberDetails(srn: Srn)(f: NameDOB => Future[Result])(implicit request: DataRequest[_]): Future[Result] =
    request.userAnswers.get(MemberDetailsPage(srn)) match {
      case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(memberDetails) => f(memberDetails)
    }
}

object DoesSchemeMemberHaveNINOController {
  def form(formProvider: YesNoPageFormProvider, memberName: String): Form[Boolean] = formProvider(
    "nationalInsuranceNumber.error.required", List(memberName)
  )

  def viewModel(memberName: String, srn: Srn, mode: Mode): YesNoPageViewModel = YesNoPageViewModel(
    SimpleMessage("nationalInsuranceNumber.title", memberName),
    SimpleMessage("nationalInsuranceNumber.heading", memberName),
    controllers.routes.DoesSchemeMemberHaveNINOController.onSubmit(srn, mode)
  )
}