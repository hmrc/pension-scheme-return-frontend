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

package forms

import cats.implicits._
import forms.mappings.{DateFormErrors, Mappings}
import generators.Generators
import models.DateRange
import org.scalacheck.Gen
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form, FormError}
import utils.DateTimeUtils.localDateShow

import java.time.LocalDate

class DateRangeMappingsSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with Generators with OptionValues
  with Mappings {

  val allowedRange = DateRange(
    LocalDate.of(2000, 1, 1),
    LocalDate.of(3000, 1, 1)
  )

  val dateFormErrors = DateFormErrors(
    required = "error.required.all",
    requiredDay = "error.required.day",
    requiredMonth = "error.required.month",
    requiredYear = "error.required.year",
    requiredTwo = "error.required.two",
    invalidDate = "error.invalid.date",
    invalidCharacters = "error.invalid.characters"
  )

  val form = formWithDuplicates(Nil)

  def formWithDuplicates(duplicateRanges: List[DateRange]) = Form(
    "value" -> dateRange(
      startDateErrors = dateFormErrors,
      endDateErrors = dateFormErrors,
      invalidRangeError = "error.invalid.range",
      allowedRange = Some(allowedRange),
      startDateAllowedDateRangeError = Some("error.startDate.outsideRange"),
      endDateAllowedDateRangeError = Some("error.endDate.outsideRange"),
      duplicateRangeError = Some("error.duplicate"),
      duplicateRanges = duplicateRanges
    )
  )

  val validDate = datesBetween(
    min = allowedRange.from,
    max = allowedRange.to
  )

  val invalidStartDate = datesBetween(earliestDate, allowedRange.from)
  val invalidEndDate = datesBetween(allowedRange.to, latestDate)
  val invalidDate = Gen.oneOf(invalidStartDate, invalidEndDate)

  def range(date: Gen[LocalDate]) =
    for {
      startDate <- date
      endDate   <- date
    } yield {
      if(startDate.isBefore(endDate)) DateRange(startDate, endDate)
      else DateRange(endDate, startDate)
    }

  val invalidField: Gen[String] = Gen.alphaStr.suchThat(_.nonEmpty)

  val missingField: Gen[Option[String]] = Gen.option(Gen.const(""))

  def makeData(key: String, date: LocalDate): Map[String, String] = {
    Map(
      s"value.$key.day" -> date.getDayOfMonth.toString,
      s"value.$key.month" -> date.getMonthValue.toString,
      s"value.$key.year" -> date.getYear.toString,
    )
  }

  def makeData(startDate: LocalDate, endDate: LocalDate): Map[String, String] = {
    makeData("startDate", startDate) ++ makeData("endDate", endDate)
  }

  def makeData(range: DateRange): Map[String, String] =
    makeData(range.from, range.to)

  "must bind valid data" in {

    forAll(range(validDate) -> "valid date") {
      range =>

        val data = makeData(range)

        val result = form.bind(data)

        result.value.value mustEqual range
    }
  }

  "must fail to bind an empty date" in {

    val result = form.bind(Map.empty[String, String])

    result.errors must contain allElementsOf List(
      FormError("value.startDate", "error.required.all"),
      FormError("value.endDate", "error.required.all")
    )
  }

  "must fail to bind if end date is before start date" in {

    forAll(range(validDate) -> "valid range") { range =>

      val data = makeData(range.to, range.from)

      val result = form.bind(data)

      result.errors must contain only
        FormError("value.startDate", "error.invalid.range", List(range.to.show, range.from.show))
    }
  }

  "must fail to bind if start date is outside date range" in {

    forAll(invalidStartDate -> "invalid start date", validDate -> "valid end date") { (startDate, endDate) =>


      val data = makeData(startDate, endDate)

      val result = form.bind(data)

      result.errors must contain only
        FormError(
          "value.startDate",
          "error.startDate.outsideRange",
          List(startDate.show, allowedRange.from.show, allowedRange.to.show)
        )
    }
  }

  "must fail to bind if end date is outside date range" in {

    forAll(validDate -> "valid start date", invalidEndDate -> "invalid end date") { (startDate, endDate) =>

      val data = makeData(startDate, endDate)

      val result = form.bind(data)

      result.errors must contain only
        FormError(
          "value.endDate",
          "error.endDate.outsideRange",
          List(endDate.show, allowedRange.from.show, allowedRange.to.show)
        )
    }
  }

  "must fail to bind if start and end date are outide range" in {

    forAll(range(invalidDate) -> "invalid range") { range =>

      val data = makeData(range)

      val result = form.bind(data)

      val expectedStartDateError = FormError(
        "value.startDate",
        "error.startDate.outsideRange",
        List(range.from.show, allowedRange.from.show, allowedRange.to.show)
      )

      val expectedEndDateError = FormError(
        "value.endDate",
        "error.endDate.outsideRange",
        List(range.to.show, allowedRange.from.show, allowedRange.to.show)
      )

      result.errors must contain allElementsOf List(
        expectedStartDateError,
        expectedEndDateError
      )

    }
  }

  "must fail to bind if date range intersects another date range" in {

    forAll(range(validDate)) { range =>

      val data = makeData(range)
      val excludedRanges = List(allowedRange)

      val result = formWithDuplicates(excludedRanges).bind(data)

      result.errors must contain only FormError("value.startDate", "error.duplicate")
    }
  }

  "must unbind a date" in {

    forAll(range(validDate) -> "valid date") {
      date =>

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
