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

package controllers.nonsipp.totalvaluequotedshares

import services.{PsrSubmissionService, SchemeDateService}
import pages.nonsipp.totalvaluequotedshares._
import viewmodels.implicits._
import play.api.mvc._
import utils.nonsipp.TaskListStatusUtils.getCompletedOrUpdatedTaskListStatus
import controllers.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesCYAController._
import cats.implicits.toShow
import controllers.actions._
import viewmodels.models.TaskListStatus.Updated
import play.api.i18n._
import models.requests.DataRequest
import config.RefinedTypes.Max3
import controllers.PSRController
import cats.data.NonEmptyList
import views.html.CYAWithRemove
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class TotalValueQuotedSharesCYAController @Inject() (
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: CYAWithRemove
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn, NormalMode, showBackLink = true)
    }
  def onPageLoadViewOnly(
    srn: Srn,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, current, previous) { implicit request =>
      val showBackLink = true
      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = request.previousUserAnswers match {
          case Some(previousUserAnswers) =>
            val updated = getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              previousUserAnswers,
              Paths.quotedShares
            ) == Updated
            updated
          case _ => false
        },
        year = year,
        currentVersion = current,
        previousVersion = previous,
        compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
      )
      onPageLoadCommon(srn, ViewOnlyMode, Some(viewOnlyViewModel), showBackLink)
    }

  def onPreviousViewOnly(
    srn: Srn,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] = identifyAndRequireData(srn, ViewOnlyMode, year, (current - 1).max(0), (previous - 1).max(0)) {
    implicit request =>
      val showBackLink = false
      val viewOnlyViewModel = ViewOnlyViewModel(
        viewOnlyUpdated = request.previousUserAnswers match {
          case Some(previousUserAnswers) =>
            val updated = getCompletedOrUpdatedTaskListStatus(
              request.userAnswers,
              previousUserAnswers,
              Paths.quotedShares
            ) == Updated
            updated
          case _ => false
        },
        year = year,
        currentVersion = (current - 1).max(0),
        previousVersion = (previous - 1).max(0),
        compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
      )
      onPageLoadCommon(srn, ViewOnlyMode, Some(viewOnlyViewModel), showBackLink)
  }

  private def onPageLoadCommon(
    srn: Srn,
    mode: Mode,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean
  )(implicit
    request: DataRequest[?]
  ) =
    schemeDateService.taxYearOrAccountingPeriods(srn) match {
      case Some(periods) =>
        val totalCost = request.userAnswers.get(TotalValueQuotedSharesPage(srn))
        Ok(
          view(
            viewModel(
              srn,
              totalCost,
              periods,
              request.schemeDetails,
              mode,
              viewOnlyViewModel,
              showBackLink = showBackLink
            )
          )
        )
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  def onSubmit(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    psrSubmissionService
      .submitPsrDetails(
        srn,
        fallbackCall =
          controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController.onPageLoad(srn)
      )
      .map {
        case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        case Some(_) =>
          Redirect(navigator.nextPage(TotalValueQuotedSharesCYAPage(srn), NormalMode, request.userAnswers))
      }
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, current, previous).async {
      Future.successful(
        Redirect(controllers.nonsipp.routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      )
    }
}

object TotalValueQuotedSharesCYAController {
  def viewModel(
    srn: Srn,
    totalCost: Option[Money],
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    schemeDetails: SchemeDetails,
    mode: Mode,
    viewOnlyViewModel: Option[ViewOnlyViewModel],
    showBackLink: Boolean
  ): FormPageViewModel[CheckYourAnswersViewModel] = {
    val (title, heading) = mode.fold(
      normal = ("totalValueQuotedSharesCYA.normal.title", "totalValueQuotedSharesCYA.normal.heading"),
      check = ("totalValueQuotedSharesCYA.title.change", "totalValueQuotedSharesCYA.heading.change"),
      viewOnly = ("totalValueQuotedSharesCYA.title.view", "totalValueQuotedSharesCYA.heading.view")
    )

    FormPageViewModel[CheckYourAnswersViewModel](
      title = title,
      heading = heading,
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          totalCost,
          taxYearOrAccountingPeriods,
          schemeDetails,
          mode
        )
      ),
      refresh = None,
      buttonText =
        mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.return.to.tasklist"),
      onSubmit = viewOnlyViewModel match {
        case Some(viewOnly) =>
          routes.TotalValueQuotedSharesCYAController
            .onSubmitViewOnly(srn, viewOnly.year, viewOnly.currentVersion, viewOnly.previousVersion)
        case None => controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController.onSubmit(srn)
      },
      mode = mode,
      optViewOnlyDetails = viewOnlyViewModel.map { viewOnly =>
        ViewOnlyDetailsViewModel(
          updated = viewOnly.viewOnlyUpdated,
          link = if (viewOnly.currentVersion > 1 && viewOnly.previousVersion > 0) {
            Some(
              LinkMessage(
                "totalValueQuotedSharesCYA.view.link",
                routes.TotalValueQuotedSharesCYAController
                  .onPreviousViewOnly(
                    srn,
                    viewOnly.year,
                    viewOnly.currentVersion,
                    viewOnly.previousVersion
                  )
                  .url
              )
            )
          } else {
            None
          },
          submittedText = viewOnly.compilationOrSubmissionDate
            .fold(Some(Message("")))(date => Some(Message("site.submittedOn", date.show))),
          title = title,
          heading = heading,
          buttonText = "site.return.to.tasklist",
          onSubmit = routes.TotalValueQuotedSharesCYAController
            .onSubmitViewOnly(srn, viewOnly.year, viewOnly.currentVersion, viewOnly.previousVersion)
        )
      },
      showBackLink = showBackLink
    )
  }

  private def sections(
    srn: Srn,
    totalCost: Option[Money],
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    schemeDetails: SchemeDetails,
    mode: Mode
  ): List[CheckYourAnswersSection] = {
    val row: List[CheckYourAnswersRowViewModel] = (mode, totalCost) match {
      case (ViewOnlyMode, None | Some(Money.zero)) =>
        List(
          CheckYourAnswersRowViewModel(
            Message(
              "quotedSharesManagedFundsHeld.heading",
              schemeDetails.schemeName,
              taxEndDate(taxYearOrAccountingPeriods).show
            ),
            "site.no"
          )
        )
      case (_, Some(totalCost)) =>
        List(
          CheckYourAnswersRowViewModel(
            Message(
              "totalValueQuotedSharesCYA.section.totalCost",
              schemeDetails.schemeName,
              taxEndDate(taxYearOrAccountingPeriods).show
            ),
            "£" + totalCost.displayAs
          )
        )
      case _ => Nil
    }

    List(
      CheckYourAnswersSection(
        None,
        if (mode.isViewOnlyMode) {
          row
        } else {
          row.map(
            _.with2Actions(
              SummaryAction(
                "site.change",
                controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesController
                  .onPageLoad(srn)
                  .url
              ).withVisuallyHiddenContent(
                Message(
                  "totalValueQuotedSharesCYA.section.totalCost.hidden",
                  schemeDetails.schemeName,
                  taxEndDate(taxYearOrAccountingPeriods).show
                )
              ),
              SummaryAction(
                "site.remove",
                controllers.nonsipp.totalvaluequotedshares.routes.RemoveTotalValueQuotedSharesController
                  .onPageLoad(srn, NormalMode)
                  .url
              ).withVisuallyHiddenContent(
                Message(
                  "totalValueQuotedSharesCYA.section.totalCost.hidden",
                  schemeDetails.schemeName,
                  taxEndDate(taxYearOrAccountingPeriods).show
                )
              )
            )
          )
        }
      )
    )
  }

  private def taxEndDate(taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]]): LocalDate =
    taxYearOrAccountingPeriods match {
      case Left(taxYear) => taxYear.to
      case Right(periods) => periods.toList.maxBy(_._1.to)._1.to
    }

}
