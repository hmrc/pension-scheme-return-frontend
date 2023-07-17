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

import play.twirl.api.Html
import viewmodels.DisplayMessage.Message

case class ConditionalYesNoPageViewModel(
  legend: Option[Message] = None,
  hint: Option[Message] = None,
  yes: YesNoViewModel,
  no: YesNoViewModel,
  details: Option[FurtherDetailsViewModel] = None
) {

  def withHint(message: Message): ConditionalYesNoPageViewModel =
    copy(hint = Some(message))
}

sealed trait YesNoViewModel {
  val hint: Option[Html]
  val message: Option[Html]
}

object YesNoViewModel {

  case class Unconditional(message: Option[Html], hint: Option[Html]) extends YesNoViewModel

  object Unconditional extends Unconditional(None, None)

  case class Conditional(message: Option[Html], hint: Option[Html], conditionalMessage: Message, fieldType: FieldType)
      extends YesNoViewModel

  object Conditional {
    def apply(nestedHtml: Message, fieldType: FieldType): Conditional = Conditional(None, None, nestedHtml, fieldType)
  }
}
