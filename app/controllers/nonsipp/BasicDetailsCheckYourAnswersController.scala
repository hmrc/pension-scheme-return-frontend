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

package controllers.nonsipp

import services._
import play.api.mvc._
import utils.ListUtils.ListOps
import controllers.{nonsipp, PSRController}
import utils.nonsipp.TaskListStatusUtils.getBasicDetailsCompletedOrUpdated
import cats.implicits.{toShow, toTraverseOps}
import controllers.actions._
import controllers.nonsipp.BasicDetailsCheckYourAnswersController._
import _root_.config.Constants._
import utils.nonsipp.SchemeDetailNavigationUtils
import viewmodels.models.TaskListStatus.Updated
import models.requests.DataRequest
import _root_.config.Refined.Max3
import models.audit.PSRStartAuditEvent
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import viewmodels.implicits._
import utils.nonsipp.MemberCountUtils.hasMemberNumbersChangedToOver99
import cats.data.{EitherT, NonEmptyList}
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp._
import navigation.Navigator
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models._
import play.api.i18n._
import viewmodels.Margin
import viewmodels.DisplayMessage._
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.{LocalDate, LocalDateTime}
import javax.inject.{Inject, Named}

class BasicDetailsCheckYourAnswersController @Inject()(
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  auditService: AuditService,
  val psrVersionsService: PsrVersionsService,
  val psrRetrievalService: PsrRetrievalService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController
    with SchemeDetailNavigationUtils {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    onPageLoadCommon(srn, mode)
  }

  def onPageLoadViewOnly(srn: Srn, mode: Mode, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous).async { implicit request =>
      onPageLoadCommon(srn, mode)
    }

  def onPageLoadCommon(srn: Srn, mode: Mode)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val journeyByPassedF =
      if (isUserAnswersAlreadySubmittedAndNotModified(srn)) isJourneyBypassed(srn) else Future.successful(Right(false))
    journeyByPassedF.map(
      eitherJourneyNavigationResultOrRecovery =>
        schemeDateService.taxYearOrAccountingPeriods(srn) match {
          case Some(periods) =>
            val currentUserAnswers = request.userAnswers
            (
              for {
                schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
                activeBankAccount <- requiredPage(ActiveBankAccountPage(srn))
                whyNoBankAccount = currentUserAnswers.get(WhyNoBankAccountPage(srn))
                whichTaxYearPage = currentUserAnswers.get(WhichTaxYearPage(srn))
                userName <- loggedInUserNameOrRedirect
                journeyByPassed <- eitherJourneyNavigationResultOrRecovery
              } yield {
                val compilationOrSubmissionDate = currentUserAnswers.get(CompilationOrSubmissionDatePage(srn))
                val result = Ok(
                  view(
                    viewModel(
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
                      viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                        getBasicDetailsCompletedOrUpdated(currentUserAnswers, request.previousUserAnswers.get) == Updated
                      } else {
                        false
                      },
                      optYear = request.year,
                      optCurrentVersion = request.currentVersion,
                      optPreviousVersion = request.previousVersion,
                      compilationOrSubmissionDate = compilationOrSubmissionDate,
                      journeyByPassed = journeyByPassed
                    )
                  )
                )
                if (journeyByPassed) {
                  result
                    .addingToSession((RETURN_PERIODS, schemeDateService.returnPeriodsAsJsonString(srn)))
                    .addingToSession(
                      (SUBMISSION_DATE, schemeDateService.submissionDateAsString(compilationOrSubmissionDate.get))
                    )
                } else {
                  result
                }
              }
            ).merge
          case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
        }
    )
  }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    if (hasMemberNumbersChangedToOver99(request.userAnswers, srn, request.pensionSchemeId)) {
      auditAndRedirect(srn)(implicitly)
    } else {
      psrSubmissionService
        .submitPsrDetails(srn, fallbackCall = controllers.nonsipp.routes.TaskListController.onPageLoad(srn))
        .map {
          case None =>
            Future(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          case Some(_) =>
            auditAndRedirect(srn)(implicitly)
        }
        .flatten
    }
  }

  private def auditAndRedirect(srn: Srn)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val eitherResultOrFutureResult: Either[Result, Future[Result]] =
      for {
        taxYear <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney
        schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
        userName <- loggedInUserNameOrRedirect
        _ = auditService.sendEvent(buildAuditEvent(taxYear, schemeMemberNumbers, userName))
      } yield {
        // Determine next page in case of Declaration redirect
        val byPassedJourney = if (request.pensionSchemeId.isPSP) {
          nonsipp.declaration.routes.PspDeclarationController.onPageLoad(srn)
        } else {
          nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn)
        }
        val regularJourney = navigator
          .nextPage(BasicDetailsCheckYourAnswersPage(srn), NormalMode, request.userAnswers)
        isJourneyBypassed(srn)
          .map(res => res.map(if (_) Redirect(byPassedJourney) else Redirect(regularJourney)).merge)
      }

    // Transform Either[Result, Future[Result]] into a Future[Result]
    EitherT(eitherResultOrFutureResult.sequence).merge
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn, ViewOnlyMode, year, current, previous).async { implicit request =>
      val byPassedJourney = Redirect(routes.ReturnSubmittedController.onPageLoad(srn))
      val regularJourney = Redirect(routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous))
      isJourneyBypassed(srn).map(res => res.map(if (_) byPassedJourney else regularJourney).merge)
    }

  def onPreviousViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] = {
    val newCurrent = (current - 1).max(0)
    val newPrevious = (previous - 1).max(0)
    identifyAndRequireData(srn, ViewOnlyMode, year, newCurrent, newPrevious).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
            .onPageLoadViewOnly(srn, year, newCurrent, newPrevious)
        )
      )
    }
  }

  private def buildAuditEvent(taxYear: DateRange, schemeMemberNumbers: SchemeMemberNumbers, userName: String)(
    implicit req: DataRequest[_]
  ) = PSRStartAuditEvent(
    schemeName = req.schemeDetails.schemeName,
    req.schemeDetails.establishers.headOption.fold(userName)(e => e.name),
    psaOrPspId = req.pensionSchemeId.value,
    schemeTaxReference = req.schemeDetails.pstr,
    affinityGroup = if (req.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual",
    credentialRole = if (req.pensionSchemeId.isPSP) PSP else PSA,
    taxYear = taxYear,
    howManyMembers = schemeMemberNumbers.noOfActiveMembers,
    howManyDeferredMembers = schemeMemberNumbers.noOfDeferredMembers,
    howManyPensionerMembers = schemeMemberNumbers.noOfPensionerMembers
  )
}

