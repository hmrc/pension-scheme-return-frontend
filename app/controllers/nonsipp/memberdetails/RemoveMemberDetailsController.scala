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
import pages.nonsipp.memberdetails.{MemberDetailsPage, RemoveMemberDetailsPage}
import viewmodels.implicits._
import play.api.mvc._
import config.Refined.Max300
import controllers.actions._
import navigation.Navigator
import forms.YesNoPageFormProvider
import models.{Mode, NameDOB}
import views.html.YesNoPageView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, YesNoPageViewModel}
import controllers.nonsipp.memberdetails.RemoveMemberDetailsController._
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class RemoveMemberDetailsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: YesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = RemoveMemberDetailsController.form(formProvider)

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      withMemberDetails(srn, index)(nameDOB => Ok(view(form, viewModel(srn, index, nameDOB, mode))))
    }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
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
              } yield Redirect(
                navigator
                  .nextPage(RemoveMemberDetailsPage(srn), mode, updatedAnswers)
              )
            } else {
              Future
                .successful(Redirect(navigator.nextPage(RemoveMemberDetailsPage(srn), mode, request.userAnswers)))
            }
          }
        )
    }

  private def withMemberDetails(srn: Srn, index: Max300)(
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

  def viewModel(srn: Srn, index: Max300, nameDOB: NameDOB, mode: Mode): FormPageViewModel[YesNoPageViewModel] =
    FormPageViewModel(
      Message("removeMemberDetails.title", nameDOB.fullName),
      Message("removeMemberDetails.heading", nameDOB.fullName),
      YesNoPageViewModel(
        hint = Some(Message("removeMemberDetails.hint"))
      ),
      routes.RemoveMemberDetailsController.onSubmit(srn, index, mode)
    )
}
