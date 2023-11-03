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

package services

import akka.stream.scaladsl.Sink
import akka.util.ByteString
import connectors.UpscanConnector
import controllers.TestValues
import models._
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status.OK
import repositories.UploadRepository
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.BaseSpec

import java.time.temporal.ChronoUnit
import java.time.{Clock, Instant, ZoneId}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UploadServiceSpec extends BaseSpec with ScalaCheckPropertyChecks with TestValues {

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  private val mockUpscanConnector = mock[UpscanConnector]
  private val mockUploadRepository = mock[UploadRepository]

  val instant: Instant = Instant.now.truncatedTo(ChronoUnit.MILLIS)
  private val failure: UploadStatus.Failed = UploadStatus.Failed(ErrorDetails("reason", "message"))
  private val uploadDetails = UploadDetails(uploadKey, reference, failure, instant)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpscanConnector)
    reset(mockUploadRepository)
  }

  val service = new UploadService(mockUpscanConnector, mockUploadRepository, stubClock)

  "UploadService" - {
    "initiateUpscan should return what the connector returns" in {
      val expected = UpscanInitiateResponse(UpscanFileReference("test-ref"), "post-target", Map("test" -> "fields"))
      when(mockUpscanConnector.initiate(any(), any(), any())(any())).thenReturn(Future.successful(expected))
      val result = service.initiateUpscan("callback-url", "success-url", "failure-url")

      result.futureValue mustBe expected
    }

    "registerUploadRequest should call remove and insert" in {
      when(mockUploadRepository.remove(any())).thenReturn(Future.successful((): Unit))
      when(mockUploadRepository.insert(any())).thenReturn(Future.successful((): Unit))
      val result =
        service.registerUploadRequest(uploadKey, reference)
      result.futureValue mustBe (())
    }

    "registerUploadResult should call updateStatus and return unit" in {
      when(mockUploadRepository.updateStatus(any(), any())).thenReturn(Future.successful(UploadStatus.Failed))
      val result = service.registerUploadResult(reference, failure)
      result.futureValue mustBe (())
    }

    "getUploadStatus return the status from the connector" in {
      when(mockUploadRepository.getUploadDetails(any())).thenReturn(Future.successful(Some(uploadDetails)))
      val result = service.getUploadStatus(uploadKey)
      result.futureValue mustBe Some(failure)
    }

    "getUploadResult return the status from the connector" in {
      when(mockUploadRepository.getUploadResult(any())).thenReturn(Future.successful(Some(uploadResultSuccess)))
      val result = service.getUploadResult(uploadKey)
      result.futureValue mustBe Some(uploadResultSuccess)
    }

    "stream should return the upscan download http response body as a stream" in {
      val httpResponseBody = "test body"
      when(mockUpscanConnector.download(any())(any())).thenReturn(Future.successful(HttpResponse(OK, httpResponseBody)))
      val result = service.stream("/test-download-url")
      result.flatMap(_.runWith(Sink.seq).map(_.toList)).futureValue mustBe List(ByteString(httpResponseBody))
    }
  }
}
