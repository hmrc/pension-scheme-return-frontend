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

import controllers.ControllerBaseSpec
import views.html.ContentPageView
import controllers.nonsipp.memberdetails.upload.FileUploadErrorMissingInformationController._
import models.NormalMode

class FileUploadErrorMissingInformationControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.FileUploadErrorMissingInformationController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.FileUploadErrorMissingInformationController.onSubmit(srn, NormalMode)

  "FileUploadErrorMissingInformationController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[ContentPageView].apply(viewModel(srn, NormalMode))
    })

    act.like(redirectNextPage(onSubmit))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
