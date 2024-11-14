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

package services

import utils.nonsipp.TaskListCipUtils.transformTaskListToCipFormat
import models.audit.{ExtendedAuditEvent, PSRCompileAuditEvent, PSRSubmissionAuditEvent}
import play.api.mvc.Call
import connectors.PSRConnector
import cats.implicits._
import transformations._
import models.{DateRange, UserAnswers}
import play.api.i18n.{I18nSupport, MessagesApi}
import models.requests.DataRequest
import handlers.PostPsrException
import models.SchemeId.Srn
import utils.nonsipp.PrePopulationUtils.isPrePopulation
import models.requests.psr._
import config.Constants._
import pages.nonsipp.CheckReturnDatesPage
import play.api.Logging
import utils.nonsipp.TaskListUtils.getSectionList
import play.api.libs.json.Json
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class PsrSubmissionService @Inject()(
  psrConnector: PSRConnector,
  schemeDateService: SchemeDateService,
  auditService: AuditService,
  override val messagesApi: MessagesApi,
  minimalRequiredSubmissionTransformer: MinimalRequiredSubmissionTransformer,
  loansTransformer: LoansTransformer,
  memberPaymentsTransformer: MemberPaymentsTransformer,
  sharesTransformer: SharesTransformer,
  assetsTransformer: AssetsTransformer,
  declarationTransformer: DeclarationTransformer,
  sessionRepository: SessionRepository
) extends PsrBaseService
    with I18nSupport
    with Logging {

  def submitPsrDetailsWithUA(
    srn: Srn,
    userAnswers: UserAnswers,
    fallbackCall: Call
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] =
    submitPsrDetails(srn, fallbackCall = fallbackCall)(
      implicitly,
      implicitly,
      DataRequest(request.request, userAnswers, previousUserAnswers = request.previousUserAnswers)
    )

  def submitPsrDetails(
    srn: Srn,
    isSubmitted: Boolean = false,
    fallbackCall: Call
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] = {
    val currentUA = request.userAnswers
    sessionRepository
      .get(UNCHANGED_SESSION_PREFIX + currentUA.id)
      .flatMap(
        _.flatMap(
          initialUA => {
            val isAnyUpdated = initialUA.data != currentUA.data
            if (isSubmitted || isAnyUpdated) {
              (
                minimalRequiredSubmissionTransformer.transformToEtmp(srn, initialUA, isSubmitted),
                currentUA.get(CheckReturnDatesPage(srn)),
                schemeDateService.schemeDate(srn)
              ).mapN {
                (minimalRequiredSubmission, checkReturnDates, taxYear) =>
                  {
                    val submissionRequest: PsrSubmission = PsrSubmission(
                      minimalRequiredSubmission = minimalRequiredSubmission,
                      checkReturnDates = checkReturnDates,
                      loans = loansTransformer.transformToEtmp(srn, initialUA),
                      assets = assetsTransformer.transformToEtmp(srn, initialUA),
                      membersPayments = memberPaymentsTransformer
                        .transformToEtmp(srn, currentUA, initialUA, request.previousUserAnswers),
                      shares = sharesTransformer.transformToEtmp(srn, initialUA),
                      psrDeclaration = Option.when(isSubmitted)(declarationTransformer.transformToEtmp)
                    )
                    callSubmission(srn, submissionRequest, isSubmitted)
                      .flatMap {
                        case Left(message: String) =>
                          throw PostPsrException(message, fallbackCall.url)
                        case Right(()) =>
                          auditService.sendExtendedEvent(buildAuditEvent(taxYear, isSubmitted, submissionRequest))
                          Future.unit
                      }
                  }
              }
            } else {
              Some(Future.unit)
            }
          }
        ).sequence
      )
  }

  private def callSubmission(
    srn: Srn,
    submissionRequest: PsrSubmission,
    isSubmitted: Boolean
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Either[String, Unit]] =
    if (isPrePopulation && !isSubmitted) {
      logger.info("Submit pre-populated PSR is called")
      psrConnector
        .submitPrePopulatedPsr(
          submissionRequest,
          schemeAdministratorOrPractitionerName,
          request.schemeDetails.schemeName,
          srn
        )
    } else {
      logger.info("Submit PSR details is called")
      psrConnector
        .submitPsrDetails(
          submissionRequest,
          schemeAdministratorOrPractitionerName,
          request.schemeDetails.schemeName,
          srn
        )
    }

  def submitPsrDetailsBypassed(
    srn: Srn,
    fallbackCall: Call
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: DataRequest[_]): Future[Option[Unit]] =
    (
      minimalRequiredSubmissionTransformer.transformToEtmp(srn, request.userAnswers, isSubmitted = true),
      request.userAnswers.get(CheckReturnDatesPage(srn)),
      schemeDateService.schemeDate(srn)
    ).mapN { (minimalRequiredSubmission, checkReturnDates, taxYear) =>
      {
        val submissionRequest: PsrSubmission = PsrSubmission(
          minimalRequiredSubmission = MinimalRequiredSubmission(
            minimalRequiredSubmission.reportDetails,
            minimalRequiredSubmission.accountingPeriodDetails,
            minimalRequiredSubmission.schemeDesignatory.copy(
              totalCashStart = None,
              totalCashEnd = None,
              totalPayments = None,
              totalAssetValueStart = None,
              totalAssetValueEnd = None
            )
          ),
          checkReturnDates = checkReturnDates,
          loans = None,
          assets = None,
          membersPayments = None,
          shares = None,
          psrDeclaration = Some(declarationTransformer.transformToEtmp)
        )
        psrConnector
          .submitPsrDetails(
            submissionRequest,
            schemeAdministratorOrPractitionerName,
            request.schemeDetails.schemeName,
            srn
          )
          .flatMap {
            case Left(message: String) =>
              throw PostPsrException(message, fallbackCall.url)
            case Right(()) =>
              auditService.sendExtendedEvent(buildAuditEvent(taxYear, isSubmitted = true, submissionRequest))
              Future.unit
          }
      }
    }.sequence

  private def buildAuditEvent(taxYear: DateRange, isSubmitted: Boolean, submissionRequest: PsrSubmission)(
    implicit req: DataRequest[_]
  ): ExtendedAuditEvent = {
    val affinityGroup = if (req.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual"
    val credentialRole = if (req.pensionSchemeId.isPSP) PSP else PSA

    if (isSubmitted) {
      PSRSubmissionAuditEvent(
        schemeName = req.schemeDetails.schemeName,
        schemeAdministratorOrPractitionerName,
        psaOrPspId = req.pensionSchemeId.value,
        schemeTaxReference = req.schemeDetails.pstr,
        affinityGroup = affinityGroup,
        credentialRole = credentialRole,
        taxYear = taxYear,
        psrSubmission = submissionRequest
      )
    } else {
      PSRCompileAuditEvent(
        schemeName = req.schemeDetails.schemeName,
        schemeAdministratorOrPractitionerName,
        psaOrPspId = req.pensionSchemeId.value,
        schemeTaxReference = req.schemeDetails.pstr,
        affinityGroup = affinityGroup,
        credentialRole = credentialRole,
        taxYear = taxYear,
        taskList = Json
          .toJson(
            transformTaskListToCipFormat(
              getSectionList(
                req.srn,
                req.schemeDetails.schemeName,
                req.userAnswers,
                req.pensionSchemeId,
                noChangesSincePreviousVersion = false,
                None,
                None
              ),
              messagesApi
            ).list
          )
      )
    }
  }

}
