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

sealed trait FieldType

object FieldType {
  case object Input extends FieldType

  case class ConditionalInput(prefix: String) extends FieldType
  case object Currency extends FieldType
  case object Date extends FieldType
  case object Percentage extends FieldType
  case object Textarea extends FieldType
  case object Numeric extends FieldType

  case class ConditionalTextarea(prefix: String) extends FieldType

  case object Security extends FieldType
  case object Select extends FieldType
}
