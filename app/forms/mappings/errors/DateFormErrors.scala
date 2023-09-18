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

package forms.mappings.errors

import java.time.LocalDate

case class DateFormErrors(
  required: String,
  requiredDay: String,
  requiredMonth: String,
  requiredYear: String,
  requiredTwo: String,
  invalidDate: String,
  invalidCharacters: String,
  validators: List[LocalDate => Option[String]] = List()
)

object DateFormErrors {
  def failIfFutureDate(errorMsg: String): LocalDate => Option[String] =
    failIfDateAfter(LocalDate.now(), errorMsg)

  def failIfDateAfter(dateAfter: LocalDate, errorMsg: String): LocalDate => Option[String] =
    date => Option.when(date.isAfter(dateAfter))(errorMsg)

  def failIfDateBefore(dateBefore: LocalDate, errorMsg: String): LocalDate => Option[String] =
    date => Option.when(date.isBefore(dateBefore))(errorMsg)
}
