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

package controllers.nonsipp.memberpayments

import controllers.PSRController
import controllers.actions.IdentifyAndRequireData
import models.{CheckMode, CheckOrChange, Mode, Money, NormalMode}
import models.SchemeId.Srn
import navigation.Navigator
import pages.nonsipp.memberpayments.{UnallocatedContributionCYAPage, UnallocatedEmployerAmountPage}
import play.api.i18n.MessagesApi
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PsrSubmissionService
import viewmodels.DisplayMessage.Message
import viewmodels.models.{
  CheckYourAnswersRowViewModel,
  CheckYourAnswersSection,
  CheckYourAnswersViewModel,
  FormPageViewModel,
  SummaryAction
}
import views.html.CYAWithRemove
import viewmodels.implicits._

import javax.inject.{Inject, Named}
import scala.:+
import scala.concurrent.ExecutionContext

class UnallocatedContributionCYAController @Inject()(
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

          unallocatedAmount <- request.userAnswers.get(UnallocatedEmployerAmountPage(srn)).getOrRecoverJourney

          schemeName = request.schemeDetails.schemeName
        } yield Ok(
          view(
            UnallocatedContributionCYAController.viewModel(
              ViewModelParameters(
                srn,
                schemeName,
                unallocatedAmount,
                checkOrChange
              )
            )
          )
        )
      ).merge
    }

  def onSubmit(srn: Srn, checkOrChange: CheckOrChange): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService.submitPsrDetails(srn).map {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          Redirect(navigator.nextPage(UnallocatedContributionCYAPage(srn), NormalMode, request.userAnswers))
      }
    }
}

case class ViewModelParameters(
  srn: Srn,
  schemeName: String,
  unallocatedAmount: Money,
  checkOrChange: CheckOrChange
)
object UnallocatedContributionCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.checkOrChange
        .fold(check = "unallocatedEmployerCYA.title", change = "unallocatedEmployerCYA.change.title"),
      heading = parameters.checkOrChange.fold(
        check = "unallocatedEmployerCYA.heading",
        change = Message(
          "unallocatedEmployerCYA.change.heading",
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
      onSubmit = controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
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
            Message("unallocatedEmployerCYA.section.schemeName", schemeName),
            Message("unallocatedEmployerCYA.section.amount", unallocatedAmount.displayAs)
          ).with2Actions(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberpayments.routes.UnallocatedEmployerAmountController
                .onSubmit(srn, mode)
                .url
            ).withVisuallyHiddenContent(Message("unallocatedEmployerCYA.section.schemeName.hidden", schemeName)),
            SummaryAction(
              "site.remove",
              controllers.routes.UnauthorisedController.onPageLoad().url //TODO add the link to the Remove Controller
            ).withVisuallyHiddenContent(Message("unallocatedEmployerCYA.section.schemeName.hidden", schemeName))
          )
        )
      )
    )

}
