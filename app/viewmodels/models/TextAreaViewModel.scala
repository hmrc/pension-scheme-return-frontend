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

package viewmodels.models

import config.Constants
import viewmodels.DisplayMessage.Message

case class TextAreaViewModel(
  rows: Int = 5,
  hint: Option[Message] = None,
  maxLength: Int = Constants.maxTextAreaLength
) {
  def withHint(message: Message): TextAreaViewModel =
    copy(hint = Some(message))

  def withMaxLength(maxLength: Int): TextAreaViewModel =
    copy(maxLength = maxLength)
}
