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

import play.api.data.Form
import viewmodels.DisplayMessage.InlineMessage

sealed trait MultipleQuestionsViewModel[A] {

  val form: Form[A]

  def firstField: Field
  def fields: List[Field]
}

object MultipleQuestionsViewModel {

  case class SingleQuestion[A](
    form: Form[A],
    field1: Field
  ) extends MultipleQuestionsViewModel[A] {

    override def firstField: Field = field1
    override val fields: List[Field] = List(field1)
  }

  case class DoubleQuestion[A](
    form: Form[(A, A)],
    field1: Field,
    field2: Field
  ) extends MultipleQuestionsViewModel[(A, A)] {

    override def firstField: Field = field1
    override val fields: List[Field] = List(field1, field2)
  }

  case class TripleQuestion[A](
    form: Form[(A, A, A)],
    field1: Field,
    field2: Field,
    field3: Field
  ) extends MultipleQuestionsViewModel[(A, A, A)] {

    override def firstField: Field = field1
    override val fields: List[Field] = List(field1, field2, field3)
  }
}

case class Field(label: InlineMessage, hint: Option[InlineMessage] = None)
