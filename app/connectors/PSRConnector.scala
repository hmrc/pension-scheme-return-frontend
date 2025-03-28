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

package connectors

import config.FrontendAppConfig
import play.api.libs.ws.WSRequest
import models.AnswersSavedDisplayVersion
import uk.gov.hmrc.http.client.HttpClientV2
import models.requests.{AllowedAccessRequest, DataRequest}
import uk.gov.hmrc.http.HttpReads.Implicits._
import play.api.mvc.Call
import handlers.GetPsrException
import models.SchemeId.Srn
import models.requests.psr.PsrSubmission
import config.Constants.{PSA, PSP}
import play.api.Logger
import play.api.libs.json._
import models.backend.responses.{OverviewResponse, PsrVersionsForYearsResponse, PsrVersionsResponse}
import play.api.http.Status._
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.{Inject, Singleton}

@Singleton
class PSRConnector @Inject()(appConfig: FrontendAppConfig, http: HttpClientV2) {

  private val baseUrl = appConfig.pensionSchemeReturn.baseUrl
  protected val logger: Logger = Logger(classOf[PSRConnector])
  private def getStandardUrl(pstr: String) = s"$baseUrl/pension-scheme-return/psr/standard/$pstr"
  private def submitStandardUrl = s"$baseUrl/pension-scheme-return/psr/standard"
  private def submitPrePopulatedUrl = s"$baseUrl/pension-scheme-return/psr/pre-populated"
  private def overviewUrl(pstr: String) = s"$baseUrl/pension-scheme-return/psr/overview/$pstr"
  private def versionsForYearsUrl(pstr: String, startDates: Seq[String]) =
    s"$baseUrl/pension-scheme-return/psr/versions/years/$pstr?startDates=${startDates.mkString("&startDates=")}"
  private def versionsUrl(pstr: String, startDate: String) =
    s"$baseUrl/pension-scheme-return/psr/versions/$pstr?startDate=$startDate"

