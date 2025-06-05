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

package forms

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import forms.mappings.Mappings
import generators.Generators
import cats.implicits._
import eu.timepit.refined.refineMV
import org.scalacheck.Gen
import org.scalatest.OptionValues
import uk.gov.hmrc.time.TaxYear
import play.api.data.{Form, FormError}
import forms.mappings.errors.DateFormErrors
import utils.DateTimeUtils.localDateShow
import models.DateRange

import java.time.LocalDate

class DateRangeMappingsSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with Generators
    with OptionValues
    with Mappings {

  val dateFormErrors: DateFormErrors = DateFormErrors(
    required = "error.required.all",
    requiredDay = "error.required.day",
    requiredMonth = "error.required.month",
    requiredYear = "error.required.year",
    requiredTwo = "error.required.two",
    invalidDate = "error.invalid.date",
    invalidCharacters = "error.invalid.characters"
  )

  val defaultTaxYear: TaxYear = TaxYear(2022)
  val start: LocalDate = defaultTaxYear.starts
  val end: LocalDate = defaultTaxYear.finishes

  val form: Form[DateRange] = formWithDuplicates(Nil)

  val allowedDateRange: DateRange = DateRange(LocalDate.of(2021, 4, 6), end)

  def formWithDuplicates(duplicateRanges: List[DateRange]): Form[DateRange] = Form(
    "value" -> dateRange(
      startDateErrors = dateFormErrors,
      endDateErrors = dateFormErrors,
      invalidRangeError = "error.invalid.range",
      allowedRange = DateRange(start, end),
      startDateAllowedDateRangeError = "error.startDate.outsideRange",
      endDateAllowedDateRangeError = "error.endDate.outsideRange",
      overlappedStartDateError = "error.overlapped.start",
      overlappedEndDateError = "error.overlapped.end",
      duplicateRanges = duplicateRanges,
      previousDateRangeError = Some("error.previousStartDate"),
      index = refineMV(1),
      taxYear = defaultTaxYear,
      errorStartBefore = "error.startBefore",
      errorStartAfter = "error.startAfter",
      errorEndBefore = "error.endBefore",
      errorEndAfter = "error.endAfter"
    )
  )

  val validDate: Gen[LocalDate] = datesBetween(
    min = allowedDateRange.from.plusDays(1),
    max = allowedDateRange.to.minusDays(1)
  )

  val invalidStartDate: Gen[LocalDate] = datesBetween(earliestDate, allowedDateRange.from.minusDays(1))
  val invalidEndDate: Gen[LocalDate] = datesBetween(allowedDateRange.to.plusDays(1), latestDate)
  val invalidDate: Gen[LocalDate] = Gen.oneOf(invalidStartDate, invalidEndDate)

  def range(date: Gen[LocalDate]): Gen[DateRange] =
    for {
      startDate <- date
      endDate <- date
    } yield
      if (startDate.isBefore(endDate)) DateRange(startDate, endDate)
      else DateRange(endDate, startDate)

  val invalidField: Gen[String] = Gen.alphaStr.suchThat(_.nonEmpty)

  val missingField: Gen[Option[String]] = Gen.option(Gen.const(""))

  def makeData(key: String, date: LocalDate): Map[String, String] =
    Map(
      s"value.$key.day" -> date.getDayOfMonth.toString,
      s"value.$key.month" -> date.getMonthValue.toString,
      s"value.$key.year" -> date.getYear.toString
    )

  def makeData(startDate: LocalDate, endDate: LocalDate): Map[String, String] =
    makeData("startDate", startDate) ++ makeData("endDate", endDate)

  def makeData(range: DateRange): Map[String, String] =
    makeData(range.from, range.to)

  "must bind valid data" in {

    val range = DateRange(start.plusDays(1), end)
    val data = makeData(range)

    val result = form.bind(data)

    result.errors mustEqual Nil
    result.value.value mustEqual range
  }

  "must fail to bind an empty date" in {

    val result = form.bind(Map.empty[String, String])

    result.errors must contain allElementsOf List(
      FormError("value.startDate", "error.required.all"),
      FormError("value.endDate", "error.required.all")
    )
  }

  "must fail to bind if start date is before allowed date range" in {
    val range = DateRange(allowedDateRange.from.minusDays(1), defaultTaxYear.finishes)
    val data = makeData(range.from, range.to)
    val result = form.bind(data)

    result.errors must contain only
      FormError("value.startDate", "error.startAfter", List(defaultTaxYear.starts.show))
  }

  "must fail to bind if end date is before the start of the tax year" in {
    val range = DateRange(defaultTaxYear.starts, defaultTaxYear.finishes.plusDays(1))
    val data = makeData(range.from, range.to)
    val result = form.bind(data)

    result.errors must contain only FormError(
      "value.endDate",
      "error.endBefore",
      List(defaultTaxYear.starts.show, defaultTaxYear.finishes.show)
    )
  }

  "must fail to bind if end date is after end of tax year" in {
    val range = DateRange(defaultTaxYear.starts, defaultTaxYear.finishes.plusDays(1))
    val data = makeData(range.from, range.to)
    val result = form.bind(data)

    result.errors must contain only FormError(
      "value.endDate",
      "error.endBefore",
      List(defaultTaxYear.starts.show, defaultTaxYear.finishes.show)
    )
  }

  "must fail to bind if date range intersects another date range" in {
    val range = DateRange(start.plusDays(1), end.minusDays(1))
    val data = makeData(range)
    val excludedRanges = List(allowedDateRange)

    val result = formWithDuplicates(excludedRanges).bind(data)

    result.errors must contain only
      FormError("value.startDate", "error.overlapped.start", List(allowedDateRange.from.show, allowedDateRange.to.show))
  }

  "must unbind a date" in {

    forAll(range(validDate) -> "valid date") { date =>
      val filledForm = form.fill(date)

      filledForm("value.startDate.day").value.value mustEqual date.from.getDayOfMonth.toString
      filledForm("value.startDate.month").value.value mustEqual date.from.getMonthValue.toString
      filledForm("value.startDate.year").value.value mustEqual date.from.getYear.toString
      filledForm("value.endDate.day").value.value mustEqual date.to.getDayOfMonth.toString
      filledForm("value.endDate.month").value.value mustEqual date.to.getMonthValue.toString
      filledForm("value.endDate.year").value.value mustEqual date.to.getYear.toString
    }
  }
}
