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

package controllers.nonsipp.memberdetails

import config.Refined.Max99
import controllers.actions._
import controllers.nonsipp.memberdetails.MemberDetailsNinoController._
import forms.TextFormProvider
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{Mode, NameDOB}
import navigation.Navigator
import pages.{MemberDetailsNinoPage, MemberDetailsNinos, MemberDetailsPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.SaveService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FormUtils._
import utils.RefinedUtils.RefinedIntOps
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.TextInputViewModel
import views.html.TextInputView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class MemberDetailsNinoController @Inject()(
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: TextFormProvider,
  view: TextInputView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(details: NameDOB, duplicates: List[Nino] = List()) =
    MemberDetailsNinoController.form(formProvider, details, duplicates)

  def onPageLoad(srn: Srn, index: Max99, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.usingAnswer(MemberDetailsPage(srn, index)).sync { details =>
        Ok(view(form(details).fromUserAnswers(MemberDetailsNinoPage(srn, index)), viewModel(srn, index, mode, details)))
      }
  }

  def onSubmit(srn: Srn, index: Max99, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      request.usingAnswer(MemberDetailsPage(srn, index)).async { details =>
        val duplicates = duplicateNinos(srn, index)

        form(details, duplicates)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(view(formWithErrors, viewModel(srn, index, mode, details)))
              ),
            answer => {
              for {
                updatedAnswers <- Future.fromTry(request.userAnswers.set(MemberDetailsNinoPage(srn, index), answer))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(MemberDetailsNinoPage(srn, index), mode, updatedAnswers))
            }
          )
      }
  }

  private def duplicateNinos(srn: Srn, index: Max99)(implicit request: DataRequest[_]): List[Nino] =
    request.userAnswers.map(MemberDetailsNinos(srn)).removed(index.arrayIndex.toString).values.toList
}

object MemberDetailsNinoController {
  def form(formProvider: TextFormProvider, memberDetails: NameDOB, duplicates: List[Nino]): Form[Nino] =
    formProvider.nino(
      "memberDetailsNino.error.required",
      "memberDetailsNino.error.invalid",
      duplicates,
      "memberDetailsNino.error.duplicate",
      memberDetails.fullName
    )

  def viewModel(srn: Srn, index: Max99, mode: Mode, memberDetails: NameDOB): TextInputViewModel = TextInputViewModel(
    Message("memberDetailsNino.title"),
    Message("memberDetailsNino.heading", memberDetails.fullName),
    None,
    routes.MemberDetailsNinoController.onSubmit(srn, index, mode)
  )
}
