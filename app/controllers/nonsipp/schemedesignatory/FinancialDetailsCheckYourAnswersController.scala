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
import utils.nonsipp.TaskListStatusUtils.getFinancialDetailsCompletedOrUpdated
import controllers.actions._
import controllers.nonsipp.schemedesignatory.FinancialDetailsCheckYourAnswersController._
import viewmodels.models.TaskListStatus.Updated
import models.requests.DataRequest
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import cats.implicits.toShow
import controllers.nonsipp.routes
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models._
import play.api.i18n._
import viewmodels.Margin
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.{Inject, Named}

class FinancialDetailsCheckYourAnswersController @Inject() (
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

  def onPageLoadCommon(srn: Srn, mode: Mode, showBackLink: Boolean = true)(implicit
    request: DataRequest[AnyContent]
  ): Result =
    schemeDateService.schemeDate(srn) match {
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
              viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                getFinancialDetailsCompletedOrUpdated(request.userAnswers, request.previousUserAnswers.get) == Updated
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
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
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

object FinancialDetailsCheckYourAnswersController {
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
