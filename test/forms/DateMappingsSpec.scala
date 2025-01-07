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
import forms.behaviours.FieldBehaviours
import generators.Generators
import org.scalacheck.Gen
import org.scalatest.OptionValues
import play.api.data.{Form, FormError}
import forms.mappings.errors.DateFormErrors

import java.time.LocalDate

class DateMappingsSpec
    extends AnyFreeSpec
    with Matchers
    with ScalaCheckPropertyChecks
    with Generators
    with OptionValues
    with FieldBehaviours
    with Mappings {

  val startDate: LocalDate = LocalDate.of(2000, 1, 1)
  val endDate: LocalDate = LocalDate.of(3000, 1, 1)

  val form: Form[LocalDate] = Form(
    "value" -> localDate(
      DateFormErrors(
        required = "error.required.all",
        requiredDay = "error.required.day",
        requiredMonth = "error.required.month",
        requiredYear = "error.required.year",
        requiredTwo = "error.required.two",
        invalidDate = "error.invalid.date",
        invalidCharacters = "error.invalid.characters",
        validators = List(
          d => Option.when(d.isBefore(startDate))("error.beforeStartDate"),
          d => Option.when(d.isAfter(endDate))("error.afterEndDate")
        )
      )
    )
  )

  val validData: Gen[LocalDate] = datesBetween(
    min = startDate,
    max = endDate
  )

  val invalidField: Gen[String] = Gen.alphaStr.suchThat(_.nonEmpty)

  val missingField: Gen[Option[String]] = Gen.option(Gen.const(""))

  "must bind valid data" in {

    forAll(validData -> "valid date") { date =>
      val data = Map(
        "value.day" -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year" -> date.getYear.toString
      )

      val result = form.bind(data)

      result.value.value mustEqual date
    }
  }

  "must bind valid data with spaces either side of the date" in {

    forAll(validData -> "valid date") { date =>
      val data = Map(
        "value.day" -> s"   ${date.getDayOfMonth.toString}   ",
        "value.month" -> s"   ${date.getMonthValue.toString}   ",
        "value.year" -> s"   ${date.getYear.toString}   "
      )

      val result = form.bind(data)

      result.value.value mustEqual date
    }
  }

  "must fail to bind an empty date" in {

    val result = form.bind(Map.empty[String, String])

    result.errors must contain only FormError("value", "error.required.all", List.empty)
  }

  "must fail to bind a date with a missing day" in {

    forAll(validData -> "valid date", missingField -> "missing field") { (date, field) =>
      val initialData = Map(
        "value.month" -> date.getMonthValue.toString,
        "value.year" -> date.getYear.toString
      )

      val data = field.fold(initialData) { value =>
        initialData + ("value.day" -> value)
      }

      val result = form.bind(data)

      result.errors must contain only FormError("value.day", "error.required.day")
    }
  }

  "must fail to bind a date with an invalid day" in {

    forAll(validData -> "valid date", invalidField -> "invalid field") { (date, field) =>
      val data = Map(
        "value.day" -> field,
        "value.month" -> date.getMonthValue.toString,
        "value.year" -> date.getYear.toString
      )

      val result = form.bind(data)

      result.errors must contain(
        FormError("value.day", "error.invalid.characters", List.empty)
      )
    }
  }

  "must fail to bind a date with a missing month" in {

    forAll(validData -> "valid date", missingField -> "missing field") { (date, field) =>
      val initialData = Map(
        "value.day" -> date.getDayOfMonth.toString,
        "value.year" -> date.getYear.toString
      )

      val data = field.fold(initialData) { value =>
        initialData + ("value.month" -> value)
      }

      val result = form.bind(data)

      result.errors must contain only FormError("value.month", "error.required.month")
    }
  }

  "must fail to bind a date with an invalid month" in {

    forAll(validData -> "valid data", invalidField -> "invalid field") { (date, field) =>
      val data = Map(
        "value.day" -> date.getDayOfMonth.toString,
        "value.month" -> field,
        "value.year" -> date.getYear.toString
      )

      val result = form.bind(data)

      result.errors must contain only FormError("value.month", "error.invalid.characters")
    }
  }

  "must fail to bind a date with a missing year" in {

    forAll(validData -> "valid date", missingField -> "missing field") { (date, field) =>
      val initialData = Map(
        "value.day" -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString
      )

      val data = field.fold(initialData) { value =>
        initialData + ("value.year" -> value)
      }

      val result = form.bind(data)

      result.errors must contain only FormError("value.year", "error.required.year")
    }
  }

  "must fail to bind a date with an invalid year" in {

    forAll(validData -> "valid data", invalidField -> "invalid field") { (date, field) =>
      val data = Map(
        "value.day" -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year" -> field
      )

      val result = form.bind(data)

      result.errors must contain only FormError("value.year", "error.invalid.characters")
    }
  }

  "must fail to bind a date with a missing day and month" in {

    forAll(validData -> "valid date", missingField -> "missing day", missingField -> "missing month") {
      (date, dayOpt, monthOpt) =>
        val day = dayOpt.fold(Map.empty[String, String]) { value =>
          Map("value.day" -> value)
        }

        val month = monthOpt.fold(Map.empty[String, String]) { value =>
          Map("value.month" -> value)
        }

        val data: Map[String, String] = Map(
          "value.year" -> date.getYear.toString
        ) ++ day ++ month

        val result = form.bind(data)

        result.errors must contain allElementsOf List(
          FormError("value.day", "error.required.two", List("date.day.lower", "date.month.lower")),
          FormError("value.month", "error.required.two", List("date.day.lower", "date.month.lower"))
        )
    }
  }

  "must fail to bind a date with a missing day and year" in {

    forAll(validData -> "valid date", missingField -> "missing day", missingField -> "missing year") {
      (date, dayOpt, yearOpt) =>
        val day = dayOpt.fold(Map.empty[String, String]) { value =>
          Map("value.day" -> value)
        }

        val year = yearOpt.fold(Map.empty[String, String]) { value =>
          Map("value.year" -> value)
        }

        val data: Map[String, String] = Map(
          "value.month" -> date.getMonthValue.toString
        ) ++ day ++ year

        val result = form.bind(data)

        result.errors must contain allElementsOf List(
          FormError("value.day", "error.required.two", List("date.day.lower", "date.year.lower")),
          FormError("value.year", "error.required.two", List("date.day.lower", "date.year.lower"))
        )
    }
  }

  "must fail to bind a date with a missing month and year" in {

    forAll(validData -> "valid date", missingField -> "missing month", missingField -> "missing year") {
      (date, monthOpt, yearOpt) =>
        val month = monthOpt.fold(Map.empty[String, String]) { value =>
          Map("value.month" -> value)
        }

        val year = yearOpt.fold(Map.empty[String, String]) { value =>
          Map("value.year" -> value)
        }

        val data: Map[String, String] = Map(
          "value.day" -> date.getDayOfMonth.toString
        ) ++ month ++ year

        val result = form.bind(data)

        result.errors must contain allElementsOf List(
          FormError("value.month", "error.required.two", List("date.month.lower", "date.year.lower")),
          FormError("value.year", "error.required.two", List("date.month.lower", "date.year.lower"))
        )
    }
  }

  "must fail to bind an invalid day and month" in {

    forAll(validData -> "valid date", invalidField -> "invalid day", invalidField -> "invalid month") {
      (date, day, month) =>
        val data = Map(
          "value.day" -> day,
          "value.month" -> month,
          "value.year" -> date.getYear.toString
        )

        val result = form.bind(data)

        result.errors must contain allElementsOf List(
          FormError("value.day", "error.invalid.characters")
        )
    }
  }

  "must fail to bind an invalid day and year" in {

    forAll(validData -> "valid date", invalidField -> "invalid day", invalidField -> "invalid year") {
      (date, day, year) =>
        val data = Map(
          "value.day" -> day,
          "value.month" -> date.getMonthValue.toString,
          "value.year" -> year
        )

        val result = form.bind(data)

        result.errors must contain allElementsOf List(
          FormError("value.day", "error.invalid.characters")
        )
    }
  }

  "must fail to bind an invalid month and year" in {

    forAll(validData -> "valid date", invalidField -> "invalid month", invalidField -> "invalid year") {
      (date, month, year) =>
        val data = Map(
          "value.day" -> date.getDayOfMonth.toString,
          "value.month" -> month,
          "value.year" -> year
        )

        val result = form.bind(data)

        result.errors must contain allElementsOf List(
          FormError("value.month", "error.invalid.characters")
        )
    }
  }

  "must fail to bind an invalid day, month and year" in {

    forAll(invalidField -> "invalid day", invalidField -> "invalid month", invalidField -> "invalid year") {
      (day, month, year) =>
        val data = Map(
          "value.day" -> day,
          "value.month" -> month,
          "value.year" -> year
        )

        val result = form.bind(data)

        result.errors must contain allElementsOf List(
          FormError("value.day", "error.invalid.characters")
        )
    }
  }

  "must fail to bind an invalid date" in {

    val data = Map(
      "value.day" -> "30",
      "value.month" -> "2",
      "value.year" -> "2018"
    )

    val result = form.bind(data)

    result.errors must contain(
      FormError("value", "error.invalid.date", List.empty)
    )
  }

  "must fail to bind a date before start date" in {

    val invalidDate = datesBetween(earliestDate, startDate.minusDays(1))

    forAll(invalidDate -> "invalid date") { date =>
      val data = Map(
        "value.day" -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year" -> date.getYear.toString
      )

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.beforeStartDate")
    }
  }

  "must fail to bind a date after end date" in {

    val invalidDate = datesBetween(endDate.plusDays(1), latestDate)

    forAll(invalidDate -> "invalid date") { date =>
      val data = Map(
        "value.day" -> date.getDayOfMonth.toString,
        "value.month" -> date.getMonthValue.toString,
        "value.year" -> date.getYear.toString
      )

      val result = form.bind(data)

      result.errors must contain only FormError("value", "error.afterEndDate")
    }
  }

  "must fail to bind with only a missing year if there are multiple errors (prioritization)" in {

    val data = Map(
      "value.day" -> "xx",
      "value.month" -> "42"
    )

    val result = form.bind(data)
    result.errors must contain only FormError("value.year", "error.required.year")
  }

  "must fail to bind with only an invalid day if there are multiple errors (prioritization)" in {

    val data = Map(
      "value.day" -> "xx",
      "value.month" -> "42",
      "value.year" -> "2008"
    )

    val result = form.bind(data)
    result.errors must contain only FormError("value.day", "error.invalid.characters")
  }

  "must unbind a date" in {

    forAll(validData -> "valid date") { date =>
      val filledForm = form.fill(date)

      filledForm("value.day").value.value mustEqual date.getDayOfMonth.toString
      filledForm("value.month").value.value mustEqual date.getMonthValue.toString
      filledForm("value.year").value.value mustEqual date.getYear.toString
    }
  }

  "must bind valid month data with short and full month names" in {

    val validMonths = Table(
      ("day", "month", "year", "expectedMonthValue"),
      ("01", "Jan", "2020", 1),
      ("01", "January", "2020", 1),
      ("15", "Feb", "2021", 2),
      ("15", "February", "2021", 2)
    )

    forAll(validMonths) { (day, month, year, expectedMonthValue) =>
      val data = Map("value.day" -> day, "value.month" -> month, "value.year" -> year)

      val result = form.bind(data)

      result.value.value.getDayOfMonth mustEqual day.toInt
      result.value.value.getMonthValue mustEqual expectedMonthValue
      result.value.value.getYear mustEqual year.toInt
    }
  }

  "must fail to bind invalid month names" in {

    val invalidMonths = Table(
      ("day", "month", "year", "expectedError"),
      ("01", "Febru", "2020", "error.invalid.characters"),
      ("01", "Februaryys", "2020", "error.invalid.characters"),
      ("15", "AnyText", "2021", "error.invalid.characters")
    )

    forAll(invalidMonths) { (day, month, year, expectedError) =>
      val data = Map("value.day" -> day, "value.month" -> month, "value.year" -> year)

      val result = form.bind(data)

      result.errors must have size 1
      result.errors.head.key mustEqual "value.month"
      result.errors.head.message mustEqual expectedError
    }
  }
}
