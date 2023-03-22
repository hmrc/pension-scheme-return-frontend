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

import forms.FormMapping
import viewmodels.DisplayMessage.InlineMessage

sealed trait MultipleQuestionsViewModel[A, B] {

  val mapping: FormMapping[A, B]

  def fields: List[Field]
}

object MultipleQuestionsViewModel {

  case class SingleQuestion[A](
    mapping: FormMapping[A, A]
  ) extends MultipleQuestionsViewModel[A, A] {

    override val fields: List[Field] = List()
  }

  case class DoubleQuestion[A, B](
    mapping: FormMapping[(A, A), B],
    field1: Field,
    field2: Field
  ) extends MultipleQuestionsViewModel[(A, A), B] {

    override val fields: List[Field] = List(field1, field2)
  }

  case class TripleQuestion[A, B](
    mapping: FormMapping[(A, A, A), B],
    field1: Field,
    field2: Field,
    field3: Field
  ) extends MultipleQuestionsViewModel[(A, A, A), B] {

    override val fields: List[Field] = List(field1, field2)
  }
}

case class Field(label: InlineMessage, hint: Option[InlineMessage])
