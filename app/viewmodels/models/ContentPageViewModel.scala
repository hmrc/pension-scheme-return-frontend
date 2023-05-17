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
import viewmodels.DisplayMessage._
import viewmodels.implicits._

case class ContentPageViewModel(
  contents: List[BlockMessage],
  isStartButton: Boolean,
  isLargeHeading: Boolean
)

object ContentPageViewModel {

  def startPage(
    title: String,
    heading: String,
    contents: List[String],
    onSubmit: Call
  ): PageViewModel[ContentPageViewModel] =
    PageViewModel(
      title,
      heading,
      ContentPageViewModel(contents.map(ParagraphMessage(_)), isStartButton = true, isLargeHeading = false),
      onSubmit
    ).withButtonText("site.start")

}
