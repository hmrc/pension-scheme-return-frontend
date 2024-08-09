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
import viewmodels.implicits._
import play.api.mvc._
import utils.ListUtils.ListOps
import controllers.{nonsipp, PSRController}
import utils.nonsipp.TaskListStatusUtils.getBasicDetailsTaskListStatus
import cats.implicits.{toShow, toTraverseOps}
import controllers.actions._
import controllers.nonsipp.BasicDetailsCheckYourAnswersController._
import _root_.config.Constants.{PSA, PSP}
import pages.nonsipp.memberdetails.Paths.memberDetails
import viewmodels.models.TaskListStatus.Updated
import models.requests.DataRequest
import _root_.config.Refined.Max3
import models.audit.PSRStartAuditEvent
import pages.nonsipp.schemedesignatory.{ActiveBankAccountPage, HowManyMembersPage, WhyNoBankAccountPage}
import cats.data.{EitherT, NonEmptyList}
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp._
import navigation.Navigator
import play.api.libs.json.JsObject
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
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  schemeDateService: SchemeDateService,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  auditService: AuditService,
  psrVersionsService: PsrVersionsService,
  psrRetrievalService: PsrRetrievalService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    onPageLoadCommon(srn, mode)(implicitly)
  }

  def onPageLoadViewOnly(srn: Srn, mode: Mode, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn, mode)(implicitly)
    }

  def onPageLoadCommon(srn: Srn, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    schemeDateService.taxYearOrAccountingPeriods(srn) match {
      case Some(periods) =>
        (
          for {
            schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
            activeBankAccount <- requiredPage(ActiveBankAccountPage(srn))
            whyNoBankAccount = request.userAnswers.get(WhyNoBankAccountPage(srn))
            whichTaxYearPage = request.userAnswers.get(WhichTaxYearPage(srn))
            userName <- loggedInUserNameOrRedirect
          } yield Ok(
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
                request.pensionSchemeId.value,
                request.pensionSchemeId.isPSP,
                viewOnlyUpdated = if (mode == ViewOnlyMode && request.previousUserAnswers.nonEmpty) {
                  getBasicDetailsTaskListStatus(request.userAnswers, request.previousUserAnswers.get) == Updated
                } else {
                  false
                },
                optYear = request.year,
                optCurrentVersion = request.currentVersion,
                optPreviousVersion = request.previousVersion,
                compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
              )
            )
          )
        ).merge
      case _ => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

  def onSubmit(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    psrSubmissionService
      .submitPsrDetails(srn, fallbackCall = controllers.nonsipp.routes.TaskListController.onPageLoad(srn))
      .map {
        case None =>
          Future(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        case Some(_) =>
          val eitherResultOrFutureResult: Either[Result, Future[Result]] =
            for {
              taxYear <- schemeDateService.taxYearOrAccountingPeriods(srn).merge.getOrRecoverJourney
              schemeMemberNumbers <- requiredPage(HowManyMembersPage(srn, request.pensionSchemeId))
              userName <- loggedInUserNameOrRedirect
              _ = auditService.sendEvent(buildAuditEvent(taxYear, schemeMemberNumbers, userName))
            } yield {
              calculateNextPage(srn)
            }

          // Transform Either[Result, Future[Result]] into a Future[Result]
          EitherT(eitherResultOrFutureResult.sequence).merge
      }
      .flatten
  }

  /**
   * This method determines whether the user proceeds directly to the Task List page or skips to the Declaration page.
   *
   * This is dependent on two factors: (1) the number of Active & Deferred members in the scheme, and (2) whether any
   * 'full' returns have been submitted for this scheme before.
   *
   * If the number of Active + Deferred members > 99, and no 'full' returns have been submitted for this scheme, then
   * the user will skip to the Declaration page. In all other cases, they will proceed to the Task List page.
   *
   * A 'full' return must include at least 1 member, while a 'skipped' return will contain no member details at all, so
   * we use {{{.get(memberDetails).getOrElse(JsObject.empty).as[JsObject] == JsObject.empty}}} to determine whether or
   * not a retrieved set of `UserAnswers` refers to a 'full' or 'skipped' return.
   */
  private def calculateNextPage(srn: Srn)(implicit request: DataRequest[AnyContent]): Future[Result] = {

    // Determine next page in case of Declaration redirect
    val declarationPage = if (request.pensionSchemeId.isPSP) {
      nonsipp.declaration.routes.PspDeclarationController.onPageLoad(srn)
    } else {
      nonsipp.declaration.routes.PsaDeclarationController.onPageLoad(srn)
    }

    // Determine if the member threshold is reached
    val currentSchemeMembers = request.userAnswers.get(HowManyMembersPage(srn, request.pensionSchemeId))
    if (currentSchemeMembers.exists(_.totalActiveAndDeferred > 99)) {
      // If so, then determine if a full return was submitted this tax year
      val noFullReturnSubmittedThisTaxYear = request.previousUserAnswers match {
        case Some(previousUserAnswers) =>
          previousUserAnswers.get(memberDetails).getOrElse(JsObject.empty).as[JsObject] == JsObject.empty
        case None =>
          true
      }

      if (noFullReturnSubmittedThisTaxYear) {
        // If so, then determine if a full return was submitted last tax year
        request.userAnswers.get(WhichTaxYearPage(srn)) match {
          case Some(currentReturnTaxYear) =>
            val previousTaxYear = formatDateForApi(currentReturnTaxYear.from.minusYears(1))
            val previousTaxYearVersions = psrVersionsService.getVersions(request.schemeDetails.pstr, previousTaxYear)

            previousTaxYearVersions.map { psrVersionsResponses =>
              if (psrVersionsResponses.nonEmpty) {
                // If so, then determine if the latest submitted return from last year was a full return
                val latestVersionNumber = "%03d".format(psrVersionsResponses.map(_.reportVersion).max)
                val latestReturnFromPreviousTaxYear = psrRetrievalService.getAndTransformStandardPsrDetails(
                  optPeriodStartDate = Some(previousTaxYear),
                  optPsrVersion = Some(latestVersionNumber),
                  fallBackCall = controllers.routes.OverviewController.onPageLoad(request.srn)
                )

                latestReturnFromPreviousTaxYear.map { previousUserAnswers =>
                  val noFullReturnSubmittedLastTaxYear =
                    previousUserAnswers.get(memberDetails).getOrElse(JsObject.empty).as[JsObject] == JsObject.empty

                  if (noFullReturnSubmittedLastTaxYear) { // Redirect triggered: no 'full' returns submitted last year
                    Redirect(declarationPage)
                  } else { // No redirect triggered: 'full' return submitted last year
                    Redirect(navigator.nextPage(BasicDetailsCheckYourAnswersPage(srn), NormalMode, request.userAnswers))
                  }
                }
              } else { // Redirect triggered: no returns of any kind submitted last year
                Future(Redirect(declarationPage))
              }
            }.flatten
          case None => // Couldn't get current return's tax year, so something's gone wrong
            Future(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
      } else { // No redirect: full return was submitted earlier this tax year
        Future(Redirect(navigator.nextPage(BasicDetailsCheckYourAnswersPage(srn), NormalMode, request.userAnswers)))
      }
    } else { // No redirect: too few Active & Deferred members
      Future(Redirect(navigator.nextPage(BasicDetailsCheckYourAnswersPage(srn), NormalMode, request.userAnswers)))
    }
  }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(Redirect(routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous)))
    }

  def onPreviousViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController
            .onPageLoadViewOnly(srn, year, (current - 1).max(0), (previous - 1).max(0))
        )
      )
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
    pensionSchemeId: String,
    isPSP: Boolean,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
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
      buttonText = "site.saveAndContinue",
      onSubmit = routes.BasicDetailsCheckYourAnswersController.onSubmit(srn, mode),
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
            buttonText = "site.return.to.tasklist",
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
    pensionSchemeId: String,
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
          pensionSchemeId
        ).withOneHalfWidth()
      ) ++
        (taxYearOrAccountingPeriods match {
          case Left(taxYear) =>
            List(
              CheckYourAnswersRowViewModel(
                "basicDetailsCheckYourAnswersController.schemeDetails.taxYear",
                taxYear.show
              ).withOneHalfWidth()
            )
          case Right(accountingPeriods) =>
            List(
              CheckYourAnswersRowViewModel(
                "basicDetailsCheckYourAnswersController.schemeDetails.taxYear",
                whichTaxYearPage.get.show
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

  private def taxEndDate(taxYearOrAccountingPeriods: Either[DateRange, NonEmptyList[(DateRange, Max3)]]): LocalDate =
    taxYearOrAccountingPeriods match {
      case Left(taxYear) => taxYear.to
      case Right(periods) => periods.toList.maxBy(_._1.to)._1.to
    }
}
