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

import config.Refined.Max300
import controllers.actions._
import controllers.nonsipp.memberdetails.DoesSchemeMemberHaveNINOController._
import forms.YesNoPageFormProvider
import forms.mappings.Mappings
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{ConditionalYesNo, Mode, NameDOB}
import navigation.Navigator
import pages.nonsipp.memberdetails.{DoesMemberHaveNinoPage, DoesMemberHaveNinoPages, MemberDetailsPage}
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SaveService
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.RefinedUtils.RefinedIntOps
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{ConditionalYesNoPageViewModel, FieldType, FormPageViewModel, YesNoViewModel}
import views.html.ConditionalYesNoPageView

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}

class DoesSchemeMemberHaveNINOController @Inject()(
  override val messagesApi: MessagesApi,
  saveService: SaveService,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  formProvider: YesNoPageFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ConditionalYesNoPageView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private def form(memberName: String, duplicates: List[Nino] = List()): Form[Either[String, Nino]] =
    DoesSchemeMemberHaveNINOController.form(formProvider, memberName, duplicates)

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      withMemberDetails(srn, index) { memberDetails =>
        val preparedForm =
          request.userAnswers.fillForm(DoesMemberHaveNinoPage(srn, index), form(memberDetails.fullName))
        Future.successful(Ok(view(preparedForm, viewModel(srn, index, memberDetails.fullName, mode))))
      }
  }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async {
    implicit request =>
      withMemberDetails(srn, index) { memberDetails =>
        val duplicates = duplicateNinos(srn, index)

        println(duplicates)

        form(memberDetails.fullName, duplicates)
          .bindFromRequest()
          .fold(
            formWithErrors =>
              Future.successful(BadRequest(view(formWithErrors, viewModel(srn, index, memberDetails.fullName, mode)))),
            value =>
              for {
                updatedAnswers <- Future
                  .fromTry(request.userAnswers.set(DoesMemberHaveNinoPage(srn, index), ConditionalYesNo(value)))
                _ <- saveService.save(updatedAnswers)
              } yield Redirect(navigator.nextPage(DoesMemberHaveNinoPage(srn, index), mode, updatedAnswers))
          )
      }
  }

  private def duplicateNinos(srn: Srn, index: Max300)(implicit request: DataRequest[_]): List[Nino] =
    request.userAnswers.map(DoesMemberHaveNinoPages(srn)).removed(index.arrayIndex.toString).values.toList.flatMap {
      case ConditionalYesNo(Left(_)) => Nil
      case ConditionalYesNo(Right(nino)) => List(nino)
    }

  private def withMemberDetails(srn: Srn, index: Max300)(
    f: NameDOB => Future[Result]
  )(implicit request: DataRequest[_]): Future[Result] =
    request.userAnswers.get(MemberDetailsPage(srn, index)) match {
      case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(memberDetails) => f(memberDetails)
    }
}

object DoesSchemeMemberHaveNINOController {
  def form(
    formProvider: YesNoPageFormProvider,
    memberName: String,
    duplicates: List[Nino]
  ): Form[Either[String, Nino]] =
    formProvider.conditional(
      "nationalInsuranceNumber.error.required",
      mappingNo = Mappings.textArea(
        "nationalInsuranceNumber.no.conditional.error.required",
        "nationalInsuranceNumber.no.conditional.error.invalid",
        "nationalInsuranceNumber.no.conditional.error.length",
        memberName
      ),
      mappingYes = Mappings.ninoNoDuplicates(
        "nationalInsuranceNumber.yes.conditional.error.required",
        "nationalInsuranceNumber.yes.conditional.error.invalid",
        duplicates,
        "nationalInsuranceNumber.yes.conditional.error.duplicate",
        memberName
      ),
      memberName
    )

  def viewModel(
    srn: Srn,
    index: Max300,
    memberName: String,
    mode: Mode
  ): FormPageViewModel[ConditionalYesNoPageViewModel] =
    FormPageViewModel[ConditionalYesNoPageViewModel](
      "nationalInsuranceNumber.title",
      Message("nationalInsuranceNumber.heading", memberName),
      ConditionalYesNoPageViewModel(
        yes = YesNoViewModel
          .Conditional(Message("nationalInsuranceNumber.yes.conditional", memberName), FieldType.Input),
        no = YesNoViewModel
          .Conditional(Message("nationalInsuranceNumber.no.conditional", memberName), FieldType.Textarea)
      ),
      routes.DoesSchemeMemberHaveNINOController.onSubmit(srn, index, mode)
    )
}
