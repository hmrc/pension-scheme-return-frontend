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
import pages.{MemberDetailsPage, PensionSchemeMembersPage, RemoveMemberDetailsPage}
import models.SchemeId.Srn
import navigation.Navigator
import play.api.data.Form
import viewmodels.models.YesNoPageViewModel
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.YesNoPageView
import RemoveMemberDetailsController._
import config.Refined.Max99
import models.{Mode, NameDOB}
import models.requests.DataRequest
import play.api.libs.json.JsArray
import services.SaveService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._

import scala.concurrent.{ExecutionContext, Future}

class RemoveMemberDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = RemoveMemberDetailsController.form(formProvider)
  def onPageLoad(srn: Srn, index: Max99, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      withMemberDetails(srn, index)(nameDOB => Ok(view(form, viewModel(srn, index, nameDOB, mode))))
    }
  def onSubmit(srn: Srn, index: Max99, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              withMemberDetails(srn, index)(
                nameDOB => BadRequest(view(formWithErrors, viewModel(srn, index, nameDOB, mode)))
              )
            ),
          removeMemberDetails => {
            if (removeMemberDetails) {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.remove(MemberDetailsPage(srn, index)))
                _ <- saveService.save(updatedAnswers)
              } yield {
                val balanceMembersList = (updatedAnswers.data \ "memberDetailsPage").as[JsArray]
                if (balanceMembersList.value.isEmpty) {
                  Redirect(navigator.nextPage(PensionSchemeMembersPage(srn), mode, updatedAnswers))
                } else {
                  Redirect(navigator.nextPage(RemoveMemberDetailsPage(srn), mode, updatedAnswers))
                }
              }
            } else {
              Future
                .successful(Redirect(navigator.nextPage(RemoveMemberDetailsPage(srn), mode, request.userAnswers)))
            }
          }
        )
    }

  private def withMemberDetails(srn: Srn, index: Max99)(
    f: NameDOB => Result
  )(implicit request: DataRequest[_]): Result =
    request.userAnswers.get(MemberDetailsPage(srn, index)) match {
      case Some(nameDOB) => f(nameDOB)
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
}

object RemoveMemberDetailsController {
  def form(formProvider: YesNoPageFormProvider): Form[Boolean] = formProvider(
    "removeMemberDetails.error.required"
  )

  def viewModel(srn: Srn, index: Max99, nameDOB: NameDOB, mode: Mode): YesNoPageViewModel = YesNoPageViewModel(
    Message("removeMemberDetails.title", nameDOB.fullName),
    Message("removeMemberDetails.heading", nameDOB.fullName),
    controllers.routes.RemoveMemberDetailsController.onSubmit(srn, index, mode)
  )
}