object BasicDetailsCheckYourAnswersController {
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
    journeyByPassed: Boolean
  )(implicit messages: Messages): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = "checkYourAnswers.title",
      heading = "checkYourAnswers.heading",
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
        routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, mode)
      },
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
                  .onSubmit(srn, mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def sections(
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
  )(
    implicit messages: Messages
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
                  controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, CheckMode).url
                )
                .withOneHalfWidth()
            )
          case Right(accountingPeriods) =>
            List(
              CheckYourAnswersRowViewModel(
                "basicDetailsCheckYourAnswersController.schemeDetails.taxYear",
                whichTaxYearPage.get.show
              ).withChangeAction(
                  controllers.nonsipp.routes.CheckReturnDatesController.onPageLoad(srn, CheckMode).url
                )
                .withOneHalfWidth()
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
                  )
                  .withOneHalfWidth()
              )
        })
        :+ CheckYourAnswersRowViewModel(
          Message("basicDetailsCheckYourAnswersController.schemeDetails.bankAccount", schemeDetails.schemeName),
          if (activeBankAccount: Boolean) "site.yes" else "site.no"
        ).withChangeAction(
            controllers.nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, CheckMode).url,
            hidden = "basicDetailsCheckYourAnswersController.schemeDetails.bankAccount.hidden"
          )
          .withOneHalfWidth()
        :?+ whyNoBankAccount.map(
          reason =>
            CheckYourAnswersRowViewModel(
              Message(
                "basicDetailsCheckYourAnswersController.schemeDetails.whyNoBankAccount",
                schemeDetails.schemeName
              ),
              reason
            ).withChangeAction(
                controllers.nonsipp.schemedesignatory.routes.WhyNoBankAccountController.onPageLoad(srn, CheckMode).url,
                hidden = "basicDetailsCheckYourAnswersController.schemeDetails.whyNoBankAccount.hidden"
              )
              .withOneHalfWidth()
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
          )
          .withOneHalfWidth(),
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
          )
          .withOneHalfWidth(),
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
          )
          .withOneHalfWidth()
      )
    )
  )

  private def isUserAnswersAlreadySubmittedAndNotModified(
    srn: Srn
  )(implicit request: DataRequest[AnyContent]): Boolean = {
    val isAlreadySubmitted: Boolean = request.userAnswers.get(FbStatus(srn)).exists(_.isSubmitted)
    val isPureAnswerStayUnchanged: Boolean = request.pureUserAnswers.fold(false)(_.data == request.userAnswers.data)
    isAlreadySubmitted && isPureAnswerStayUnchanged
  }

  private def taxEndDate(taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]]): LocalDate =
    taxYearOrAccountingPeriods match {
      case Left(taxYear) => taxYear.to
      case Right(periods) => periods.toList.maxBy(_._1.to)._1.to
    }
}
