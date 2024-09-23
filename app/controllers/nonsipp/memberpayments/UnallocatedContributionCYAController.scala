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

package controllers.nonsipp.memberpayments

import services.PsrSubmissionService
import viewmodels.implicits._
import play.api.mvc._
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import controllers.actions.IdentifyAndRequireData
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import views.html.CYAWithRemove
import models.SchemeId.Srn
import cats.implicits.toShow
import controllers.nonsipp.routes
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.localDateTimeShow
import models._
import pages.nonsipp.memberpayments.{UnallocatedContributionCYAPage, UnallocatedEmployerAmountPage}
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.{Inject, Named}

class UnallocatedContributionCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  view: CYAWithRemove
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    onPageLoadCommon(srn, mode)
  }

  def onPageLoadViewOnly(
    srn: Srn,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      val showBackLink = true
      onPageLoadCommon(srn, mode, showBackLink)
    }

  def onPageLoadCommon(srn: Srn, mode: Mode, showBackLink: Boolean = true)(
    implicit request: DataRequest[AnyContent]
  ): Result =
    Ok(
      view(
        UnallocatedContributionCYAController.viewModel(
          srn,
          request.schemeDetails.schemeName,
          request.userAnswers.get(UnallocatedEmployerAmountPage(srn)),
          mode,
          viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
            getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              request.previousUserAnswers.get,
              pages.nonsipp.memberpayments.Paths.membersPayments \ "unallocatedContribAmount"
            ) == Updated
          } else {
            false
          },
          optYear = request.year,
          optCurrentVersion = request.currentVersion,
          optPreviousVersion = request.previousVersion,
          compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn)),
          showBackLink = showBackLink
        )
      )
    )

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService
        .submitPsrDetails(
          srn,
          fallbackCall = controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
            .onPageLoad(srn, mode)
        )
        .map {
          case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case Some(_) =>
            Redirect(navigator.nextPage(UnallocatedContributionCYAPage(srn), NormalMode, request.userAnswers))
        }
    }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, current, previous).async {
      Future.successful(Redirect(routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous)))
    }

  def onPreviousViewOnly(
    srn: Srn,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)) { implicit request =>
      val showBackLink = false
      onPageLoadCommon(srn, ViewOnlyMode, showBackLink)
    }

}

object UnallocatedContributionCYAController {
  def viewModel(
    srn: Srn,
    schemeName: String,
    unallocatedAmount: Option[Money],
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    showBackLink: Boolean = true
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode
        .fold(
          normal = "unallocatedEmployerCYA.title",
          check = "unallocatedEmployerCYA.change.title",
          viewOnly = "unallocatedEmployerCYA.viewOnly.title"
        ),
      heading = mode.fold(
        normal = "unallocatedEmployerCYA.heading",
        check = Message(
          "unallocatedEmployerCYA.change.heading",
          schemeName
        ),
        viewOnly = "unallocatedEmployerCYA.viewOnly.heading"
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          schemeName,
          unallocatedAmount,
          mode
        )
      ),
      refresh = None,
      buttonText =
        mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.return.to.tasklist"),
      onSubmit = controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
        .onSubmit(srn, mode),
      optViewOnlyDetails = Option.when(mode == ViewOnlyMode)(
        ViewOnlyDetailsViewModel(
          updated = viewOnlyUpdated,
          link = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion))
                if currentVersion > 1 && previousVersion > 0 =>
              Some(
                LinkMessage(
                  "unallocatedEmployerCYA.viewOnly.link",
                  controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
                    .onPreviousViewOnly(
                      srn,
                      year,
                      currentVersion,
                      previousVersion
                    )
                    .url
                )
              )
            case _ => None
          },
          submittedText =
            compilationOrSubmissionDate.fold(Some(Message("")))(date => Some(Message("site.submittedOn", date.show))),
          title = "unallocatedEmployerCYA.viewOnly.title",
          heading = "unallocatedEmployerCYA.viewOnly.heading",
          buttonText = "site.return.to.tasklist",
          onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion)) =>
              controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
                .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
            case _ =>
              controllers.nonsipp.memberpayments.routes.UnallocatedContributionCYAController
                .onSubmit(srn, mode)
          }
        )
      ),
      showBackLink = showBackLink
    )

  private def sections(
    srn: Srn,
    schemeName: String,
    unallocatedAmount: Option[Money],
    mode: Mode
  ): List[CheckYourAnswersSection] = {
    val rows = (unallocatedAmount, mode) match {
      case (None, ViewOnlyMode) =>
        List(
          CheckYourAnswersRowViewModel(
            Message("unallocatedEmployerCYA.section.view.none", schemeName),
            Message("site.no")
          )
        )
      case (Some(amount), mode) =>
        val newMode = mode match {
          case ViewOnlyMode => NormalMode
          case _ => mode
        }
        List(
          CheckYourAnswersRowViewModel(
            Message("unallocatedEmployerCYA.section.schemeName", schemeName),
            Message("unallocatedEmployerCYA.section.amount", amount.displayAs)
          ).with2Actions(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberpayments.routes.UnallocatedEmployerAmountController
                .onSubmit(srn, newMode)
                .url
            ).withVisuallyHiddenContent(Message("unallocatedEmployerCYA.section.hide", schemeName)),
            SummaryAction(
              "site.remove",
              controllers.nonsipp.memberpayments.routes.RemoveUnallocatedAmountController.onSubmit(srn, newMode).url
            ).withVisuallyHiddenContent(Message("unallocatedEmployerCYA.section.hide", schemeName))
          )
        )
      case _ => Nil
    }

    List(CheckYourAnswersSection(heading = None, rows))
  }
}
