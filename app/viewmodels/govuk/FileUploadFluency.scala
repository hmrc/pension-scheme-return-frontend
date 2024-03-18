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

package viewmodels.govuk

import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.Aliases.FileUpload
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import play.twirl.api.Html
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import play.api.data.FormError

object fileupload extends FileUploadFluency

trait FileUploadFluency {

  object FileUploadViewModel {
    def csv(label: Html): FileUpload =
      FileUpload(
        name = "file",
        id = "file-input",
        label = Label(content = HtmlContent(label)),
        attributes = Map("accept" -> ".csv,application/csv")
      )
  }

  implicit class FileUploadFluency(fileUpload: FileUpload) {
    def withError(msg: Option[FormError])(implicit messages: Messages): FileUpload = {

      val errorMessage = msg.map(err => messages(err.message, err.args: _*))

      fileUpload.copy(errorMessage = errorMessage.map(err => ErrorMessage(content = Text(err))))
    }
  }
}
