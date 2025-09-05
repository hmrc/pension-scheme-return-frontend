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

import services._
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import viewmodels.implicits._
import play.api.mvc._
import _root_.config.RefinedTypes.Max3
import utils.ListUtils.ListOps
import controllers.PsrControllerHelpers
import utils.nonsipp.TaskListStatusUtils.getBasicDetailsCompletedOrUpdated
import cats.implicits.toShow
import viewmodels.models.SummaryPageEntry.{Heading, Section}
import pages.nonsipp._
import viewmodels.models.TaskListStatus.Updated
import models.requests.DataRequest
import cats.data.{EitherT, NonEmptyList}
import models.SchemeId.Srn
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models._
import play.api.i18n._
import viewmodels.Margin
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.{LocalDate, LocalDateTime}

type BasicDetailsData = (
  srn: Srn,
  mode: Mode,
  schemeMemberNumbers: SchemeMemberNumbers,
  activeBankAccount: Boolean,
  whyNoBankAccount: Option[String],
  whichTaxYearPage: Option[DateRange],
  taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
  schemeAdminName: String,
  schemeDetails: SchemeDetails,
  pensionSchemeId: PensionSchemeId,
  isPSP: Boolean,
  viewOnlyUpdated: Boolean,
  optYear: Option[String],
  optCurrentVersion: Option[Int],
  optPreviousVersion: Option[Int],
  compilationOrSubmissionDate: Option[LocalDateTime],
  journeyByPassed: Boolean,
  showBackLink: Boolean
)

object BasicDetailsCheckAnswersUtils extends PsrControllerHelpers {

  private def summaryDataT(srn: Srn, mode: Mode)(using
    request: DataRequest[AnyContent],
    schemeDateService: SchemeDateService
  ): EitherT[Future, Result, BasicDetailsData] = EitherT(Future.successful(summaryData(srn, mode)))

  def sectionEntries(srn: Srn, mode: Mode)(using
    request: DataRequest[AnyContent],
    ec: ExecutionContext,
    schemeDateService: SchemeDateService,
    messages: Messages
  ): EitherT[Future, Result, List[SummaryPageEntry]] =
    summaryDataT(srn, mode).map { data =>
      List(
        Heading(Message("nonsipp.summary.basicDetails.heading")),
        Section(viewModel(data).page.toSummaryViewModel(), true)
      )
    }

