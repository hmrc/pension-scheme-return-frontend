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
import models.audit.PSRCompileAuditEvent
import play.api.mvc.Call
import connectors.PSRConnector
import cats.implicits._
import transformations._
import models.{DateRange, UserAnswers}
import play.api.i18n.{I18nSupport, MessagesApi}
import models.requests.DataRequest
import handlers.PostPsrException
import models.SchemeId.Srn
import models.requests.psr._
import config.Constants.{PSA, PSP, UNCHANGED_SESSION_PREFIX}
import pages.nonsipp.CheckReturnDatesPage
import utils.nonsipp.TaskListUtils.getSectionList
import play.api.libs.json.Json
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

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
) extends I18nSupport {

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
            val isAnyUpdated = initialUA != currentUA
            if (isAnyUpdated) {
              (
                minimalRequiredSubmissionTransformer.transformToEtmp(srn, initialUA),
                currentUA.get(CheckReturnDatesPage(srn)),
                schemeDateService.schemeDate(srn)
              ).mapN {
                (minimalRequiredSubmission, checkReturnDates, taxYear) =>
                  {
                    psrConnector
                      .submitPsrDetails(
                        PsrSubmission(
                          minimalRequiredSubmission = minimalRequiredSubmission,
                          checkReturnDates = checkReturnDates,
                          loans = loansTransformer.transformToEtmp(srn, initialUA),
                          assets = assetsTransformer.transformToEtmp(srn, initialUA),
                          membersPayments = memberPaymentsTransformer
                            .transformToEtmp(srn, currentUA, initialUA, request.previousUserAnswers),
                          shares = sharesTransformer.transformToEtmp(srn, initialUA),
                          psrDeclaration = Option.when(isSubmitted)(declarationTransformer.transformToEtmp)
                        )
                      )
                      .flatMap {
                        case Left(message: String) =>
                          throw PostPsrException(message, fallbackCall.url)
                        case Right(()) =>
                          auditService.sendExtendedEvent(buildAuditEvent(taxYear, loggedInUserNameOrBlank(request)))
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

  private def loggedInUserNameOrBlank(implicit request: DataRequest[_]): String =
    request.minimalDetails.individualDetails match {
      case Some(individual) => individual.fullName
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => orgName
          case None => ""
        }
    }

  private def buildAuditEvent(taxYear: DateRange, userName: String)(
    implicit req: DataRequest[_]
  ) = PSRCompileAuditEvent(
    schemeName = req.schemeDetails.schemeName,
    req.schemeDetails.establishers.headOption.fold(userName)(e => e.name),
    psaOrPspId = req.pensionSchemeId.value,
    schemeTaxReference = req.schemeDetails.pstr,
    affinityGroup = if (req.minimalDetails.organisationName.nonEmpty) "Organisation" else "Individual",
    credentialRole = if (req.pensionSchemeId.isPSP) PSP else PSA,
    taxYear = taxYear,
    taskList = Json
      .toJson(
        transformTaskListToCipFormat(
          getSectionList(req.srn, req.schemeDetails.schemeName, req.userAnswers, req.pensionSchemeId),
          messagesApi
        ).list
      )
  )
}
