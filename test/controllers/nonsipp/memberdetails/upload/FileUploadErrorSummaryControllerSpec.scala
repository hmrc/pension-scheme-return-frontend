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

package controllers.nonsipp.memberdetails.upload

import services.UploadService
import controllers.nonsipp.memberdetails.upload.FileUploadErrorSummaryController._
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.ContentPageView
import play.api.inject
import models.{NormalMode, Upload, UploadFormatError}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, when}

import scala.concurrent.Future

class FileUploadErrorSummaryControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val mockUploadService = mock[UploadService]

  override val additionalBindings: List[GuiceableModule] = List(
    inject.bind[UploadService].toInstance(mockUploadService)
  )

  override def beforeEach(): Unit =
    reset(mockUploadService)

  private lazy val onPageLoad = routes.FileUploadErrorSummaryController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.FileUploadErrorSummaryController.onSubmit(srn, NormalMode)

  "FileUploadErrorSummaryController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ContentPageView].apply(viewModel(srn, uploadResultErrors.errors, NormalMode))
    }.before(mockGetUploadResult(Some(uploadResultErrors))))

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(mockGetUploadResult(None))
        .updateName("onPageLoad when upload result doesn't exist" + _)
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(mockGetUploadResult(Some(uploadResultSuccess)))
        .updateName("onPageLoad when upload result is a success" + _)
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(mockGetUploadResult(Some(UploadFormatError)))
        .updateName("onPageLoad when upload result is a format error" + _)
    )

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  private def mockGetUploadResult(upload: Option[Upload]): Unit =
    when(mockUploadService.getUploadResult(any())).thenReturn(Future.successful(upload))
}
