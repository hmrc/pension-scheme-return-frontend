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

package controllers

import services.UploadService
import play.api.mvc.{Action, MessagesControllerComponents}
import play.api.libs.json.JsValue
import models._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class UploadCallbackController @Inject()(
  uploadService: UploadService,
  mcc: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendController(mcc) {

  def callback: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[CallbackBody] { callback: CallbackBody =>
      val uploadStatus = callback match {
        case s: ReadyCallbackBody =>
          UploadStatus.Success(
            s.uploadDetails.fileName,
            s.uploadDetails.fileMimeType,
            s.downloadUrl.toString,
            Some(s.uploadDetails.size)
          )
        case f: FailedCallbackBody => UploadStatus.Failed(f.failureDetails)
      }
      uploadService.registerUploadResult(callback.reference, uploadStatus).map(_ => Ok)
    }
  }
}
