/*
 * Copyright 2025 HM Revenue & Customs
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

package utils.nonsipp.summary

import services.SchemeDateService
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import viewmodels.implicits._
import play.api.mvc._
import cats.implicits.toShow
import viewmodels.models.SummaryPageEntry.{Heading, MessageLine, Section}
import models.requests.DataRequest
import config.RefinedTypes.Max3
import controllers.PsrControllerHelpers
import cats.data.{EitherT, NonEmptyList}
import models.SchemeId.Srn
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.Future

import java.time.LocalDate

object TotalValueQuotedSharesCheckAnswersUtils extends PsrControllerHelpers {

  def sectionEntries(
    srn: Srn,
    mode: Mode,
    viewOnlyViewModel: Option[ViewOnlyViewModel] = None,
    showBackLink: Boolean = false
  )(using
    request: DataRequest[AnyContent],
    schemeDateService: SchemeDateService
  ): EitherT[Future, Result, List[SummaryPageEntry]] =
    EitherT(
      Future.successful(
        for {
          periods <- schemeDateService.taxYearOrAccountingPeriods(srn).getOrRecoverJourney
          totalCost = request.userAnswers.get(TotalValueQuotedSharesPage(srn))
          vm = TotalValueQuotedSharesCheckAnswersUtils.viewModel(
            srn,
            totalCost,
            periods,
            request.schemeDetails,
            mode,
            viewOnlyViewModel,
            showBackLink = showBackLink
          )
          isRecorded = request.userAnswers.get(TotalValueQuotedSharesPage(srn)).exists(_ != Money(0))
          body =
            if (isRecorded) Section(vm.page.toSummaryViewModel())
            else MessageLine(Message("nonsipp.summary.message.noneRecorded"))
        } yield List(
          Heading(Message("nonsipp.summary.quotedShares.heading")),
          body
        )
      )
    )

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
          controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController
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
                controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController
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
          onSubmit = controllers.nonsipp.totalvaluequotedshares.routes.TotalValueQuotedSharesCYAController
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
