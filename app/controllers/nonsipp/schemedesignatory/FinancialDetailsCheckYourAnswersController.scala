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

package controllers.nonsipp.schemedesignatory

import services.{PsrSubmissionService, SchemeDateService}
import pages.nonsipp.schemedesignatory._
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils.ListOps
import controllers.PSRController
import utils.nonsipp.TaskListStatusUtils.getFinancialDetailsTaskListStatus
import cats.implicits.toShow
import controllers.actions._
import navigation.Navigator
import controllers.nonsipp.schemedesignatory.FinancialDetailsCheckYourAnswersController._
import viewmodels.models.TaskListStatus.Updated
import _root_.config.Refined.Max3
import cats.data.NonEmptyList
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import utils.DateTimeUtils.localDateShow
import models._
import play.api.i18n._
import viewmodels.Margin
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDate
import javax.inject.{Inject, Named}

class FinancialDetailsCheckYourAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  psrSubmissionService: PsrSubmissionService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    schemeDateService.taxYearOrAccountingPeriods(srn) match {
      case Some(periods) =>
        val howMuchCashPage = request.userAnswers.get(HowMuchCashPage(srn, mode))
        val valueOfAssetsPage = request.userAnswers.get(ValueOfAssetsPage(srn, mode))
        val feesCommissionsWagesSalariesPage = request.userAnswers.get(FeesCommissionsWagesSalariesPage(srn, mode))
        Ok(
          view(
            viewModel(
              srn,
              mode,
              howMuchCashPage,
              valueOfAssetsPage,
              feesCommissionsWagesSalariesPage,
              periods,
              request.schemeDetails,
              if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                getFinancialDetailsTaskListStatus(request.userAnswers, request.previousUserAnswers.get) == Updated
              } else {
                false
              }
            )
          )
        )
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    mode match {
      case ViewOnlyMode =>
        //TODO revert mongodb collections to the latest version
        Future.successful(Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))
      case _ =>
        psrSubmissionService
          .submitPsrDetails(
            srn,
            fallbackCall = controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
              .onPageLoad(srn, NormalMode)
          )
          .map {
            case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
            case Some(_) =>
              Redirect(navigator.nextPage(FinancialDetailsCheckYourAnswersPage(srn), mode, request.userAnswers))
          }
    }
  }

  def onPrevious(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    // TODO: shift mongo collections ()
    // and redirect to onPageLoad of this same page
    Future.successful(
      Redirect(
        controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
          .onPageLoad(srn, ViewOnlyMode)
      )
    )
  }
}

