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

  case class TripleQuestion[A, B, C](
    form: Form[(A, B, C)],
    field1: QuestionField,
    field2: QuestionField,
    field3: QuestionField
  ) extends MultipleQuestionsViewModel[(A, B, C)] {

    override def firstField: QuestionField = field1

    override val fields: List[QuestionField] = List(field1, field2, field3)
  }
}

case class QuestionField(label: InlineMessage, hint: Option[InlineMessage] = None, fieldType: FieldType)

object QuestionField {
  def input(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    QuestionField(label, hint, FieldType.Input)

  def currency(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    QuestionField(label, hint, FieldType.Currency)

  // alias for currency
  def money(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    currency(label, hint)

  def date(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    QuestionField(label, hint, FieldType.Date)

  // alias for date
  def localDate(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    date(label, hint)

  def percentage(label: InlineMessage, hint: Option[InlineMessage] = None): QuestionField =
    QuestionField(label, hint, FieldType.Percentage)
}
