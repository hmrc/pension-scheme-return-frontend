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

package controllers.actions

import play.api.mvc.{Action, AnyContent}
import controllers.routes
import config.{Constants, FrontendAppConfig}
import models.cache.PensionSchemeUser.{Administrator, Practitioner}
import play.api.mvc.Results.Ok
import org.mockito.ArgumentMatchers._
import connectors.cache.SessionDataCacheConnector
import play.api.test.{FakeRequest, StubPlayBodyParsersFactory}
import utils.BaseSpec
import play.api.test.Helpers._
import org.mockito.Mockito.{reset, when}
import uk.gov.hmrc.auth.core._
import models.requests.IdentifierRequest.{AdministratorRequest, PractitionerRequest}
import uk.gov.hmrc.auth.core.retrieve.~
import models.cache.SessionData
import play.api.Application
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class IdentifierActionSpec extends BaseSpec with StubPlayBodyParsersFactory {

  def authAction(appConfig: FrontendAppConfig) =
    new IdentifierActionImpl(
      appConfig,
      mockAuthConnector,
      mockSessionDataCacheConnector,
      stubPlayBodyParsers
    )(using ExecutionContext.global)

  class Handler(appConfig: FrontendAppConfig) {
    def run: Action[AnyContent] = authAction(appConfig) { request =>
      request match {
        case AdministratorRequest(userId, externalId, _, id) =>
          Ok(Json.obj("userId" -> userId, "externalId" -> externalId, "psaId" -> id.value))
        case PractitionerRequest(userId, externalId, _, id) =>
          Ok(Json.obj("userId" -> userId, "externalId" -> externalId, "pspId" -> id.value))
      }
    }
  }

  def handler(implicit app: Application) = new Handler(appConfig)

  def appConfig(implicit app: Application): FrontendAppConfig = injected[FrontendAppConfig]

  val mockAuthConnector: AuthConnector = mock[AuthConnector]
  val mockSessionDataCacheConnector: SessionDataCacheConnector = mock[SessionDataCacheConnector]

  def authResult(internalId: Option[String], externalId: Option[String], enrolments: Enrolment*) =
    new ~(new ~(internalId, externalId), Enrolments(enrolments.toSet))

  val psaEnrolment: Enrolment =
    Enrolment(Constants.psaEnrolmentKey, Seq(EnrolmentIdentifier(Constants.psaIdKey, "A000000")), "Activated")
  val pspEnrolment: Enrolment =
    Enrolment(Constants.pspEnrolmentKey, Seq(EnrolmentIdentifier(Constants.pspIdKey, "A000001")), "Activated")

  override def beforeEach(): Unit = {
    reset(mockAuthConnector)
    reset(mockSessionDataCacheConnector)
  }

  def setAuthValue(value: Option[String] ~ Option[String] ~ Enrolments): Unit =
    setAuthValue(Future.successful(value))

  def setAuthValue[A](value: Future[A]): Unit =
    when(mockAuthConnector.authorise[A](any(), any())(using any(), any()))
      .thenReturn(value)

  def setSessionValue(value: Option[SessionData]): Unit =
    setSessionValue(Future.successful(value))

  def setSessionValue(value: Future[Option[SessionData]]): Unit =
    when(mockSessionDataCacheConnector.fetch(any())(using any(), any()))
      .thenReturn(value)

  "IdentifierAction" - {
    "return an unauthorised result" - {

      "Redirect user to sign in page when user is not signed in to GG" in runningApplication { implicit app =>
        setAuthValue(Future.failed(new NoActiveSession("No user signed in") {}))

        val result = handler.run(FakeRequest())
        val continueUrl = urlEncode(appConfig.urls.loginContinueUrl)
        val expectedUrl = s"${appConfig.urls.loginUrl}?continue=$continueUrl"

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "Redirect user to manage pension scheme when authorise fails to match predicate" in runningApplication {
        implicit app =>
          setAuthValue(Future.failed(new AuthorisationException("Authorise predicate fails") {}))

          val result = handler.run(FakeRequest())
          val expectedUrl = appConfig.urls.managePensionsSchemes.registerUrl

          redirectLocation(result) mustBe Some(expectedUrl)
      }

      "Redirect to unauthorised page when user does not have an external id" in runningApplication { implicit app =>
        setAuthValue(authResult(Some("id"), None, psaEnrolment))

        val result = handler.run(FakeRequest())
        val expectedUrl = routes.UnauthorisedController.onPageLoad().url

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "Redirect to unauthorised page when user does not have an internal id" in runningApplication { implicit app =>
        setAuthValue(authResult(None, Some("id"), psaEnrolment))

        val result = handler.run(FakeRequest())
        val expectedUrl = routes.UnauthorisedController.onPageLoad().url

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "Redirect user to manage pension scheme when user does not have a psa or psp enrolment" in runningApplication {
        implicit app =>
          setAuthValue(authResult(Some("internalId"), Some("externalId")))

          val result = handler.run(FakeRequest())
          val expectedUrl = appConfig.urls.managePensionsSchemes.registerUrl

          redirectLocation(result) mustBe Some(expectedUrl)
      }

      "Redirect user to admin or practitioner page" - {
        "user has both psa and psp enrolment but nothing is in the cache" in runningApplication { implicit app =>
          setAuthValue(authResult(Some("internalId"), Some("externalId"), psaEnrolment, pspEnrolment))
          setSessionValue(None)

          val result = handler.run(FakeRequest())
          val expectedUrl = appConfig.urls.managePensionsSchemes.adminOrPractitionerUrl

          redirectLocation(result) mustBe Some(expectedUrl)
        }
      }
    }

    "return an IdentifierRequest" - {
      "User has a psa enrolment" in runningApplication { implicit app =>
        setAuthValue(authResult(Some("internalId"), Some("externalId"), psaEnrolment))

        val result = handler.run(FakeRequest())

        status(result) mustBe OK
        (contentAsJson(result) \ "psaId").asOpt[String] mustBe Some("A000000")
        (contentAsJson(result) \ "pspId").asOpt[String] mustBe None
        (contentAsJson(result) \ "userId").asOpt[String] mustBe Some("internalId")
        (contentAsJson(result) \ "externalId").asOpt[String] mustBe Some("externalId")
      }

      "User has a psp enrolment" in runningApplication { implicit app =>
        setAuthValue(authResult(Some("internalId"), Some("externalId"), pspEnrolment))

        val result = handler.run(FakeRequest())

        status(result) mustBe OK
        (contentAsJson(result) \ "psaId").asOpt[String] mustBe None
        (contentAsJson(result) \ "pspId").asOpt[String] mustBe Some("A000001")
        (contentAsJson(result) \ "userId").asOpt[String] mustBe Some("internalId")
        (contentAsJson(result) \ "externalId").asOpt[String] mustBe Some("externalId")
      }

      "User has a both psa and psp enrolment with admin stored in cache" in runningApplication { implicit app =>
        setAuthValue(authResult(Some("internalId"), Some("externalId"), psaEnrolment, pspEnrolment))
        setSessionValue(Some(SessionData(Administrator)))

        val result = handler.run(FakeRequest())

        status(result) mustBe OK
        (contentAsJson(result) \ "psaId").asOpt[String] mustBe Some("A000000")
        (contentAsJson(result) \ "pspId").asOpt[String] mustBe None
        (contentAsJson(result) \ "userId").asOpt[String] mustBe Some("internalId")
        (contentAsJson(result) \ "externalId").asOpt[String] mustBe Some("externalId")
      }

      "User has a both psa and psp enrolment with practitioner stored in cache" in runningApplication { implicit app =>
        setAuthValue(authResult(Some("internalId"), Some("externalId"), psaEnrolment, pspEnrolment))
        setSessionValue(Some(SessionData(Practitioner)))

        val result = handler.run(FakeRequest())

        status(result) mustBe OK
        (contentAsJson(result) \ "psaId").asOpt[String] mustBe None
        (contentAsJson(result) \ "pspId").asOpt[String] mustBe Some("A000001")
        (contentAsJson(result) \ "userId").asOpt[String] mustBe Some("internalId")
        (contentAsJson(result) \ "externalId").asOpt[String] mustBe Some("externalId")
      }
    }
  }
}
