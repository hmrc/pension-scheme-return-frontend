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

import viewmodels.{DisplayMessage, InputWidth}

case class TextInputViewModel(
  label: Option[DisplayMessage],
  isFixedLength: Boolean,
  inputWidth: Option[InputWidth] = None
)

object TextInputViewModel {

  def apply(): TextInputViewModel = TextInputViewModel(None, false, None)

  def apply(isFixedLength: Boolean): TextInputViewModel = TextInputViewModel(None, isFixedLength)

  def withWidth(inputWidth: Option[InputWidth]): TextInputViewModel =
    TextInputViewModel(None, true, inputWidth)
}
