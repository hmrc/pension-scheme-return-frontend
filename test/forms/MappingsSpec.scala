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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import forms.mappings.Mappings
import config.Constants.{maxPercentage, minPercentage}
import models._
import play.api.data.{Form, FormError}
import forms.mappings.errors.{InputFormErrors, MoneyFormErrors, PercentageFormErrors}
import org.scalatest.OptionValues
import uk.gov.hmrc.domain.Nino

object MappingsSpec {

  sealed trait Foo
  case object Bar extends Foo
  case object Baz extends Foo

  object Foo {

    val values: Set[Foo] = Set(Bar, Baz)

    implicit val fooEnumerable: Enumerable[Foo] =
      Enumerable(values.toSeq.map(v => v.toString -> v)*)
  }
}

class MappingsSpec extends AnyFreeSpec with Matchers with OptionValues with Mappings {

  import MappingsSpec._

  "text" - {

    val testForm: Form[String] =
      Form(
        "value" -> text()
      )

    "must bind a valid string" in {
      val result = testForm.bind(Map("value" -> "foobar"))
      result.get mustEqual "foobar"
    }

    "must not bind an empty string" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a string of whitespace only" in {
      val result = testForm.bind(Map("value" -> " \t"))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must return a custom error message" in {
      val form = Form("value" -> text("custom.error"))
      val result = form.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "custom.error"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill("foobar")
      result.apply("value").value.value mustEqual "foobar"
    }
  }

  "boolean" - {

    val testForm: Form[Boolean] =
      Form(
        "value" -> boolean()
      )

    "must bind true" in {
      val result = testForm.bind(Map("value" -> "true"))
      result.get mustEqual true
    }

    "must bind false" in {
      val result = testForm.bind(Map("value" -> "false"))
      result.get mustEqual false
    }

    "must not bind a non-boolean" in {
      val result = testForm.bind(Map("value" -> "not a boolean"))
      result.errors must contain(FormError("value", "error.boolean"))
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must unbind" in {
      val result = testForm.fill(true)
      result.apply("value").value.value mustEqual "true"
    }
  }

  "int" - {

    val testForm: Form[Int] =
      Form(
        "value" -> int(max = (99, "error.tooLarge"))
      )
    val testFormWithMin: Form[Int] =
      Form(
        "value" -> int(max = (99, "error.tooLarge"), min = (-99, "error.tooSmall"))
      )

    "must bind a valid integer" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind when int is too large" in {
      val result = testForm.bind(Map("value" -> "100"))
      result.errors must contain(FormError("value", "error.tooLarge"))
    }

    "must not bind when larger than max int" in {
      val result = testForm.bind(Map("value" -> (Int.MaxValue.toLong + 1).toString))
      result.errors must contain(FormError("value", "error.tooLarge"))
    }

    "must not bind when smaller than 0" in {
      val result = testForm.bind(Map("value" -> "-1"))
      result.errors must contain(FormError("value", "error.nonNumeric"))
    }

    "must bind when -99" in {
      val result = testFormWithMin.bind(Map("value" -> "-99"))
      result.get mustEqual -99
    }

    "must not bind when smaller than -99" in {
      val result = testFormWithMin.bind(Map("value" -> "-100"))
      result.errors must contain(FormError("value", "error.tooSmall"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill(123)
      result.apply("value").value.value mustEqual "123"
    }
  }

  "double" - {

    val testForm: Form[Double] =
      Form(
        "value" -> double(max = 100d -> "error.tooLarge")
      )

    val testFormWithMin: Form[Double] =
      Form(
        "value" -> double(max = 100d -> "error.tooLarge", min = -100d -> "error.tooSmall")
      )

    "must bind a valid int" in {
      val result = testForm.bind(Map("value" -> "1"))
      result.get mustEqual 1
    }

    "must bind a valid double" in {
      val result = testForm.bind(Map("value" -> "1.1"))
      result.get mustEqual 1.1
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a string" in {
      val result = testForm.bind(Map("value" -> "error"))
      result.errors must contain(FormError("value", "error.nonNumeric"))
    }

    "must not bind double that is too large" in {
      val result = testForm.bind(Map("value" -> "100.01"))
      result.errors must contain(FormError("value", "error.tooLarge"))
    }

    "must unbind a valid value" in {
      val result = testForm.fill(123.2)
      result.apply("value").value.value mustEqual "123.2"
    }

    "must not bind when smaller than 0" in {
      val result = testForm.bind(Map("value" -> "-1.23"))
      result.errors must contain(FormError("value", "error.nonNumeric"))
    }

    "must bind when -99" in {
      val result = testFormWithMin.bind(Map("value" -> "-100"))
      result.get mustEqual -100
    }

    "must not bind when smaller than -100" in {
      val result = testFormWithMin.bind(Map("value" -> "-100.01"))
      result.errors must contain(FormError("value", "error.tooSmall"))
    }
  }

  "money" - {
    val testForm: Form[Money] =
      Form(
        "value" -> money(MoneyFormErrors.default(max = 999999999.99d -> "error.tooLarge"))
      )
    val testFormWithMin: Form[Money] =
      Form(
        "value" -> money(
          MoneyFormErrors.default(max = 999999999.99d -> "error.tooLarge", min = -100d -> "error.tooSmall")
        )
      )

    "must bind a valid int" in {
      val result = testForm.bind(Map("value" -> "1.00"))
      result.get mustEqual Money(1, "1.00")
    }

    "must bind a valid double" in {
      val result = testForm.bind(Map("value" -> "10.1"))
      result.get mustEqual Money(10.1, "10.10")
    }

    "must bind a double with a £ symbol" in {
      val result = testForm.bind(Map("value" -> "10.01"))
      result.get mustEqual Money(10.01, "10.01")
    }

    "must remove whitespaces" in {
      val result = testForm.bind(Map("value" -> "10. 10"))
      result.get mustEqual Money(10.1, "10.10")
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a string" in {
      val result = testForm.bind(Map("value" -> "error"))
      result.errors must contain(FormError("value", "error.nonMoney"))
    }

    "must not bind a double with more than 2 decimal places" in {
      val result = testForm.bind(Map("value" -> "10.001"))
      result.errors must contain(FormError("value", "error.nonMoney"))
    }

    "must not bind double that is too large" in {
      val result = testForm.bind(Map("value" -> "1000000000.00"))
      result.errors must contain(FormError("value", "error.tooLarge"))
    }

    "must bind max allowed value" in {
      val result = testForm.bind(Map("value" -> "999999999.99"))
      result.get.value mustEqual 999999999.99
    }

    "must unbind a valid value" in {
      val result = testForm.fill(Money(123.4))
      result.apply("value").value.value mustEqual "123.40"
    }

    "must unbind using displayAs" in {
      val result = testForm.fill(Money(123, "123"))
      result.apply("value").value.value mustEqual "123"
    }

    "must not bind when smaller than 0" in {
      val result = testForm.bind(Map("value" -> "-0.01"))
      result.errors must contain(FormError("value", "error.nonMoney"))
    }

    "must bind when -100" in {
      val result = testFormWithMin.bind(Map("value" -> "-100"))
      result.apply("value").value.value mustEqual "-100"
    }

    "must not bind when smaller than -100" in {
      val result = testFormWithMin.bind(Map("value" -> "-100.01"))
      result.errors must contain(FormError("value", "error.tooSmall"))
    }

  }

  "enumerable" - {

    val testForm = Form(
      "value" -> enumerable[Foo]()
    )

    "must bind a valid option" in {
      val result = testForm.bind(Map("value" -> "Bar"))
      result.get mustEqual Bar
    }

    "must not bind an invalid option" in {
      val result = testForm.bind(Map("value" -> "Not Bar"))
      result.errors must contain(FormError("value", "error.invalid"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }
  }

  "crn" - {
    val testForm = Form("value" -> crn("error.required", "error.invalid", "error.minmax"))

    "must bind a valid value" in {
      val result = testForm.bind(Map("value" -> "12345678"))
      result.get mustEqual Crn("12345678")
    }

    "must bind a valid value with spaces" in {
      val result = testForm.bind(Map("value" -> "1234 5678"))
      result.get mustEqual Crn("12345678")
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an invalid value" in {
      val result = testForm.bind(Map("value" -> "*"))
      result.errors must contain(FormError("value", "error.invalid"))
    }

    "must not bind an under minimum length value" in {
      val result = testForm.bind(Map("value" -> "12"))
      result.errors must contain(FormError("value", "error.minmax"))
    }

    "must not bind an over maximum length value" in {
      val result = testForm.bind(Map("value" -> "123456789"))
      result.errors must contain(FormError("value", "error.minmax"))
    }
  }

  "validatedPsaId" - {
    val testForm = Form(
      "value" ->
        validatedPsaId(
          requiredKey = "error.required",
          regexChecks = List(("^(A[0-9]{7})$", "error.invalid.characters")),
          maxLength = 8,
          maxLengthErrorKey = "error.tooLong",
          authorisingPSAID = Some("A1234567"),
          noMatchKey = "error.noMatch"
        )
    )

    "must bind a valid value" in {
      val result = testForm.bind(Map("value" -> "A1234567"))
      result.get mustEqual "A1234567"
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an invalid value" in {
      val result = testForm.bind(Map("value" -> "*"))
      result.errors must contain(FormError("value", "error.invalid.characters"))
    }

    "must not bind an non matching value" in {
      val result = testForm.bind(Map("value" -> "A7654321"))
      result.errors must contain(FormError("value", "error.noMatch"))
    }
  }

  "optionalInput" - {
    val fieldErrors: InputFormErrors =
      InputFormErrors.input(
        "required",
        "invalid",
        "max"
      )

    val testForm = Form(
      "value" -> optionalInput(fieldErrors)
    )

    "must bind a valid value" in {
      val result = testForm.bind(Map("value" -> "foobar"))
      result.get mustEqual Some("foobar")
    }

    "must bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.get mustEqual None
    }

    "must trim space" in {
      val result = testForm.bind(Map("value" -> " foobar"))
      result.get mustEqual Some("foobar")
    }

    "must not bind an invalid value" in {
      val result = testForm.bind(Map("value" -> "*"))
      result.errors must contain(FormError("value", "invalid"))
    }
  }

  "optionalPostcode" - {
    val fieldErrors: InputFormErrors =
      InputFormErrors.postcode(
        "required",
        "invalid characters",
        "invalid format"
      )

    val testForm = Form(
      "value" -> optionalInput(fieldErrors)
    )

    "must bind a valid value" in {
      val result = testForm.bind(Map("value" -> "AB1 1BA"))
      result.get mustEqual Some("AB1 1BA")
    }

    "must bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.get mustEqual None
    }

    "must trim space" in {
      val result = testForm.bind(Map("value" -> " AB1 1BA"))
      result.get mustEqual Some("AB1 1BA")
    }

    "must not bind an invalid value" in {
      val result = testForm.bind(Map("value" -> "*"))
      result.errors must contain(FormError("value", "invalid format"))
    }
  }

  "validatedText" - {
    val fieldErrors: InputFormErrors =
      InputFormErrors.input(
        "error.required",
        "invalid",
        "max"
      )

    val testForm =
      Form(
        "value" -> validatedText(
          "value",
          fieldErrors.requiredKey,
          fieldErrors.regexChecks,
          fieldErrors.max._1,
          fieldErrors.max._2
        )
      )

    "must bind a valid string" in {
      val result = testForm.bind(Map("value" -> "foobar"))
      result.get mustEqual "foobar"
    }

    "must not bind an empty string" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a string of whitespace only" in {
      val result = testForm.bind(Map("value" -> " \t"))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must return a custom error message" in {
      val form = Form("value" -> text("custom.error"))
      val result = form.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "custom.error"))
    }
  }

  "nino" - {

    val testForm: Form[Nino] =
      Form("value" -> nino("error.required", "error.invalid"))

    "must bind a valid nino" in {
      val result = testForm.bind(Map("value" -> "AB 123456 A"))
      result.get mustEqual Nino("AB123456A")
    }

    "must not bind an empty nino" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a nino of whitespace only" in {
      val result = testForm.bind(Map("value" -> " \t"))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must return a custom error message" in {
      val form = Form("value" -> text("custom.error"))
      val result = form.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "custom.error"))
    }
  }

  "nino with duplicates" - {

    val duplicates = List(Nino("AB123456C"), Nino("AB123456D"))
    val testForm: Form[Nino] =
      Form("value" -> ninoNoDuplicates("error.required", "error.invalid", duplicates, "error.duplicate"))

    "must bind a valid nino" in {
      val result = testForm.bind(Map("value" -> "AB 123456 A"))
      result.get mustEqual Nino("AB123456A")
    }

    "must not bind an empty nino" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a duplicate nino" in {
      val result = testForm.bind(Map("value" -> "AB123456C"))
      result.errors must contain(FormError("value", "error.duplicate"))
    }

    "must not bind a duplicate nino with lowercase" in {
      val result = testForm.bind(Map("value" -> "aB123456c"))
      result.errors must contain(FormError("value", "error.duplicate"))
    }

    "must not bind a duplicate nino with whitespaces" in {
      val result = testForm.bind(Map("value" -> "AB12 3456C"))
      result.errors must contain(FormError("value", "error.duplicate"))
    }

    "must not bind a nino of whitespace only" in {
      val result = testForm.bind(Map("value" -> " \t"))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must return a custom error message" in {
      val form = Form("value" -> text("custom.error"))
      val result = form.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "custom.error"))
    }
  }

  "postcode" - {
    val fieldErrors: InputFormErrors =
      InputFormErrors.postcode(
        "required",
        "invalid characters",
        "invalid format"
      )

    val testForm = Form(
      "value" -> postCode(fieldErrors)
    )

    "must bind a valid value" in {
      val result = testForm.bind(Map("value" -> "AB1 1BA"))
      result.get mustEqual "AB11BA"
    }

    "must trim whitespaces" in {
      val result = testForm.bind(Map("value" -> " AB1 \t1BA "))
      result.get mustEqual "AB11BA"
    }

    "must not bind an invalid value" in {
      val result = testForm.bind(Map("value" -> "*"))
      result.errors must contain(FormError("value", "invalid format"))
    }
  }
  "percentage" - {
    val formErrors = PercentageFormErrors(
      "error.required",
      "error.nonNumeric",
      (maxPercentage, "error.tooLarge"),
      (minPercentage, "error.tooLow")
    )
    val testForm: Form[Percentage] =
      Form(
        "value" -> percentage(formErrors)
      )

    "must bind a valid percentage" in {
      val result = testForm.bind(Map("value" -> "1.00"))
      result.get mustEqual Percentage(1, "1.00")
    }

    "must bind a valid Percentage" in {
      val result = testForm.bind(Map("value" -> "10.1"))
      result.get mustEqual Percentage(10.1, "10.1")
    }

    "must remove whitespaces" in {
      val result = testForm.bind(Map("value" -> "10. 10"))
      result.get mustEqual Percentage(10.1, "10.10")
    }

    "must not bind an empty value" in {
      val result = testForm.bind(Map("value" -> ""))
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind an empty map" in {
      val result = testForm.bind(Map.empty[String, String])
      result.errors must contain(FormError("value", "error.required"))
    }

    "must not bind a string" in {
      val result = testForm.bind(Map("value" -> "error"))
      result.errors must contain(FormError("value", "error.nonNumeric"))
    }

    "must not bind a double with more than 2 decimal places" in {
      val result = testForm.bind(Map("value" -> "10.001"))
      result.errors must contain(FormError("value", "error.nonNumeric"))
    }

    "must not bind double that is too large" in {
      val result = testForm.bind(Map("value" -> "1000.00"))
      result.errors must contain(FormError("value", "error.tooLarge"))
    }

    "must bind max allowed value" in {
      val result = testForm.bind(Map("value" -> "999.99"))
      result.get.value mustEqual 999.99
    }

    "must unbind a valid value" in {
      val result = testForm.fill(Percentage(123.4))
      result.apply("value").value.value mustEqual "123.4"
    }

    "must unbind using displayAs" in {
      val result = testForm.fill(Percentage(123, "123"))
      result.apply("value").value.value mustEqual "123"
    }

    "must not bind when smaller than -999.99" in {
      val result = testForm.bind(Map("value" -> "-1000.00"))
      result.errors must contain(FormError("value", "error.tooLow"))
    }
  }

}
