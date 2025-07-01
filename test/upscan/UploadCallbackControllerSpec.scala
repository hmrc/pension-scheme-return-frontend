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

package upscan

import play.api.test.FakeRequest
import services.UploadService
import play.api.mvc.MessagesControllerComponents
import controllers.ControllerBaseSpec
import play.api.libs.json.{JsValue, Json}
import models._
import org.mockito.ArgumentMatchers.any
import play.api.test.Helpers._
import org.mockito.Mockito.{reset, verify, when}
import org.mockito.ArgumentMatchers
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class UploadCallbackControllerSpec extends ControllerBaseSpec with MockitoSugar {

  private val mockUploadService: UploadService = mock[UploadService]
  private val controllerComponents: MessagesControllerComponents = stubMessagesControllerComponents()

  private val controller = UploadCallbackController(mockUploadService, controllerComponents)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUploadService)
  }

  "UploadCallbackController" - {

    "handle a ReadyCallbackBody and return OK" in {

      val jsonBody: JsValue = Json.parse(s"""
           |{
           |  "reference": "some-reference",
           |  "downloadUrl": "http://download.url",
           |  "fileStatus": "READY",
           |  "uploadDetails": {
           |    "uploadTimestamp": "2023-10-14T12:00:00Z",
           |    "checksum": "checksum-value",
           |    "fileMimeType": "application/pdf",
           |    "fileName": "test.pdf",
           |    "size": 12345
           |  }
           |}
           |""".stripMargin)

      when(mockUploadService.registerUploadResult(any(), any())).thenReturn(Future.successful(()))

      val request = FakeRequest("POST", "/upload-callback")
        .withBody(jsonBody)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.callback()(request)

      status(result) mustBe OK

      val expectedUploadStatus = UploadStatus.Success(
        name = "test.pdf",
        mimeType = "application/pdf",
        downloadUrl = "http://download.url",
        size = Some(12345L)
      )

      verify(mockUploadService).registerUploadResult(
        ArgumentMatchers.eq(Reference("some-reference")),
        ArgumentMatchers.eq(expectedUploadStatus)
      )
    }

    "handle a FailedCallbackBody and return OK" in {

      val jsonBody: JsValue = Json.parse(s"""
           |{
           |  "reference": "some-reference",
           |  "fileStatus": "FAILED",
           |  "failureDetails": {
           |    "failureReason": "QUARANTINE",
           |    "message": "This file has a virus"
           |  }
           |}
           |""".stripMargin)

      when(mockUploadService.registerUploadResult(any(), any())).thenReturn(Future.successful(()))

      val request = FakeRequest("POST", "/upload-callback")
        .withBody(jsonBody)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.callback()(request)

      status(result) mustBe OK

      val expectedFailureDetails = ErrorDetails(
        failureReason = "QUARANTINE",
        message = "This file has a virus"
      )

      val expectedUploadStatus = UploadStatus.Failed(expectedFailureDetails)

      verify(mockUploadService).registerUploadResult(
        ArgumentMatchers.eq(Reference("some-reference")),
        ArgumentMatchers.eq(expectedUploadStatus)
      )
    }

    "return BadRequest when invalid JSON is sent" in {

      val invalidJson: JsValue = Json.parse(s"""
           |{
           |  "invalidField": "some value"
           |}
           |""".stripMargin)

      val request = FakeRequest("POST", "/upload-callback")
        .withBody(invalidJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.callback()(request)

      status(result) mustBe BAD_REQUEST
    }
  }
}
