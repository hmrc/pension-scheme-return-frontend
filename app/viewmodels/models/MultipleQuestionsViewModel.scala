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

import models.SelectInput
import viewmodels.InputWidth
import viewmodels.DisplayMessage.{InlineMessage, Message}
import play.api.data.Form

sealed trait MultipleQuestionsViewModel[A] {

  val form: Form[A]

  def firstField: QuestionField
  def fields: List[QuestionField]
}

object MultipleQuestionsViewModel {

  case class SingleQuestion[A](form: Form[A], field1: QuestionField) extends MultipleQuestionsViewModel[A] {

    override def firstField: QuestionField = field1

    override val fields: List[QuestionField] = List(field1)
  }

  case class DoubleQuestion[A](
    form: Form[(A, A)],
    field1: QuestionField,
    field2: QuestionField
  ) extends MultipleQuestionsViewModel[(A, A)] {

    override def firstField: QuestionField = field1

    override val fields: List[QuestionField] = List(field1, field2)
  }

  case class DoubleDifferentQuestion[A, B](
    form: Form[(A, B)],
    field1: QuestionField,
    field2: QuestionField
  ) extends MultipleQuestionsViewModel[(A, B)] {

    override def firstField: QuestionField = field1

    override val fields: List[QuestionField] = List(field1, field2)
  }

  case class TripleQuestion[A, B, C](
    form: Form[(A, B, C)],
    field1: QuestionField,
    field2: QuestionField,
    field3: QuestionField
  ) extends MultipleQuestionsViewModel[(A, B, C)] {

    override def firstField: QuestionField = field1

    override val fields: List[QuestionField] = List(field1, field2, field3)
  }

  case class QuintupleQuestion[A, B, C, D, E](
    form: Form[(A, B, C, D, E)],
    field1: QuestionField,
    field2: QuestionField,
    field3: QuestionField,
    field4: QuestionField,
    field5: QuestionField
  ) extends MultipleQuestionsViewModel[(A, B, C, D, E)] {

    override def firstField: QuestionField = field1

    override val fields: List[QuestionField] = List(field1, field2, field3, field4, field5)
  }

  case class SextupleQuestion[A, B, C, D, E, F](
    form: Form[(A, B, C, D, E, F)],
    field1: QuestionField,
    field2: QuestionField,
    field3: QuestionField,
    field4: QuestionField,
    field5: QuestionField,
    field6: QuestionField
  ) extends MultipleQuestionsViewModel[(A, B, C, D, E, F)] {

    override def firstField: QuestionField = field1

    override val fields: List[QuestionField] = List(field1, field2, field3, field4, field5, field6)
  }
}

case class QuestionField(
  label: InlineMessage,
  hint: Option[InlineMessage] = None,
  width: Option[InputWidth] = None,
  selectSource: Seq[SelectInput],
  fieldType: FieldType
) {
  def withWidth(width: InputWidth): QuestionField = copy(width = Some(width))

  def withHint(message: Message): QuestionField = copy(hint = Some(message))
}

object QuestionField {
  def input(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    QuestionField(label, hint, None, Nil, FieldType.Input)

  def numeric(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    QuestionField(label, hint, None, Nil, FieldType.Numeric)

  def currency(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    QuestionField(label, hint, None, Nil, FieldType.Currency)

  def date(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    QuestionField(label, hint, None, Nil, FieldType.Date)

  def percentage(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    QuestionField(label, hint, None, Nil, FieldType.Percentage)

  def select(label: InlineMessage, hint: Option[InlineMessage] = None, selectSource: Seq[SelectInput]): QuestionField =
    QuestionField(label, hint, None, selectSource, FieldType.Select)
}
