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

package models

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import java.time.format.DateTimeFormatter

case class DateRange(from: LocalDate, to: LocalDate) {

  def intersects(range: DateRange): Boolean =
    contains(range.from) || contains(range.to)

  def contains(date: LocalDate): Boolean =
    !date.isBefore(from) && !date.isAfter(to)

  private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
  override def toString: String =
    s"${from.format(formatter)}-${to.format(formatter)}"
}

object DateRange {

  def from(taxYear: TaxYear): DateRange = DateRange(taxYear.starts, taxYear.finishes)

  implicit val format: Format[DateRange] = Json.format[DateRange]

  implicit val ordering: Ordering[DateRange] = (x: DateRange, y: DateRange) => {
    Ordering[LocalDate].compare(x.to, y.to)
  }
}
