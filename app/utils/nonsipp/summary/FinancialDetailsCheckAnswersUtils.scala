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
import pages.nonsipp.schemedesignatory._
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils.ListOps
import controllers.PsrControllerHelpers
import utils.nonsipp.TaskListStatusUtils.getFinancialDetailsCompletedOrUpdated
import cats.implicits.toShow
import viewmodels.models.SummaryPageEntry.{Heading, Section}
import pages.nonsipp.CompilationOrSubmissionDatePage
import viewmodels.models.TaskListStatus.Updated
import viewmodels.Margin
import models.requests.DataRequest
import cats.data.EitherT
import models.SchemeId.Srn
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models._
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.Future

import java.time.LocalDateTime

type FinancialDetailsData = (
  srn: Srn,
  mode: Mode,
  howMuchCashPage: Option[MoneyInPeriod],
  valueOfAssetsPage: Option[MoneyInPeriod],
  feesCommissionsWagesSalariesPage: Option[Money],
  schemeDates: DateRange,
  schemeDetails: SchemeDetails,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int],
  compilationOrSubmissionDate: Option[LocalDateTime],
  showBackLink: Boolean
)

object FinancialDetailsCheckAnswersUtils extends PsrControllerHelpers {

  def sectionEntries(srn: Srn, mode: Mode)(using
    request: DataRequest[AnyContent],
    schemeDateService: SchemeDateService
  ): EitherT[Future, Result, List[SummaryPageEntry]] =
    EitherT(
      Future.successful(
        summaryData(srn, mode).map(x =>
          List(
            Heading(Message("nonsipp.summary.financialDetails.heading")),
            Section(viewModel(x).page.toSummaryViewModel())
          )
        )
      )
    )

