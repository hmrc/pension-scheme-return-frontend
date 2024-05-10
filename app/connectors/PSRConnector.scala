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
import handlers.GetPsrException
import models.requests.psr.PsrSubmission
import models.AnswersSavedDisplayVersion
import uk.gov.hmrc.http.HttpReads.Implicits._
import play.api.mvc.Call
import play.api.Logger
import play.api.libs.json._
import models.backend.responses.{OverviewResponse, PsrVersionsForYearsResponse, PsrVersionsResponse}
import play.api.http.Status._
import uk.gov.hmrc.http._

import scala.concurrent.{ExecutionContext, Future}

import javax.inject.Inject

class PSRConnector @Inject()(appConfig: FrontendAppConfig, http: HttpClient) {

  private val baseUrl = appConfig.pensionSchemeReturn.baseUrl
  protected val logger: Logger = Logger(classOf[PSRConnector])
  private def submitStandardUrl = s"$baseUrl/pension-scheme-return/psr/standard"
  private def overviewUrl(pstr: String) = s"$baseUrl/pension-scheme-return/psr/overview/$pstr"
  private def versionsForYearsUrl(pstr: String, startDates: Seq[String]) =
    s"$baseUrl/pension-scheme-return/psr/versions/years/$pstr?startDates=${startDates.mkString("&startDates=")}"
  private def versionsUrl(pstr: String, startDate: String) =
    s"$baseUrl/pension-scheme-return/psr/versions/$pstr?startDate=$startDate"

  def submitPsrDetails(
    psrSubmission: PsrSubmission
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[String, Unit]] =
    http
      .POST[PsrSubmission, HttpResponse](
        submitStandardUrl,
        psrSubmission
      )
      .map { response =>
        response.status match {
          case NO_CONTENT => Right(Future.unit)
          case _ =>
            Left(s"{${response.status}, ${response.json}}")
        }
      }

  def getStandardPsrDetails(
    pstr: String,
    optFbNumber: Option[String],
    optPeriodStartDate: Option[String],
    optPsrVersion: Option[String],
    fallBackCall: Call
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[PsrSubmission]] = {
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
      .GET[HttpResponse](s"$baseUrl/pension-scheme-return/psr/standard/$pstr", queryParams)
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[PsrSubmission] match {
              case JsSuccess(data, _) => Some(data)
              case JsError(errors) => throw JsResultException(errors)
            }
          case NOT_FOUND => None
          case _ =>
            throw GetPsrException(s"${response.body}", fallBackCall.url, AnswersSavedDisplayVersion.NoDisplay)
        }
      }
  }

  def getVersionsForYears(pstr: String, startDates: Seq[String])(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Seq[PsrVersionsForYearsResponse]] =
    http
      .GET[HttpResponse](
        versionsForYearsUrl(pstr, startDates)
      )
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[Seq[PsrVersionsForYearsResponse]] match {
              case JsSuccess(_, _) =>
                response.json
                  .as[Seq[PsrVersionsForYearsResponse]]
              case JsError(errors) =>
                logger.error(
                  s"getVersions for $pstr and years $startDates returned http response 200 but could not parse the response body"
                )
                throw JsResultException(errors)
            }
          case NOT_FOUND =>
            logger.error(
              s"getVersions for $pstr and years $startDates returned http response 404 - returning empty Seq"
            )
            Seq.empty[PsrVersionsForYearsResponse]
          case _ =>
            // TODO verify if there are still 503 returned on QA
            // just logging errors to be able to continue on QA env
            logger.error(
              s"getVersions for $pstr and years $startDates returned http response $response.status - returning empty Seq"
            )
            Seq.empty[PsrVersionsForYearsResponse]
        }
      }

  def getVersions(pstr: String, startDate: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Seq[PsrVersionsResponse]] =
    http
      .GET[HttpResponse](
        versionsUrl(pstr, startDate)
      )
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[Seq[PsrVersionsResponse]] match {
              case JsSuccess(_, _) =>
                response.json
                  .as[Seq[PsrVersionsResponse]]
              case JsError(errors) =>
                logger.error(
                  s"getVersions for $pstr and $startDate returned http response 200 but could not parse the response body"
                )
                throw JsResultException(errors)
            }
          case NOT_FOUND =>
            logger.error(s"getVersions for $pstr and $startDate returned http response 404 - returning empty Seq")
            Seq.empty[PsrVersionsResponse]
          case _ =>
            // TODO verify if there are still 503 returned on QA
            // just logging errors to be able to continue on QA env
            logger.error(
              s"getVersions for $pstr and $startDate returned http response $response.status - returning empty Seq"
            )
            Seq.empty[PsrVersionsResponse]
        }
      }

  def getOverview(pstr: String, fromDate: String, toDate: String)(
    implicit hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Option[Seq[OverviewResponse]]] = {

    val queryParams =
      Seq(
        "fromDate" -> fromDate,
        "toDate" -> toDate
      )

    http
      .GET[HttpResponse](overviewUrl(pstr), queryParams)
      .map { response =>
        response.status match {
          case OK =>
            Json.parse(response.body).validate[Seq[OverviewResponse]] match {
              case JsSuccess(data, _) => Some(data)
              case JsError(errors) => throw JsResultException(errors)
            }
          case _ =>
            logger.error(
              s"getOverview for $pstr and $fromDate - $toDate returned http response $response.status - returning empty Seq"
            )
            None
        }
      }
  }
}
