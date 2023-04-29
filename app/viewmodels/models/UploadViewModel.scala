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

package viewmodels.models

import play.api.mvc.Call
import uk.gov.hmrc.play.bootstrap.graphite.DisabledMetrics
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.Message

case class UploadViewModel(
  title: Message,
  heading: DisplayMessage,
  content: DisplayMessage,
  detailsContent: DisplayMessage,
  acceptedFileType: String,
  maxFileSize: String,
  formFields: Map[String, String],
  error: Option[String],
  onSubmit: Call
)