  def summaryData(srn: Srn, mode: Mode)(using
    request: DataRequest[AnyContent],
    schemeDateService: SchemeDateService
  ): Either[Result, BasicDetailsData] = {
    val compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))

    for {
      periods <- schemeDateService
        .taxYearOrAccountingPeriods(srn)
        .toRight(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
      activeBankAccount <- requiredPage(ActiveBankAccountPage(srn))
      whyNoBankAccount = request.userAnswers.get(WhyNoBankAccountPage(srn))
      whichTaxYearPage = request.userAnswers.get(WhichTaxYearPage(srn))
      userName <- loggedInUserNameOrRedirect
    } yield (
      srn,
      mode,
      schemeMemberNumbers,
      activeBankAccount,
      whyNoBankAccount,
      whichTaxYearPage,
      periods,
      userName,
      request.schemeDetails,
      request.pensionSchemeId,
      request.pensionSchemeId.isPSP,
      if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
        getBasicDetailsCompletedOrUpdated(request.userAnswers, request.previousUserAnswers.get) == Updated
      } else {
        false
      },
      request.year,
      request.currentVersion,
      request.previousVersion,
      compilationOrSubmissionDate,
      false,
      false
    )
  }

  def viewModel(data: BasicDetailsData)(implicit messages: Messages): FormPageViewModel[CheckYourAnswersViewModel] =
    viewModel(
      data.srn,
      data.mode,
      data.schemeMemberNumbers,
      data.activeBankAccount,
      data.whyNoBankAccount,
      data.whichTaxYearPage,
      data.taxYearOrAccountingPeriods,
      data.schemeAdminName,
      data.schemeDetails,
      data.pensionSchemeId,
      data.isPSP,
      data.viewOnlyUpdated,
      data.optYear,
      data.optCurrentVersion,
      data.optPreviousVersion,
      data.compilationOrSubmissionDate,
      data.journeyByPassed,
      data.showBackLink
    )

  def viewModel(
    srn: Srn,
    mode: Mode,
    schemeMemberNumbers: SchemeMemberNumbers,
    activeBankAccount: Boolean,
    whyNoBankAccount: Option[String],
    whichTaxYearPage: Option[DateRange],
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    schemeAdminName: String,
    schemeDetails: SchemeDetails,
    pensionSchemeId: PensionSchemeId,
    isPSP: Boolean,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None,
    journeyByPassed: Boolean,
    showBackLink: Boolean = true
  )(implicit messages: Messages): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = "basicDetailsCheckYourAnswersController.schemeDetails.normal.title",
      heading = "basicDetailsCheckYourAnswersController.schemeDetails.normal.heading",
      description = Some(ParagraphMessage("basicDetailsCheckYourAnswers.paragraph")),
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          },
          activeBankAccount,
          whyNoBankAccount,
          whichTaxYearPage,
          taxYearOrAccountingPeriods,
          schemeMemberNumbers,
          schemeAdminName,
          schemeDetails,
          pensionSchemeId,
          isPSP
        )
      ).withMarginBottom(Margin.Fixed60Bottom),
      refresh = None,
      buttonText = if (journeyByPassed) {
        "site.view.submission.confirmation"
      } else {
        "site.saveAndContinue"
      },
      onSubmit = if (journeyByPassed) {
        controllers.nonsipp.routes.ReturnSubmittedController.onPageLoad(srn)
      } else {
        controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onSubmit(srn)
      },
      showBackLink = showBackLink,
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion))
                  if currentVersion > 1 && previousVersion > 0 =>
                Some(
                  LinkMessage(
                    "basicDetailsCheckYourAnswersController.viewOnly.link",
                    controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
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
            title = "basicDetailsCheckYourAnswersController.viewOnly.title",
            heading = "basicDetailsCheckYourAnswersController.viewOnly.heading",
            buttonText = if (journeyByPassed) {
              "site.view.submission.confirmation"
            } else {
              "site.return.to.tasklist"
            },
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
                  .onSubmit(srn)
            },
            showBackLink = showBackLink
          )
        )
      } else {
        None
      }
    )

  def sections(
    srn: Srn,
    mode: Mode,
    activeBankAccount: Boolean,
    whyNoBankAccount: Option[String],
    whichTaxYearPage: Option[DateRange],
    taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]],
    schemeMemberNumbers: SchemeMemberNumbers,
    schemeAdminName: String,
    schemeDetails: SchemeDetails,
    pensionSchemeId: PensionSchemeId,
    isPSP: Boolean
  )(implicit
    messages: Messages
  ): List[CheckYourAnswersSection] = List(
    CheckYourAnswersSection(
      Some(Heading2.medium("basicDetailsCheckYourAnswersController.schemeDetails.heading")),
      List(
        CheckYourAnswersRowViewModel(
          "basicDetailsCheckYourAnswersController.schemeDetails.schemeName",
          schemeDetails.schemeName
        ).withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          "basicDetailsCheckYourAnswersController.schemeDetails.pstr",
          schemeDetails.pstr
        ).withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          if (isPSP) {
            "basicDetailsCheckYourAnswersController.schemeDetails.schemePractitionerName"
          } else {
            "basicDetailsCheckYourAnswersController.schemeDetails.schemeAdminName"
          },
          schemeAdminName
        ).withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          if (isPSP) {
            "basicDetailsCheckYourAnswersController.schemeDetails.practitionerId"
          } else {
            "basicDetailsCheckYourAnswersController.schemeDetails.adminId"
          },
          pensionSchemeId.value
        ).withOneHalfWidth()
      ) ++
        (taxYearOrAccountingPeriods match {
          case Left(taxYear) =>
            List(
              CheckYourAnswersRowViewModel(
                "basicDetailsCheckYourAnswersController.schemeDetails.taxYear",
                taxYear.show
              ).withChangeAction(
                controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, CheckMode).url,
                hidden = "basicDetailsCheckYourAnswersController.schemeDetails.taxYear.hidden"
              ).withOneHalfWidth()
            )
          case Right(accountingPeriods) =>
            List(
              CheckYourAnswersRowViewModel(
                "basicDetailsCheckYourAnswersController.schemeDetails.taxYear",
                whichTaxYearPage.get.show
              ).withChangeAction(
                controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, CheckMode).url,
                hidden = "basicDetailsCheckYourAnswersController.schemeDetails.taxYear.hidden"
              ).withOneHalfWidth()
            ) ++
              List(
                CheckYourAnswersRowViewModel(
                  Message("basicDetailsCheckYourAnswersController.schemeDetails.accountingPeriod"),
                  accountingPeriods.map(_._1.show).toList.mkString("\n")
                ).withChangeAction(
                  controllers.nonsipp.accountingperiod.routes.AccountingPeriodListController
                    .onPageLoad(srn, CheckMode)
                    .url,
                  hidden = "basicDetailsCheckYourAnswersController.schemeDetails.accountingPeriod.hidden"
                ).withOneHalfWidth()
              )
        })
        :+ CheckYourAnswersRowViewModel(
          Message("basicDetailsCheckYourAnswersController.schemeDetails.bankAccount"),
          if (activeBankAccount: Boolean) "site.yes" else "site.no"
        ).withChangeAction(
          controllers.nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, CheckMode).url,
          hidden = "basicDetailsCheckYourAnswersController.schemeDetails.bankAccount.hidden"
        ).withOneHalfWidth()
        :?+ whyNoBankAccount.map(reason =>
          CheckYourAnswersRowViewModel(
            Message("basicDetailsCheckYourAnswersController.schemeDetails.whyNoBankAccount"),
            reason
          ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, CheckMode).url,
            hidden = "basicDetailsCheckYourAnswersController.schemeDetails.whyNoBankAccount.hidden"
          ).withOneHalfWidth()
        )
    ),
    CheckYourAnswersSection(
      Some(Heading2.medium("basicDetailsCheckYourAnswersController.memberDetails.heading")),
      List(
        CheckYourAnswersRowViewModel(
          Message(
            "basicDetailsCheckYourAnswersController.memberDetails.activeMembers",
            schemeDetails.schemeName,
            taxEndDate(taxYearOrAccountingPeriods).show
          ),
          schemeMemberNumbers.noOfActiveMembers.toString
        ).withChangeAction(
          controllers.nonsipp.schemedesignatory.routes.HowManyMembersController
            .onPageLoad(srn, mode)
            .url + "#activeMembers",
          hidden = Message(
            "basicDetailsCheckYourAnswersController.memberDetails.activeMembers.hidden",
            taxEndDate(taxYearOrAccountingPeriods).show
          )
        ).withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          Message(
            "basicDetailsCheckYourAnswersController.memberDetails.deferredMembers",
            schemeDetails.schemeName,
            taxEndDate(taxYearOrAccountingPeriods).show
          ),
          schemeMemberNumbers.noOfDeferredMembers.toString
        ).withChangeAction(
          controllers.nonsipp.schemedesignatory.routes.HowManyMembersController
            .onPageLoad(srn, mode)
            .url + "#deferredMembers",
          hidden = Message(
            "basicDetailsCheckYourAnswersController.memberDetails.deferredMembers.hidden",
            taxEndDate(taxYearOrAccountingPeriods).show
          )
        ).withOneHalfWidth(),
        CheckYourAnswersRowViewModel(
          Message(
            "basicDetailsCheckYourAnswersController.memberDetails.pensionerMembers",
            schemeDetails.schemeName,
            taxEndDate(taxYearOrAccountingPeriods).show
          ),
          schemeMemberNumbers.noOfPensionerMembers.toString
        ).withChangeAction(
          controllers.nonsipp.schemedesignatory.routes.HowManyMembersController
            .onPageLoad(srn, mode)
            .url + "#pensionerMembers",
          hidden = Message(
            "basicDetailsCheckYourAnswersController.memberDetails.pensionerMembers.hidden",
            taxEndDate(taxYearOrAccountingPeriods).show
          )
        ).withOneHalfWidth()
      )
    )
  )

  def isUserAnswersAlreadySubmittedAndNotModified(
    srn: Srn
  )(implicit request: DataRequest[AnyContent]): Boolean = {
    val isAlreadySubmitted: Boolean = request.userAnswers.get(FbStatus(srn)).exists(_.isSubmitted)
    val isPureAnswerStayUnchanged: Boolean = request.pureUserAnswers.fold(false)(_.data == request.userAnswers.data)
    isAlreadySubmitted && isPureAnswerStayUnchanged
  }

  def taxEndDate(taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]]): LocalDate =
    taxYearOrAccountingPeriods match {
      case Left(taxYear) => taxYear.to
      case Right(periods) => periods.toList.maxBy(_._1.to)._1.to
    }
}
