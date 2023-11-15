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

package controllers.nonsipp.membercontributions

import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import models.{CheckMode, CheckOrChange, Mode, Money, NormalMode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.membercontributions.CYAMemberContributionsPage
import pages.nonsipp.memberpayments.UnallocatedEmployerAmountPage
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PsrSubmissionService
import viewmodels.DisplayMessage.Message
import viewmodels.implicits._
import viewmodels.models.{
  CheckYourAnswersRowViewModel,
  CheckYourAnswersSection,
  CheckYourAnswersViewModel,
  FormPageViewModel,
  SummaryAction
}
import views.html.CYAWithRemove

import javax.inject.{Inject, Named}
import scala.concurrent.ExecutionContext

class CYAMemberContributionsController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  view: CYAWithRemove
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(
    srn: Srn,
    checkOrChange: CheckOrChange
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      (
        for {

          unallocatedAmount <- Some(Money(123.4)) //TODO request the ACTUAL amount

          schemeName = request.schemeDetails.schemeName
        } yield Ok(
          view(
            CYAMemberContributionsController.viewModel(
              ViewModelParameters(
                srn,
                schemeName,
                unallocatedAmount,
                checkOrChange
              )
            )
          )
        )
      ).get
    }

  def onSubmit(srn: Srn, checkOrChange: CheckOrChange): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService.submitPsrDetails(srn).map {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          Redirect(navigator.nextPage(CYAMemberContributionsPage(srn), NormalMode, request.userAnswers))
      }
    }
}

case class ViewModelParameters(
  srn: Srn,
  schemeName: String,
  unallocatedAmount: Money,
  checkOrChange: CheckOrChange
)
object CYAMemberContributionsController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.checkOrChange
        .fold(check = "MemberContributionCYA.title", change = "MemberContributionCYA.change.title"),
      heading = parameters.checkOrChange.fold(
        check = "MemberContributionCYA.heading",
        change = Message(
          "MemberContributionCYA.change.heading",
          parameters.schemeName
        )
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          parameters.srn,
          parameters.schemeName,
          parameters.unallocatedAmount,
          CheckMode
        )
      ),
      refresh = None,
      buttonText = parameters.checkOrChange.fold(check = "site.saveAndContinue", change = "site.continue"),
      onSubmit = controllers.nonsipp.membercontributions.routes.CYAMemberContributionsController
        .onSubmit(parameters.srn, parameters.checkOrChange)
    )

  private def sections(
    srn: Srn,
    schemeName: String,
    unallocatedAmount: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    checkYourAnswerSection(
      srn,
      schemeName,
      unallocatedAmount,
      mode
    )

  private def checkYourAnswerSection(
    srn: Srn,
    schemeName: String,
    unallocatedAmount: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
        List(
          CheckYourAnswersRowViewModel(
            Message("MemberContributionCYA.section.memberName", schemeName),
            Message("MemberContributionCYA.section.amount", unallocatedAmount.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberpayments.routes.UnallocatedEmployerAmountController
                .onSubmit(srn, mode)
                .url
            ).withVisuallyHiddenContent(Message("MemberContributionCYA.section.hide", schemeName))
          )
        )
      )
    )

}