  def summaryData(srn: Srn, mode: Mode, showBackLink: Boolean = true)(using
    request: DataRequest[AnyContent],
    schemeDateService: SchemeDateService
  ): Either[Result, FinancialDetailsData] =
    for {
      periods <- schemeDateService.schemeDate(srn).getOrRecoverJourney
      howMuchCashPage = request.userAnswers.get(HowMuchCashPage(srn, mode))
      valueOfAssetsPage = request.userAnswers.get(ValueOfAssetsPage(srn, mode))
      feesCommissionsWagesSalariesPage = request.userAnswers.get(FeesCommissionsWagesSalariesPage(srn, mode))
    } yield (
      srn,
      mode,
      howMuchCashPage,
      valueOfAssetsPage,
      feesCommissionsWagesSalariesPage,
      periods,
      request.schemeDetails,
      if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
        getFinancialDetailsCompletedOrUpdated(request.userAnswers, request.previousUserAnswers.get) == Updated
      } else {
        false
      },
      request.year,
      request.currentVersion,
      request.previousVersion,
      request.userAnswers.get(CompilationOrSubmissionDatePage(srn)),
      showBackLink
    )

  def viewModel(data: FinancialDetailsData): FormPageViewModel[CheckYourAnswersViewModel] = viewModel(
    data.srn,
    data.mode,
    data.howMuchCashPage,
    data.valueOfAssetsPage,
    data.feesCommissionsWagesSalariesPage,
    data.schemeDates,
    data.schemeDetails,
    data.viewOnlyUpdated,
    data.optYear,
    data.optCurrentVersion,
    data.optPreviousVersion,
    data.compilationOrSubmissionDate,
    data.showBackLink
  )

  def viewModel(
    srn: Srn,
    mode: Mode,
    howMuchCashPage: Option[MoneyInPeriod],
    valueOfAssetsPage: Option[MoneyInPeriod],
    feesCommissionsWagesSalariesPage: Option[Money],
    schemeDates: DateRange,
    schemeDetails: SchemeDetails,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    showBackLink: Boolean = true
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = "financialDetailsCheckYourAnswersController.title",
      heading = "financialDetailsCheckYourAnswersController.heading",
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          howMuchCashPage,
          valueOfAssetsPage,
          feesCommissionsWagesSalariesPage,
          schemeDates,
          schemeDetails
        )
      ).withMarginBottom(Margin.Fixed60Bottom),
      refresh = None,
      buttonText = "site.saveAndContinue",
      onSubmit =
        controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController.onSubmit(srn, mode),
      optViewOnlyDetails = Option.when(mode == ViewOnlyMode)(
        ViewOnlyDetailsViewModel(
          updated = viewOnlyUpdated,
          link = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion))
                if currentVersion > 1 && previousVersion > 0 =>
              Some(
                LinkMessage(
                  "financialDetailsCheckYourAnswersController.viewOnly.link",
                  controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
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
          title = "financialDetailsCheckYourAnswersController.viewOnly.title",
          heading = "financialDetailsCheckYourAnswersController.viewOnly.heading",
          buttonText = "site.return.to.tasklist",
          onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
            case (Some(year), Some(currentVersion), Some(previousVersion)) =>
              controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
                .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
            case _ =>
              controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
                .onSubmit(srn, mode)
          }
        )
      ),
      showBackLink = showBackLink
    )

  private def sections(
    srn: Srn,
    howMuchCashPage: Option[MoneyInPeriod],
    valueOfAssetsPage: Option[MoneyInPeriod],
    feesCommissionsWagesSalariesPage: Option[Money],
    schemeDates: DateRange,
    schemeDetails: SchemeDetails
  ): List[CheckYourAnswersSection] = List(
    CheckYourAnswersSection(
      None,
      List() :?+ howMuchCashPage.map(howMuchCash =>
        CheckYourAnswersRowViewModel(
          Message(
            "financialDetailsCheckYourAnswersController.totalCashInStartDate",
            schemeDetails.schemeName,
            schemeDates.from.show
          ),
          "£" + howMuchCash.moneyAtStart.displayAs
        ).withChangeAction(
          controllers.nonsipp.schemedesignatory.routes.HowMuchCashController
            .onPageLoad(srn, CheckMode)
            .url + "#taxStartDate",
          hidden = Message(
            "financialDetailsCheckYourAnswersController.totalCashInStartDate.hidden",
            schemeDates.from.show
          )
        ).withOneHalfWidth()
      ) :?+
        howMuchCashPage.map(howMuchCash =>
          CheckYourAnswersRowViewModel(
            Message(
              "financialDetailsCheckYourAnswersController.totalCashInEndDate",
              schemeDetails.schemeName,
              schemeDates.to.show
            ),
            "£" + howMuchCash.moneyAtEnd.displayAs
          ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.HowMuchCashController
              .onPageLoad(srn, CheckMode)
              .url + "#taxEndDate",
            hidden = Message(
              "financialDetailsCheckYourAnswersController.totalCashInEndDate.hidden",
              schemeDates.to.show
            )
          ).withOneHalfWidth()
        ) :?+
        valueOfAssetsPage.map(valueOfAssets =>
          CheckYourAnswersRowViewModel(
            Message(
              "financialDetailsCheckYourAnswersController.valueOfAssetsInStartDate",
              schemeDates.from.show
            ),
            "£" + valueOfAssets.moneyAtStart.displayAs
          ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.ValueOfAssetsController
              .onPageLoad(srn, CheckMode)
              .url + "#taxStartDate",
            hidden = Message(
              "financialDetailsCheckYourAnswersController.valueOfAssetsInStartDate.hidden",
              schemeDates.from.show
            )
          ).withOneHalfWidth()
        ) :?+
        valueOfAssetsPage.map(valueOfAssets =>
          CheckYourAnswersRowViewModel(
            Message(
              "financialDetailsCheckYourAnswersController.valueOfAssetsInEndDate",
              schemeDates.to.show
            ),
            "£" + valueOfAssets.moneyAtEnd.displayAs
          ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.ValueOfAssetsController
              .onPageLoad(srn, CheckMode)
              .url + "#taxEndDate",
            hidden = Message(
              "financialDetailsCheckYourAnswersController.valueOfAssetsInEndDate.hidden",
              schemeDates.to.show
            )
          ).withOneHalfWidth()
        ) :?+
        feesCommissionsWagesSalariesPage.map(feesCommissionsWagesSalaries =>
          CheckYourAnswersRowViewModel(
            Message(
              "financialDetailsCheckYourAnswersController.feeCommissionWagesSalary"
            ),
            "£" + feesCommissionsWagesSalaries.displayAs
          ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.FeesCommissionsWagesSalariesController
              .onPageLoad(srn, CheckMode)
              .url,
            hidden = "financialDetailsCheckYourAnswersController.feeCommissionWagesSalary.hidden"
          ).withOneHalfWidth()
        )
    )
  )
}