object FinancialDetailsCheckYourAnswersController {
  def viewModel(
    srn: Srn,
    mode: Mode,
    howMuchCashPage: Option[MoneyInPeriod],
    valueOfAssetsPage: Option[MoneyInPeriod],
    feesCommissionsWagesSalariesPage: Option[Money],
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    schemeDetails: SchemeDetails,
    viewOnlyUpdated: Boolean
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = "financialDetailsCheckYourAnswersController.title",
      heading = "financialDetailsCheckYourAnswersController.heading",
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          mode,
          howMuchCashPage,
          valueOfAssetsPage,
          feesCommissionsWagesSalariesPage,
          taxYearOrAccountingPeriods,
          schemeDetails
        )
      ).withMarginBottom(Margin.Fixed60Bottom),
      refresh = None,
      buttonText = "site.saveAndContinue",
      onSubmit =
        controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController.onSubmit(srn, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = Some(
              LinkMessage(
                "financialDetailsCheckYourAnswersController.viewOnly.link",
                controllers.nonsipp.schemedesignatory.routes.FinancialDetailsCheckYourAnswersController
                  .onPrevious(srn)
                  .url
              )
            ),
            submittedText = Some("Submitted on 26 July 2022"), // TODO
            title = "financialDetailsCheckYourAnswersController.viewOnly.title",
            heading = "financialDetailsCheckYourAnswersController.viewOnly.heading",
            buttonText = "site.continue"
          )
        )
      } else {
        None
      }
    )

  private def sections(
    srn: Srn,
    mode: Mode,
    howMuchCashPage: Option[MoneyInPeriod],
    valueOfAssetsPage: Option[MoneyInPeriod],
    feesCommissionsWagesSalariesPage: Option[Money],
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    schemeDetails: SchemeDetails
  ): List[CheckYourAnswersSection] = List(
    CheckYourAnswersSection(
      None,
      List() :?+ howMuchCashPage.map(
        howMuchCash =>
          CheckYourAnswersRowViewModel(
            Message(
              "financialDetailsCheckYourAnswersController.totalCashInStartDate",
              schemeDetails.schemeName,
              taxStartDate(taxYearOrAccountingPeriods).show
            ),
            "£" + howMuchCash.moneyAtStart.displayAs
          ).withChangeAction(
              controllers.nonsipp.schemedesignatory.routes.HowMuchCashController
                .onPageLoad(srn, CheckMode)
                .url + "#taxStartDate",
              hidden = Message(
                "financialDetailsCheckYourAnswersController.totalCashInStartDate.hidden",
                taxStartDate(taxYearOrAccountingPeriods).show
              )
            )
            .withOneHalfWidth()
      ) :?+
        howMuchCashPage.map(
          howMuchCash =>
            CheckYourAnswersRowViewModel(
              Message(
                "financialDetailsCheckYourAnswersController.totalCashInEndDate",
                schemeDetails.schemeName,
                taxEndDate(taxYearOrAccountingPeriods).show
              ),
              "£" + howMuchCash.moneyAtEnd.displayAs
            ).withChangeAction(
                controllers.nonsipp.schemedesignatory.routes.HowMuchCashController
                  .onPageLoad(srn, CheckMode)
                  .url + "#taxEndDate",
                hidden = Message(
                  "financialDetailsCheckYourAnswersController.totalCashInEndDate.hidden",
                  taxEndDate(taxYearOrAccountingPeriods).show
                )
              )
              .withOneHalfWidth()
        ) :?+
        valueOfAssetsPage.map(
          valueOfAssets =>
            CheckYourAnswersRowViewModel(
              Message(
                "financialDetailsCheckYourAnswersController.valueOfAssetsInStartDate",
                schemeDetails.schemeName,
                taxStartDate(taxYearOrAccountingPeriods).show
              ),
              "£" + valueOfAssets.moneyAtStart.displayAs
            ).withChangeAction(
                controllers.nonsipp.schemedesignatory.routes.ValueOfAssetsController
                  .onPageLoad(srn, CheckMode)
                  .url + "#taxStartDate",
                hidden = Message(
                  "financialDetailsCheckYourAnswersController.valueOfAssetsInStartDate.hidden",
                  taxStartDate(taxYearOrAccountingPeriods).show
                )
              )
              .withOneHalfWidth()
        ) :?+
        valueOfAssetsPage.map(
          valueOfAssets =>
            CheckYourAnswersRowViewModel(
              Message(
                "financialDetailsCheckYourAnswersController.valueOfAssetsInEndDate",
                schemeDetails.schemeName,
                taxEndDate(taxYearOrAccountingPeriods).show
              ),
              "£" + valueOfAssets.moneyAtEnd.displayAs
            ).withChangeAction(
                controllers.nonsipp.schemedesignatory.routes.ValueOfAssetsController
                  .onPageLoad(srn, CheckMode)
                  .url + "#taxEndDate",
                hidden = Message(
                  "financialDetailsCheckYourAnswersController.valueOfAssetsInEndDate.hidden",
                  taxEndDate(taxYearOrAccountingPeriods).show
                )
              )
              .withOneHalfWidth()
        ) :?+
        feesCommissionsWagesSalariesPage.map(
          feesCommissionsWagesSalaries =>
            CheckYourAnswersRowViewModel(
              Message(
                "financialDetailsCheckYourAnswersController.feeCommissionWagesSalary",
                schemeDetails.schemeName,
                taxEndDate(taxYearOrAccountingPeriods).show
              ),
              "£" + feesCommissionsWagesSalaries.displayAs
            ).withChangeAction(
                controllers.nonsipp.schemedesignatory.routes.FeesCommissionsWagesSalariesController
                  .onPageLoad(srn, CheckMode)
                  .url,
                hidden = "financialDetailsCheckYourAnswersController.feeCommissionWagesSalary.hidden"
              )
              .withOneHalfWidth()
        )
    )
  )

  private def taxEndDate(taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]]): LocalDate =
    taxYearOrAccountingPeriods match {
      case Left(taxYear) => taxYear.to
      case Right(periods) => periods.toList.maxBy(_._1.to)._1.to
    }

  private def taxStartDate(taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]]): LocalDate =
    taxYearOrAccountingPeriods match {
      case Left(taxYear) => taxYear.from
      case Right(periods) => periods.toList.maxBy(_._1.from)._1.from
    }
}
