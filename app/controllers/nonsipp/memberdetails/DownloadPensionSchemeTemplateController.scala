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

package controllers.nonsipp.memberdetails

import models.SchemeId.Srn
import play.api.Configuration
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import scala.concurrent.ExecutionContext

import javax.inject.Inject

class DownloadPensionSchemeTemplateController @Inject()(config: Configuration, cc: ControllerComponents)(
  implicit ec: ExecutionContext
) extends AbstractController(cc) {

  def downloadFile(srn: Srn): Action[AnyContent] = Action {
    Ok.sendFile(
      content = new java.io.File("conf/pension-scheme-return-member-details-template.csv"),
      fileName = _ => Option("pension-scheme-return-member-details-template.csv")
    )
  }
}
