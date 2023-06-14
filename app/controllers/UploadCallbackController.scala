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

package controllers

import models._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, MessagesControllerComponents}
import services.UploadService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

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
        case _: FailedCallbackBody => UploadStatus.Failed
      }
      uploadService.registerUploadResult(callback.reference, uploadStatus).map(_ => Ok)
    }
  }
}
