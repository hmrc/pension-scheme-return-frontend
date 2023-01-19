/*
 * Copyright 2023 HM Revenue & Customs
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

import connectors.MinimalDetailsError.{DelimitedAdmin, DetailsNotFound}
import connectors.{MinimalDetailsConnector, MinimalDetailsError, SchemeDetailsConnector}
import models.PensionSchemeId.{PsaId, PspId}
import models.{MinimalDetails, SchemeDetails}
import models.SchemeId.Srn
import models.requests.IdentifierRequest
import models.requests.IdentifierRequest.{AdministratorRequest, PractitionerRequest}
import org.mockito.ArgumentMatchers.{any, eq => meq}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.mvc.Results.Ok
import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status}
import utils.BaseSpec

import scala.concurrent.{ExecutionContext, Future}

class AllowAccessActionSpec extends BaseSpec with ScalaCheckPropertyChecks {

  lazy val allowAccessAction = new AllowAccessActionProviderImpl(
    mockSchemeDetailsConnector,
    mockMinimalDetailsConnector
  )(ExecutionContext.global)

  lazy val mockMinimalDetailsConnector: MinimalDetailsConnector = mock[MinimalDetailsConnector]
  lazy val mockSchemeDetailsConnector: SchemeDetailsConnector = mock[SchemeDetailsConnector]

  class Handler[A](request: IdentifierRequest[A]) {

    def run(srn: Srn): Action[AnyContent] = (new FakeActionBuilder(request) andThen allowAccessAction(srn)) {
      request =>
        Ok(Json.toJson(request.schemeDetails))
    }
  }

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

  override def beforeEach() = {
    reset(mockSchemeDetailsConnector, mockMinimalDetailsConnector)

    // setup green path
    setupSchemeDetails(psaId, srn, Future.successful(Some(schemeDetails)))
    setupCheckAssociation(psaId, srn, Future.successful(true))
    setupMinimalDetails(psaId, Future.successful(Right(minimalDetails)))

    setupSchemeDetails(pspId, srn, Future.successful(Some(schemeDetails)))
    setupCheckAssociation(pspId, srn, Future.successful(true))
    setupMinimalDetails(pspId, Future.successful(Right(minimalDetails)))
  }

  val psaId = psaIdGen.sample.value
  val pspId = pspIdGen.sample.value
  val schemeDetails = schemeDetailsGen.sample.value.copy(schemeStatus = validSchemeStatusGen.sample.value)
  val minimalDetails = minimalDetailsGen.sample.value.copy(rlsFlag = false, deceasedFlag = false)
  val srn = srnGen.sample.value
  val administratorRequest = AdministratorRequest("", "", FakeRequest(), psaId)
  val practitionerRequest = PractitionerRequest("", "", FakeRequest(), pspId)

  "AllowAccessAction" should {

    "return ok" when {

      "psa is associated, no rls flag, no deceased flag, no DelimitedAdmin and a valid status" in {
        val handler = new Handler(administratorRequest)

        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(schemeDetails)
      }

      "psp is associated, no rls flag, no deceased flag, no DelimitedAdmin and a valid status" in {
        val handler = new Handler(practitionerRequest)

        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(schemeDetails)
      }
    }

    "return unauthorized" when {

      "psa check association returns false" in {
        setupCheckAssociation(psaId, srn, Future.successful(false))

        val handler = new Handler(administratorRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psp check association returns false" in {
        setupCheckAssociation(pspId, srn, Future.successful(false))

        val handler = new Handler(practitionerRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psa rls flag is set" in {
        setupMinimalDetails(psaId, Future.successful(Right(minimalDetails.copy(rlsFlag = true))))

        val handler = new Handler(administratorRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psp rls flag is set" in {
        setupMinimalDetails(pspId, Future.successful(Right(minimalDetails.copy(rlsFlag = true))))

        val handler = new Handler(practitionerRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psa deceased flag is set" in {
        setupMinimalDetails(psaId, Future.successful(Right(minimalDetails.copy(deceasedFlag = true))))

        val handler = new Handler(administratorRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psp deceased flag is set" in {
        setupMinimalDetails(pspId, Future.successful(Right(minimalDetails.copy(deceasedFlag = true))))

        val handler = new Handler(practitionerRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psa minimal details return delimited admin" in {
        setupMinimalDetails(psaId, Future.successful(Left(DelimitedAdmin)))

        val handler = new Handler(administratorRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psp minimal details return delimited admin" in {
        setupMinimalDetails(pspId, Future.successful(Left(DelimitedAdmin)))

        val handler = new Handler(practitionerRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psa minimal details return not found" in {
        setupMinimalDetails(psaId, Future.successful(Left(DetailsNotFound)))

        val handler = new Handler(administratorRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psp minimal details return not found" in {
        setupMinimalDetails(pspId, Future.successful(Left(DetailsNotFound)))

        val handler = new Handler(practitionerRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psa - scheme details not found" in {
        setupSchemeDetails(psaId, srn, Future.successful(None))

        val handler = new Handler(administratorRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psp - scheme details not found" in {
        setupSchemeDetails(pspId, srn, Future.successful(None))

        val handler = new Handler(practitionerRequest)
        val result = handler.run(srn)(FakeRequest())

        status(result) mustBe UNAUTHORIZED
      }

      "psa - scheme has an invalid status" in {
        forAll(invalidSchemeStatusGen) { schemeStatus =>
          setupSchemeDetails(psaId, srn, Future.successful(Some(schemeDetails.copy(schemeStatus = schemeStatus))))

          val handler = new Handler(administratorRequest)
          val result = handler.run(srn)(FakeRequest())

          status(result) mustBe UNAUTHORIZED
        }
      }

      "psp - scheme has an invalid status" in {
        forAll(invalidSchemeStatusGen) { schemeStatus =>
          setupSchemeDetails(pspId, srn, Future.successful(Some(schemeDetails.copy(schemeStatus = schemeStatus))))

          val handler = new Handler(practitionerRequest)
          val result = handler.run(srn)(FakeRequest())

          status(result) mustBe UNAUTHORIZED
        }
      }
    }
  }

}