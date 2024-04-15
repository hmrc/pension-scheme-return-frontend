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

import play.api.test.FakeRequest
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty}
import connectors.{MinimalDetailsConnector, MinimalDetailsError, SchemeDetailsConnector}
import controllers.routes
import config.FrontendAppConfig
import models.SchemeId.Srn
import models.requests.IdentifierRequest.{AdministratorRequest, PractitionerRequest}
import models.PensionSchemeId.{PsaId, PspId}
import connectors.MinimalDetailsError.{DelimitedAdmin, DetailsNotFound}
import play.api.http.Status.OK
import models.{MinimalDetails, SchemeDetails}
import models.requests.IdentifierRequest
import play.api.mvc.Results.Ok
import org.mockito.ArgumentMatchers.{any, eq => meq}
import utils.BaseSpec
import play.api.test.Helpers._
import org.mockito.Mockito.{reset, when}
import play.api.Application
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

class AllowAccessActionSpec extends BaseSpec with ScalaCheckPropertyChecks {

  def allowAccessAction(appConfig: FrontendAppConfig) =
    new AllowAccessActionProviderImpl(
      appConfig,
      mockSchemeDetailsConnector,
      mockMinimalDetailsConnector
    )(ExecutionContext.global)

  lazy val mockMinimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
  lazy val mockSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]

  class Handler[A](appConfig: FrontendAppConfig, request: IdentifierRequest[A]) {

    def run(srn: Srn): Action[AnyContent] = new FakeActionBuilder(request).andThen(allowAccessAction(appConfig)(srn)) {
      request =>
        Ok(Json.toJson(request.schemeDetails))
    }
  }

  def handler[A](request: IdentifierRequest[A])(implicit app: Application) = new Handler(appConfig, request)

  def appConfig(implicit app: Application): FrontendAppConfig = injected[FrontendAppConfig]

  def setupSchemeDetails(psaId: PsaId, srn: Srn, result: Future[Option[SchemeDetails]]): Unit =
    when(mockSchemeDetailsConnector.details(meq(psaId), meq(srn))(any(), any()))
      .thenReturn(result)

  def setupSchemeDetails(pspId: PspId, srn: Srn, result: Future[Option[SchemeDetails]]): Unit =
    when(mockSchemeDetailsConnector.details(meq(pspId), meq(srn))(any(), any()))
      .thenReturn(result)

  def setupCheckAssociation(psaId: PsaId, srn: Srn, result: Future[Boolean]): Unit =
    when(mockSchemeDetailsConnector.checkAssociation(meq(psaId), meq(srn))(any(), any()))
      .thenReturn(result)

  def setupCheckAssociation(pspId: PspId, srn: Srn, result: Future[Boolean]): Unit =
    when(mockSchemeDetailsConnector.checkAssociation(meq(pspId), meq(srn))(any(), any()))
      .thenReturn(result)

  def setupMinimalDetails(psaId: PsaId, result: Future[Either[MinimalDetailsError, MinimalDetails]]): Unit =
    when(mockMinimalDetailsConnector.fetch(meq(psaId))(any(), any()))
      .thenReturn(result)

  def setupMinimalDetails(pspId: PspId, result: Future[Either[MinimalDetailsError, MinimalDetails]]): Unit =
    when(mockMinimalDetailsConnector.fetch(meq(pspId))(any(), any()))
      .thenReturn(result)

  override def beforeEach(): Unit = {
    reset(mockSchemeDetailsConnector)
    reset(mockMinimalDetailsConnector)

    // setup green path
    setupSchemeDetails(psaId, srn, Future.successful(Some(schemeDetails)))
    setupCheckAssociation(psaId, srn, Future.successful(true))
    setupMinimalDetails(psaId, Future.successful(Right(minimalDetails)))

    setupSchemeDetails(pspId, srn, Future.successful(Some(schemeDetails)))
    setupCheckAssociation(pspId, srn, Future.successful(true))
    setupMinimalDetails(pspId, Future.successful(Right(minimalDetails)))
  }

  val psaId: PsaId = psaIdGen.sample.value
  val pspId: PspId = pspIdGen.sample.value
  val schemeDetails: SchemeDetails =
    schemeDetailsGen.sample.value.copy(schemeStatus = validSchemeStatusGen.sample.value)
  val minimalDetails: MinimalDetails = minimalDetailsGen.sample.value.copy(rlsFlag = false, deceasedFlag = false)
  val srn: Srn = srnGen.sample.value
  val administratorRequest: AdministratorRequest[AnyContentAsEmpty.type] =
    AdministratorRequest("", "", FakeRequest(), psaId)
  val practitionerRequest: PractitionerRequest[AnyContentAsEmpty.type] =
    PractitionerRequest("", "", FakeRequest(), pspId)

  "AllowAccessAction" - {

    "return ok" - {

      "psa is associated, no rls flag, no deceased flag, no DelimitedAdmin and a valid status" in runningApplication {
        implicit app =>
          val result = handler(administratorRequest).run(srn)(FakeRequest())

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(schemeDetails)
      }

      "psp is associated, no rls flag, no deceased flag, no DelimitedAdmin and a valid status" in runningApplication {
        implicit app =>
          val result = handler(practitionerRequest).run(srn)(FakeRequest())

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(schemeDetails)
      }
    }

    "redirect to unauthorized page" - {

      "psa check association returns false" in runningApplication { implicit app =>
        setupCheckAssociation(psaId, srn, Future.successful(false))

        val result = handler(administratorRequest).run(srn)(FakeRequest())
        val expectedUrl = routes.UnauthorisedController.onPageLoad().url

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "psp check association returns false" in runningApplication { implicit app =>
        setupCheckAssociation(pspId, srn, Future.successful(false))

        val result = handler(practitionerRequest).run(srn)(FakeRequest())
        val expectedUrl = routes.UnauthorisedController.onPageLoad().url

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "psa minimal details return not found" in runningApplication { implicit app =>
        setupMinimalDetails(psaId, Future.successful(Left(DetailsNotFound)))

        val result = handler(administratorRequest).run(srn)(FakeRequest())
        val expectedUrl = routes.UnauthorisedController.onPageLoad().url

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "psp minimal details return not found" in runningApplication { implicit app =>
        setupMinimalDetails(pspId, Future.successful(Left(DetailsNotFound)))

        val result = handler(practitionerRequest).run(srn)(FakeRequest())
        val expectedUrl = routes.UnauthorisedController.onPageLoad().url

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "psa - scheme details not found" in runningApplication { implicit app =>
        setupSchemeDetails(psaId, srn, Future.successful(None))

        val result = handler(administratorRequest).run(srn)(FakeRequest())
        val expectedUrl = routes.UnauthorisedController.onPageLoad().url

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "psp - scheme details not found" in runningApplication { implicit app =>
        setupSchemeDetails(pspId, srn, Future.successful(None))

        val result = handler(practitionerRequest).run(srn)(FakeRequest())
        val expectedUrl = routes.UnauthorisedController.onPageLoad().url

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "psa - scheme has an invalid status" in runningApplication { implicit app =>
        forAll(invalidSchemeStatusGen) { schemeStatus =>
          setupSchemeDetails(psaId, srn, Future.successful(Some(schemeDetails.copy(schemeStatus = schemeStatus))))

          val result = handler(administratorRequest).run(srn)(FakeRequest())
          val expectedUrl = routes.UnauthorisedController.onPageLoad().url

          redirectLocation(result) mustBe Some(expectedUrl)
        }
      }

      "psp - scheme has an invalid status" in runningApplication { implicit app =>
        forAll(invalidSchemeStatusGen) { schemeStatus =>
          setupSchemeDetails(pspId, srn, Future.successful(Some(schemeDetails.copy(schemeStatus = schemeStatus))))

          val result = handler(practitionerRequest).run(srn)(FakeRequest())
          val expectedUrl = routes.UnauthorisedController.onPageLoad().url

          redirectLocation(result) mustBe Some(expectedUrl)
        }
      }
    }

    "redirect to update contact details page" - {

      "psa rls flag is set" in runningApplication { implicit app =>
        setupMinimalDetails(psaId, Future.successful(Right(minimalDetails.copy(rlsFlag = true))))

        val result = handler(administratorRequest).run(srn)(FakeRequest())
        val expectedUrl = appConfig.urls.pensionAdministrator.updateContactDetails

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "psp rls flag is set" in runningApplication { implicit app =>
        setupMinimalDetails(pspId, Future.successful(Right(minimalDetails.copy(rlsFlag = true))))

        val result = handler(practitionerRequest).run(srn)(FakeRequest())
        val expectedUrl = appConfig.urls.pensionPractitioner.updateContactDetails

        redirectLocation(result) mustBe Some(expectedUrl)
      }
    }

    "redirect to contact hmrc page" - {

      "psa deceased flag is set" in runningApplication { implicit app =>
        setupMinimalDetails(psaId, Future.successful(Right(minimalDetails.copy(deceasedFlag = true))))

        val result = handler(administratorRequest).run(srn)(FakeRequest())
        val expectedUrl = appConfig.urls.managePensionsSchemes.contactHmrc

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "psp deceased flag is set" in runningApplication { implicit app =>
        setupMinimalDetails(pspId, Future.successful(Right(minimalDetails.copy(deceasedFlag = true))))

        val result = handler(practitionerRequest).run(srn)(FakeRequest())
        val expectedUrl = appConfig.urls.managePensionsSchemes.contactHmrc

        redirectLocation(result) mustBe Some(expectedUrl)
      }
    }

    "redirect to cannot access deregistered page" - {

      "psa minimal details return delimited admin" in runningApplication { implicit app =>
        setupMinimalDetails(psaId, Future.successful(Left(DelimitedAdmin)))

        val result = handler(administratorRequest).run(srn)(FakeRequest())
        val expectedUrl = appConfig.urls.managePensionsSchemes.cannotAccessDeregistered

        redirectLocation(result) mustBe Some(expectedUrl)
      }

      "psp minimal details return delimited admin" in runningApplication { implicit app =>
        setupMinimalDetails(pspId, Future.successful(Left(DelimitedAdmin)))

        val result = handler(practitionerRequest).run(srn)(FakeRequest())
        val expectedUrl = appConfig.urls.managePensionsSchemes.cannotAccessDeregistered

        redirectLocation(result) mustBe Some(expectedUrl)
      }
    }
  }

}
