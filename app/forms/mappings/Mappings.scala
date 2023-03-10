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

package forms.mappings

import models.{DateRange, Enumerable, Money}
import play.api.data.FieldMapping
import play.api.data.Forms.of
import play.api.data.validation.{Constraint, Invalid, Valid}

import java.time.LocalDate

trait Mappings extends Formatters with Constraints {

  protected def text(errorKey: String = "error.required", args: Seq[Any] = Seq.empty): FieldMapping[String] =
    of(stringFormatter(errorKey, args))

  protected def int(requiredKey: String = "error.required",
                    wholeNumberKey: String = "error.wholeNumber",
                    nonNumericKey: String = "error.nonNumeric",
                    args: Seq[String] = Seq.empty): FieldMapping[Int] =
    of(intFormatter(requiredKey, wholeNumberKey, nonNumericKey, args))

  protected def double(requiredKey: String = "error.required",
                       nonNumericKey: String = "error.nonNumeric",
                       max: (Double, String) = (Double.MaxValue, "error.tooLarge"),
                       args: Seq[String] = Seq.empty): FieldMapping[Double] =
    of(doubleFormatter(requiredKey, nonNumericKey, max, args))

  protected def money(requiredKey: String = "error.required",
                       nonNumericKey: String = "error.nonMoney",
                       max: (Double, String) = (Double.MaxValue, "error.tooLarge"),
                       args: Seq[String] = Seq.empty): FieldMapping[Money] =
    of(moneyFormatter(requiredKey, nonNumericKey, max, args))

  protected def boolean(requiredKey: String = "error.required",
                        invalidKey: String = "error.boolean",
                        args: Seq[String] = Seq.empty): FieldMapping[Boolean] =
    of(booleanFormatter(requiredKey, invalidKey, args))


  protected def enumerable[A](requiredKey: String = "error.required",
                              invalidKey: String = "error.invalid",
                              args: Seq[String] = Seq.empty)(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(enumerableFormatter[A](requiredKey, invalidKey, args))

  protected def localDate(dateFormErrors: DateFormErrors,
                          args: Seq[String] = Seq.empty): FieldMapping[LocalDate] =
    of(new LocalDateFormatter(dateFormErrors, args))

  protected def dateRange(startDateErrors: DateFormErrors,
                          endDateErrors: DateFormErrors,
                          invalidRangeError: String,
                          allowedRange: Option[DateRange],
                          startDateAllowedDateRangeError: Option[String],
                          endDateAllowedDateRangeError: Option[String],
                          duplicateRangeError: Option[String],
                          duplicateRanges: List[DateRange]): FieldMapping[DateRange] =
    of(new DateRangeFormatter(startDateErrors, endDateErrors, invalidRangeError, allowedRange, startDateAllowedDateRangeError, endDateAllowedDateRangeError, duplicateRangeError, duplicateRanges))

  protected def verify[A](errorKey: String, pred: A => Boolean, args: Any*): Constraint[A] =
    Constraint[A] { (a: A) =>
      if (pred(a)) Valid
      else Invalid(errorKey, args: _*)
    }
}
