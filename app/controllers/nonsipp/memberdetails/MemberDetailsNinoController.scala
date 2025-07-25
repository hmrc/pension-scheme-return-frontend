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

import utils.RefinedUtils.RefinedIntOps
import services.SaveService
import pages.nonsipp.memberdetails._
import viewmodels.implicits._
import utils.FormUtils._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.Max300
import utils.IntUtils.{toInt, toRefined300}
import controllers.actions._
import forms.TextFormProvider
import models.{Mode, NameDOB}
import controllers.nonsipp.memberdetails.MemberDetailsNinoController._
import views.html.TextInputView
import models.SchemeId.Srn
import navigation.Navigator
import uk.gov.hmrc.domain.Nino
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models.{FormPageViewModel, TextInputViewModel}
import models.requests.DataRequest
import play.api.data.Form

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class MemberDetailsNinoController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  formProvider: TextFormProvider,
  view: TextInputView,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(details: NameDOB, duplicates: List[Nino] = List()) =
    MemberDetailsNinoController.form(formProvider, details.fullName, duplicates)

  def onPageLoad(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) {
    implicit request =>
      request.usingAnswer(MemberDetailsPage(srn, index)).sync { details =>
        Ok(
          view(
            form(details).fromUserAnswers(MemberDetailsNinoPage(srn, index)),
            viewModel(srn, index, mode, details.fullName)
          )
        )
      }
  }

  def onSubmit(srn: Srn, index: Int, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      request.usingAnswer(MemberDetailsPage(srn, index)).async { details =>
        val duplicates = duplicateNinos(srn, index)

        form(details, duplicates)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(
                BadRequest(view(formWithErrors, viewModel(srn, index, mode, details.fullName)))
              ),
            value =>
              for {
                updatedAnswers <- request.userAnswers
                  .set(MemberDetailsNinoPage(srn, index), value)
                  .mapK
                nextPage = navigator.nextPage(MemberDetailsNinoPage(srn, index), mode, updatedAnswers)
                updatedProgressAnswers <- saveProgress(srn, index, updatedAnswers, nextPage)
                _ <- saveService.save(updatedProgressAnswers)
              } yield Redirect(nextPage)
          )
      }
  }

  private def duplicateNinos(srn: Srn, index: Max300)(implicit request: DataRequest[?]): List[Nino] =
    request.userAnswers.map(MemberDetailsNinoPages(srn)).removed(index.arrayIndex.toString).values.toList
}

object MemberDetailsNinoController {
  def form(formProvider: TextFormProvider, memberFullName: String, duplicates: List[Nino]): Form[Nino] =
    formProvider.nino(
      "memberDetailsNino.error.required",
      "memberDetailsNino.error.invalid",
      duplicates,
      "memberDetailsNino.error.duplicate",
      memberFullName
    )

  def viewModel(srn: Srn, index: Max300, mode: Mode, memberFullName: String): FormPageViewModel[TextInputViewModel] =
    FormPageViewModel(
      Message("memberDetailsNino.title"),
      Message("memberDetailsNino.heading", memberFullName),
      TextInputViewModel(),
      routes.MemberDetailsNinoController.onSubmit(srn, index, mode)
    )
}
