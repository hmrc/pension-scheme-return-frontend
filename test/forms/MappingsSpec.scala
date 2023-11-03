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

import forms.mappings.Mappings
import forms.mappings.errors.MoneyFormErrors
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.data.{Form, FormError}
import models.{Crn, Enumerable, Money}

object MappingsSpec {

  sealed trait Foo
  case object Bar extends Foo
  case object Baz extends Foo

  object Foo {

    val values: Set[Foo] = Set(Bar, Baz)

    implicit val fooEnumerable: Enumerable[Foo] =
      Enumerable(values.toSeq.map(v => v.toString -> v): _*)
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
      val result = testForm.bind(Map("value" -> "1234567"))
      result.get mustEqual Crn("1234567")
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
}
