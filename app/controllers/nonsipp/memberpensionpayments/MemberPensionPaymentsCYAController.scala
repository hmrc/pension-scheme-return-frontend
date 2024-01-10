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

package controllers.nonsipp.memberpensionpayments

import config.Refined.Max300
import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import controllers.nonsipp.memberpensionpayments.MemberPensionPaymentsCYAController._
import models.SchemeId.Srn
import models.{CheckMode, Mode, Money, NormalMode}
import navigation.Navigator
import pages.nonsipp.memberdetails.MembersDetailsPages.MembersDetailsOps
import pages.nonsipp.memberpensionpayments.{MemberPensionPaymentsCYAPage, TotalAmountPensionPaymentsPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PsrSubmissionService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models._
import views.html.CheckYourAnswersView

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class MemberPensionPaymentsCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    index: Max300,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {
          contribution <- request.userAnswers.get(TotalAmountPensionPaymentsPage(srn, index))
          memberName = request.userAnswers.membersDetails(srn)
        } yield Ok(
          view(
            viewModel(
              ViewModelParameters(
                srn,
                memberName(index.value - 1).fullName,
                index,
                contribution,
                mode
              )
            )
          )
        )
      ).get
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService.submitPsrDetails(srn).map {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          Redirect(navigator.nextPage(MemberPensionPaymentsCYAPage(srn), mode, request.userAnswers))
      }
    }
}

case class ViewModelParameters(
  srn: Srn,
  memberName: String,
  index: Max300,
  contributions: Money,
  mode: Mode
)
object MemberPensionPaymentsCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.mode
        .fold(normal = "MemberPensionPaymentsCYA.title", check = "MemberPensionPaymentsCYA.change.title"),
      heading = parameters.mode.fold(
        normal = "MemberPensionPaymentsCYA.heading",
        check = Message(
          "MemberPensionPaymentsCYA.change.heading",
          parameters.memberName
        )
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          parameters.srn,
          parameters.memberName,
          parameters.index,
          parameters.contributions,
          CheckMode
        )
      ),
      refresh = None,
      buttonText = parameters.mode.fold(normal = "site.saveAndContinue", check = "site.continue"),
      onSubmit = controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
        .onSubmit(parameters.srn, NormalMode)
    )

  private def sections(
    srn: Srn,
    memberName: String,
    index: Max300,
    contributions: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
        List(
          CheckYourAnswersRowViewModel(
            Message("MemberPensionPaymentsCYA.section.memberName.header"),
            Message(memberName)
          ),
          CheckYourAnswersRowViewModel(
            Message("MemberPensionPaymentsCYA.section.memberName", memberName),
            Message("MemberPensionPaymentsCYA.section.amount", contributions.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberpensionpayments.routes.TotalAmountPensionPaymentsController
                .onSubmit(srn, index, mode)
                .url
            ).withVisuallyHiddenContent(Message("MemberPensionPaymentsCYA.section.hide", memberName))
          )
        )
      )
    )

}
