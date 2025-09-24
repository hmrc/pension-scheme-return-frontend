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
import utils.nonsipp.MemberCountUtils.hasMemberNumbersChangedToOver99
import play.api.mvc._
import utils.nonsipp.summary.BasicDetailsCheckAnswersUtils
import controllers.{nonsipp, PSRController}
import cats.implicits.toTraverseOps
import controllers.actions._
import _root_.config.Constants._
import utils.nonsipp.SchemeDetailNavigationUtils
import models._
import viewmodels.models.TaskListStatus.Updated
import models.requests.DataRequest
import models.audit.PSRStartAuditEvent
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import utils.nonsipp.TaskListStatusUtils.getBasicDetailsCompletedOrUpdated
import config.Constants
import cats.data.EitherT
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp._
import navigation.Navigator

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Named}

class BasicDetailsCheckYourAnswersController @Inject() (
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

  def onPageLoadViewOnly(
    srn: Srn,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous).async { implicit request =>
      val showBackLink = true
      onPageLoadCommon(srn, mode, showBackLink)
    }

  def onPageLoadCommon(srn: Srn, mode: Mode, showBackLink: Boolean = true)(implicit
    request: DataRequest[AnyContent]
  ): Future[Result] = {
    val journeyByPassedF =
      if (BasicDetailsCheckAnswersUtils.isUserAnswersAlreadySubmittedAndNotModified(srn)) isJourneyBypassed(srn)
      else Future.successful(Right(false))
    journeyByPassedF.map(eitherJourneyNavigationResultOrRecovery =>
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
                  BasicDetailsCheckAnswersUtils.viewModel(
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
                    journeyByPassed = journeyByPassed,
                    showBackLink = showBackLink
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

  def onSubmit(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    if (hasMemberNumbersChangedToOver99(request.userAnswers, srn, request.pensionSchemeId, isPrePopulation)) {
      auditAndRedirect(srn)(using implicitly)
    } else {
      psrSubmissionService
        .submitPsrDetails(srn, fallbackCall = controllers.nonsipp.routes.TaskListController.onPageLoad(srn))
        .map {
          case None =>
            Future(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
          case Some(_) =>
            auditAndRedirect(srn)(using implicitly)
        }
        .flatten
    }
  }

  private def isCIPStartEvent(implicit request: DataRequest[?]): Boolean =
    request.session.get(CIP_START_EVENT_FLAG).fold(false)(_.toBoolean)

  private def auditAndRedirect(srn: Srn)(implicit request: DataRequest[AnyContent]): Future[Result] = {
    val eitherResultOrFutureResult: Either[Result, Future[Result]] =
      for {
        taxYear <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney
        schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
        userName <- loggedInUserNameOrRedirect
        _ = if (isCIPStartEvent) auditService.sendEvent(buildAuditEvent(taxYear, schemeMemberNumbers, userName))
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
          .map(res =>
            res
              .map(
                if (_)
                  Redirect(byPassedJourney)
                    .removingFromSession(Constants.CIP_START_EVENT_FLAG)
                else Redirect(regularJourney).removingFromSession(Constants.CIP_START_EVENT_FLAG)
              )
              .merge
          )
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
    identifyAndRequireData(srn, ViewOnlyMode, year, newCurrent, newPrevious).async { implicit request =>
      val showBackLink = false
      onPageLoadCommon(srn, ViewOnlyMode, showBackLink)
    }
  }

  private def buildAuditEvent(taxYear: DateRange, schemeMemberNumbers: SchemeMemberNumbers, userName: String)(implicit
    req: DataRequest[?]
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
