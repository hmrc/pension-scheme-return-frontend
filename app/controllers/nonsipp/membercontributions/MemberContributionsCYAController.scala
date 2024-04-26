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

package controllers.nonsipp.membercontributions

import services.PsrSubmissionService
import pages.nonsipp.memberdetails.MembersDetailsPages
import play.api.mvc._
import config.Refined.Max300
import controllers.PSRController
import controllers.nonsipp.membercontributions.MemberContributionsCYAController._
import controllers.actions.IdentifyAndRequireData
import navigation.Navigator
import models._
import play.api.i18n.MessagesApi
import viewmodels.implicits._
import pages.nonsipp.membercontributions.{MemberContributionsCYAPage, TotalMemberContributionPage}
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.ExecutionContext
import scala.util.Left

import javax.inject.{Inject, Named}

class MemberContributionsCYAController @Inject()(
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
          contribution <- request.userAnswers.get(TotalMemberContributionPage(srn, index))
          memberMap = request.userAnswers.map(MembersDetailsPages(srn))
          maxIndex: Either[Result, Int] = memberMap.keys
            .map(_.toInt)
            .maxOption
            .map(Right(_))
            .getOrElse(Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))

          optionList: List[Option[NameDOB]] = maxIndex match {
            case Right(index) =>
              (0 to index).toList.map { index =>
                val memberOption = memberMap.get(index.toString)
                memberOption match {
                  case Some(member) => Some(member)
                  case None => None
                }
              }
            case Left(_) => List.empty
          }

        } yield optionList(index.value - 1)
          .map(_.fullName)
          .getOrRecoverJourney
          .map(
            memberName =>
              Ok(
                view(
                  viewModel(
                    ViewModelParameters(
                      srn,
                      memberName,
                      index,
                      contribution,
                      mode
                    )
                  )
                )
              )
          )
          .merge
      ).get
    }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService
        .submitPsrDetails(
          srn,
          optFallbackCall = Some(
            controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController.onPageLoad(srn, index, mode)
          )
        )
        .map {
          case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case Some(_) =>
            Redirect(navigator.nextPage(MemberContributionsCYAPage(srn), mode, request.userAnswers))
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
object MemberContributionsCYAController {
  def viewModel(parameters: ViewModelParameters): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      title = parameters.mode
        .fold(normal = "MemberContributionCYA.title", check = "MemberContributionCYA.change.title"),
      heading = parameters.mode.fold(
        normal = "MemberContributionCYA.heading",
        check = Message(
          "MemberContributionCYA.change.heading",
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
      onSubmit = controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
        .onSubmit(parameters.srn, parameters.index, parameters.mode)
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
            Message("MemberContributionCYA.section.memberName.header"),
            Message(memberName)
          ),
          CheckYourAnswersRowViewModel(
            Message("MemberContributionCYA.section.memberName", memberName),
            Message("MemberContributionCYA.section.amount", contributions.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.membercontributions.routes.TotalMemberContributionController
                .onSubmit(srn, index, mode)
                .url
            ).withVisuallyHiddenContent(Message("MemberContributionCYA.section.hide", memberName))
          )
        )
      )
    )

}
