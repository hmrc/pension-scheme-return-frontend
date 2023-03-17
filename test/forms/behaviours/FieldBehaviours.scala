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

package forms.behaviours

import forms.FormSpec
import generators.Generators
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.{Form, FormError}

import java.time.LocalDate

trait FieldBehaviours extends FormSpec with ScalaCheckPropertyChecks with Generators {

  def fieldThatBindsValidData(form: Form[_], fieldName: String, validDataGenerator: Gen[String]): Unit =
    "bind valid data" in {

      forAll(validDataGenerator -> "validDataItem") { dataItem: String =>
        val result = form.bind(Map(fieldName -> dataItem)).apply(fieldName)
        result.value.value mustBe dataItem
        result.errors mustBe empty
      }
    }

  def mandatoryField(form: Form[_], fieldName: String, requiredError: FormError): Unit = {

    "not bind when key is not present at all" in {

      val result = form.bind(emptyForm).apply(fieldName)
      result.errors mustEqual Seq(requiredError)
    }

    "not bind blank values" in {

      val result = form.bind(Map(fieldName -> "")).apply(fieldName)
      result.errors mustEqual Seq(requiredError)
    }
  }

  def mandatoryField(form: Form[_], fieldName: String, message: String): Unit =
    mandatoryField(form, fieldName, FormError(fieldName, message))

  def invalidNumericField(form: Form[_], fieldName: String, errorMessage: String, args: Any*): Unit =
    errorField(
      "numeric value is invalid",
      form,
      fieldName,
      FormError(fieldName, errorMessage, args.toList),
      alphaStr.filter(_.nonEmpty)
    )

  def invalidAlphaField(form: Form[_], fieldName: String, errorMessage: String, args: List[Any] = Nil): Unit =
    errorField(
      "alpha value is invalid",
      form,
      fieldName,
      FormError(fieldName, errorMessage, args),
      numStr.filter(_.nonEmpty)
    )

  def fieldLengthError(
    form: Form[_],
    fieldName: String,
    error: FormError,
    min: Int,
    max: Int,
    charGen: Gen[Char]
  ): Unit = {
    val lengthGen = stringLengthBetween(min, max, charGen)
    errorField(s"length is between $min and $max", form, fieldName, error, lengthGen)
  }

  def fieldRejectDuplicates(
    form: Form[_],
    fieldName: String,
    errorMessage: String,
    duplicates: List[String],
    args: Any*
  ): Unit =
    errorField(
      "duplicate values",
      form,
      fieldName,
      FormError(fieldName, errorMessage, args.toList),
      Gen.oneOf(duplicates)
    )

  def invalidField(form: Form[_], fieldName: String, errorMessage: String, invalidData: Gen[String], args: Any*): Unit =
    errorField("invalid field", form, fieldName, FormError(fieldName, errorMessage, args.toList), invalidData)

  def errorField(testName: String, form: Form[_], fieldName: String, error: FormError, gen: Gen[String]): Unit =
    s"not bind when $testName" in {
      forAll(gen -> "validDataItem") { value: String =>
        val result = form.bind(Map(fieldName -> value))(fieldName)
        result.errors mustEqual Seq(error)
      }
    }

  def fieldThatBindsValidDate(form: Form[_], fieldName: String): Unit =
    "bind valid date" in {

      forAll(date -> "valid date") { localDate: LocalDate =>
        val result = form
          .bind(
            Map(
              s"$fieldName.day" -> localDate.getDayOfMonth.toString,
              s"$fieldName.month" -> localDate.getMonthValue.toString,
              s"$fieldName.year" -> localDate.getYear.toString
            )
          )
          .apply(fieldName)

        result.errors mustBe empty
      }
    }
}