  def submitPsrDetails(
    psrSubmission: PsrSubmission,
    userName: String,
    schemeName: String,
    srn: Srn
  )(
    implicit request: DataRequest[_],
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[String, Unit]] =
    http
      .post(url"$submitStandardUrl")
      .withBody(Json.toJson(psrSubmission))
      .transform(buildHeaders(_, userName, schemeName, srn, if (request.pensionSchemeId.isPSP) PSP else PSA))
      .transform(_.withRequestTimeout(appConfig.ifsTimeout))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case NO_CONTENT => Right(())
          case _ =>
            Left(s"{${response.status}, ${response.json}}")
        }
      }

  def submitPrePopulatedPsr(
    psrSubmission: PsrSubmission,
    userName: String,
    schemeName: String,
    srn: Srn
  )(
    implicit request: DataRequest[_],
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[String, Unit]] =
    http
      .post(url"$submitPrePopulatedUrl")
      .withBody(Json.toJson(psrSubmission))
      .transform(buildHeaders(_, userName, schemeName, srn, if (request.pensionSchemeId.isPSP) PSP else PSA))
      .transform(_.withRequestTimeout(appConfig.ifsTimeout))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case NO_CONTENT => Right(())
          case _ =>
            Left(s"{${response.status}, ${response.json}}")
        }
      }

  def getStandardPsrDetails(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    fallBackCall: Call,
    userName: String,
    schemeName: String,
    srn: Srn
  )(
    implicit request: DataRequest[_],
    headerCarrier: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[PsrSubmission]] = {
    val queryParams = (optPeriodStartDate, optPsrVersion, optFbNumber) match {
      case (Some(startDate), Some(version), _) =>
        Seq(
          "periodStartDate" -> startDate,
          "psrVersion" -> version
        )
      case (_, _, Some(fbNumber)) =>
        Seq("fbNumber" -> fbNumber)
    }
    http
      .get(url"${getStandardUrl(pstr)}?$queryParams")
      .transform(buildHeaders(_, userName, schemeName, srn, if (request.pensionSchemeId.isPSP) PSP else PSA))
      .transform(_.withRequestTimeout(appConfig.ifsTimeout))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            logger.info(s"Getting standard PSR for pstr $pstr with params $queryParams returned OK.")
            Json.parse(response.body).validate[PsrSubmission] match {
              case JsSuccess(data, _) => Some(data)
              case JsError(errors) => throw JsResultException(errors)
            }
          case NOT_FOUND =>
            logger.info(
              s"Getting standard PSR for pstr $pstr returned NOT_FOUND. fbNumber $optFbNumber - periodStartDate $optPeriodStartDate - psrVersion $optPsrVersion"
            )
            None
          case _ =>
            throw GetPsrException(s"${response.body}", fallBackCall.url, AnswersSavedDisplayVersion.NoDisplay)
        }
      }
  }

  def getVersionsForYears(pstr: String, startDates: Seq[String], srn: Srn, fallBackCall: Call)(
    implicit request: AllowedAccessRequest[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Seq[PsrVersionsForYearsResponse]] =
    http
      .get(url"${versionsForYearsUrl(pstr, startDates)}")
      .transform(
        _.addHttpHeaders(
          "srn" -> srn.value,
          "requestRole" -> (if (request.pensionSchemeId.isPSP) PSP else PSA)
        )
      )
      .transform(_.withRequestTimeout(appConfig.ifsTimeout))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[Seq[PsrVersionsForYearsResponse]] match {
              case JsSuccess(_, _) =>
                response.json
                  .as[Seq[PsrVersionsForYearsResponse]]
              case JsError(errors) =>
                logger.warn(
                  s"getVersions for $pstr and years $startDates returned http response 200 but could not parse the response body"
                )
                throw JsResultException(errors)
            }
          case NOT_FOUND =>
            logger.warn(
              s"getVersions for $pstr and years $startDates returned http response 404 - returning empty Seq"
            )
            Seq.empty[PsrVersionsForYearsResponse]
          case SERVICE_UNAVAILABLE =>
            logger.warn(
              s"getVersions for $pstr and years $startDates returned http response 503 - returning empty Seq"
            )
            Seq.empty[PsrVersionsForYearsResponse]
          case _ =>
            logger.warn(
              s"getVersions for $pstr and years $startDates returned http response ${response.status}"
            )
            throw GetPsrException(s"${response.body}", fallBackCall.url, AnswersSavedDisplayVersion.NoDisplay)
        }
      }

  def getVersions(pstr: String, startDate: String, srn: Srn, fallBackCall: Call)(
    implicit request: DataRequest[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Seq[PsrVersionsResponse]] =
    http
      .get(url"${versionsUrl(pstr, startDate)}")
      .transform(
        _.addHttpHeaders(
          "srn" -> srn.value,
          "requestRole" -> (if (request.pensionSchemeId.isPSP) PSP
                            else PSA)
        )
      )
      .transform(_.withRequestTimeout(appConfig.ifsTimeout))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[Seq[PsrVersionsResponse]] match {
              case JsSuccess(_, _) =>
                response.json
                  .as[Seq[PsrVersionsResponse]]
              case JsError(errors) =>
                logger.warn(
                  s"getVersions for $pstr and $startDate returned http response 200 but could not parse the response body"
                )
                throw JsResultException(errors)
            }
          case NOT_FOUND =>
            logger.warn(s"getVersions for $pstr and $startDate returned http response 404 - returning empty Seq")
            Seq.empty[PsrVersionsResponse]
          case SERVICE_UNAVAILABLE =>
            logger.warn(
              s"getVersions for $pstr and $startDate returned http response 503 - returning empty Seq"
            )
            Seq.empty[PsrVersionsResponse]
          case _ =>
            logger.warn(
              s"getVersions for $pstr and $startDate returned http response ${response.status} - returning empty Seq"
            )
            throw GetPsrException(s"${response.body}", fallBackCall.url, AnswersSavedDisplayVersion.NoDisplay)
        }
      }

  def getOverview(pstr: String, fromDate: String, toDate: String, srn: Srn)(
    implicit request: AllowedAccessRequest[_],
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[Seq[OverviewResponse]]] = {

    val queryParams =
      Seq(
        "fromDate" -> fromDate,
        "toDate" -> toDate
      )
    http
      .get(url"${overviewUrl(pstr)}?$queryParams")
      .transform(
        _.addHttpHeaders(
          "srn" -> srn.value,
          "requestRole" -> (if (request.pensionSchemeId.isPSP) PSP else PSA)
        )
      )
      .transform(_.withRequestTimeout(appConfig.ifsTimeout))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[Seq[OverviewResponse]] match {
              case JsSuccess(data, _) => Some(data)
              case JsError(errors) => throw JsResultException(errors)
            }
          case _ =>
            logger.warn(
              s"getOverview for $pstr and $fromDate - $toDate returned http response ${response.status} - returning empty Seq"
            )
            None
        }
      }
  }

  private def buildHeaders(
    wsRequest: WSRequest,
    userName: String,
    schemeName: String,
    srn: Srn,
    requestRole: String
  ): WSRequest =
    wsRequest.addHttpHeaders(
      "Content-Type" -> "application/json",
      "userName" -> userName,
      "schemeName" -> schemeName,
      "srn" -> srn.value,
      "requestRole" -> requestRole
    )
}
