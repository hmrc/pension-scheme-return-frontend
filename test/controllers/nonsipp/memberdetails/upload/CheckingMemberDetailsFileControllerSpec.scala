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
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import views.html.ContentPageView
import play.api.inject
import pages.nonsipp.memberdetails.upload.UploadStatusPage
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, when}
import models._
import controllers.nonsipp.memberdetails.upload.CheckingMemberDetailsFileController._

import scala.concurrent.Future

class CheckingMemberDetailsFileControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private lazy val onPageLoad = routes.CheckingMemberDetailsFileController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.CheckingMemberDetailsFileController.onSubmit(srn, NormalMode)

  private val mockUploadService = mock[UploadService]

  override val additionalBindings: List[GuiceableModule] = List(
    inject.bind[UploadService].toInstance(mockUploadService)
  )

  override def beforeEach(): Unit =
    reset(mockUploadService)

  "CheckingMemberDetailsFileController" - {

    act.like(
      renderView(
        onPageLoad,
        defaultUserAnswers.unsafeSet(UploadStatusPage(srn), UploadSubmitted)
      ) { implicit app => implicit request =>
        injected[ContentPageView].apply(viewModel(srn, NormalMode))
      }.before(mockGetUploadResult(Some(uploadResultSuccess)))
    )

    act.like(
      redirectToPage(onSubmit, routes.FileUploadSuccessController.onPageLoad(srn, NormalMode))
        .before(mockGetUploadResult(Some(uploadResultSuccess)))
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }

  private def mockGetUploadResult(uploadStatus: Option[Upload]): Unit =
    when(mockUploadService.getUploadResult(any())).thenReturn(Future.successful(uploadStatus))
}
