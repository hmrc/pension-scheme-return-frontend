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

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.{MemberDetailsPage, MemberStatus}
import play.api.mvc._
import org.slf4j.LoggerFactory
import controllers.nonsipp.membercontributions.MemberContributionsCYAController._
import controllers.actions.IdentifyAndRequireData
import models._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import viewmodels.implicits._
import pages.nonsipp.membercontributions.{MemberContributionsCYAPage, TotalMemberContributionPage}
import config.RefinedTypes.Max300
import controllers.PSRController
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.{Inject, Named}

class MemberContributionsCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName)

  def onPageLoad(
    srn: Srn,
    index: Max300,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn, index, mode)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Max300,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn, index, mode)
    }

  def onPageLoadCommon(srn: Srn, index: Max300, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    request.userAnswers.get(MemberDetailsPage(srn, index)) match {
      case None =>
        logger.warn(s"Member details for member index $index not found")
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      case Some(memberDetails) =>
        request.userAnswers.get(TotalMemberContributionPage(srn, index)).getOrRecoverJourney { contribution =>
          Ok(
            view(
              viewModel(
                srn,
                memberDetails.fullName,
                index,
                contribution,
                mode,
                viewOnlyUpdated = false, // flag is not displayed on this tier
                optYear = request.year,
                optCurrentVersion = request.currentVersion,
                optPreviousVersion = request.previousVersion,
                compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
              )
            )
          )
        }
    }

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      lazy val memberContributionsChanged =
        request.userAnswers.changed(_.buildMemberContributions(srn, index))

      for {
        updatedAnswers <- request.userAnswers
          .setWhen(memberContributionsChanged)(MemberStatus(srn, index), MemberState.Changed)
          .mapK[Future]
        _ <- saveService.save(updatedAnswers)
        submissionResult <- psrSubmissionService
          .submitPsrDetailsWithUA(
            srn,
            updatedAnswers,
            fallbackCall = controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
              .onPageLoad(srn, index, mode)
          )
      } yield submissionResult.getOrRecoverJourney(
        _ => Redirect(navigator.nextPage(MemberContributionsCYAPage(srn), mode, request.userAnswers))
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.membercontributions.routes.MemberContributionListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}

object MemberContributionsCYAController {
  def viewModel(
    srn: Srn,
    memberName: String,
    index: Max300,
    contributions: Money,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode
        .fold(
          normal = "MemberContributionCYA.title",
          check = "MemberContributionCYA.change.title",
          viewOnly = "MemberContributionCYA.viewOnly.title"
        ),
      heading = mode.fold(
        normal = "MemberContributionCYA.heading",
        check = Message(
          "MemberContributionCYA.change.heading",
          memberName
        ),
        viewOnly = "MemberContributionCYA.viewOnly.heading"
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          memberName,
          index,
          contributions,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
        .onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "MemberContributionCYA.viewOnly.title",
            heading = Message("MemberContributionCYA.viewOnly.heading", memberName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.membercontributions.routes.MemberContributionsCYAController
                  .onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
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
