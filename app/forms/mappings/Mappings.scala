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

import forms.mappings.errors.{DateFormErrors, IntFormErrors, MoneyFormErrors}
import models.{Crn, DateRange, Enumerable, Money}
import play.api.data.{FieldMapping, Mapping}
import play.api.data.Forms.of
import play.api.data.validation.{Constraint, Invalid, Valid}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

trait Mappings extends Formatters with Constraints {

  def text(errorKey: String = "error.required", args: Seq[Any] = Seq.empty): Mapping[String] =
    of(stringFormatter(errorKey, args)).transform(_.trim, _.trim)

  def int(
    requiredKey: String = "error.required",
    wholeNumberKey: String = "error.wholeNumber",
    nonNumericKey: String = "error.nonNumeric",
    max: (Int, String) = (Int.MaxValue, "error.tooLarge"),
    args: Seq[String] = Seq.empty
  ): FieldMapping[Int] =
    int(IntFormErrors(requiredKey, wholeNumberKey, nonNumericKey, max), args)

  def int(
    intFormErrors: IntFormErrors,
    args: Seq[String]
  ): FieldMapping[Int] =
    of(intFormatter(intFormErrors, args))

  def double(
    requiredKey: String = "error.required",
    nonNumericKey: String = "error.nonNumeric",
    max: (Double, String) = (Double.MaxValue, "error.tooLarge"),
    args: Seq[String] = Seq.empty
  ): FieldMapping[Double] =
    of(doubleFormatter(requiredKey, nonNumericKey, max, args))

  def money(
    moneyFormErrors: MoneyFormErrors,
    args: Seq[String] = Seq.empty
  ): FieldMapping[Money] =
    of(moneyFormatter(moneyFormErrors, args))

  def boolean(
    requiredKey: String = "error.required",
    invalidKey: String = "error.boolean",
    args: Seq[String] = Seq.empty
  ): FieldMapping[Boolean] =
    of(booleanFormatter(requiredKey, invalidKey, args))

  def enumerable[A](
    requiredKey: String = "error.required",
    invalidKey: String = "error.invalid",
    args: Seq[String] = Seq.empty
  )(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(enumerableFormatter[A](requiredKey, invalidKey, args))

  def localDate(dateFormErrors: DateFormErrors, args: Seq[String] = Seq.empty): FieldMapping[LocalDate] =
    of(new LocalDateFormatter(dateFormErrors, args))

  def dateRange(
    startDateErrors: DateFormErrors,
    endDateErrors: DateFormErrors,
    invalidRangeError: String,
    allowedRange: Option[DateRange],
    startDateAllowedDateRangeError: Option[String],
    endDateAllowedDateRangeError: Option[String],
    duplicateRangeError: Option[String],
    duplicateRanges: List[DateRange]
  ): FieldMapping[DateRange] =
    of(
      new DateRangeFormatter(
        startDateErrors,
        endDateErrors,
        invalidRangeError,
        allowedRange,
        startDateAllowedDateRangeError,
        endDateAllowedDateRangeError,
        duplicateRangeError,
        duplicateRanges
      )
    )

  def verify[A](errorKey: String, pred: A => Boolean, args: Any*): Constraint[A] =
    Constraint[A] { (a: A) =>
      if (pred(a)) Valid
      else Invalid(errorKey, args: _*)
    }

  def validatedText(
    requiredKey: String,
    textRegex: String,
    invalidCharactersKey: String,
    maxLength: Int,
    maxLengthErrorKey: String,
    args: Any*
  ): Mapping[String] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidCharactersKey, _.matches(textRegex), args: _*))
      .verifying(verify[String](maxLengthErrorKey, _.length <= maxLength, args: _*))

  def nino(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Mapping[Nino] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Nino.isValid(s.toUpperCase), args: _*))
      .transform[Nino](s => Nino(s.toUpperCase), _.nino.toUpperCase)

  def crn(requiredKey: String, invalidKey: String, minMaxLengthErrorKey: String, args: Any*): Mapping[Crn] =
    text(requiredKey, args.toList)
      .verifying(verify[String](minMaxLengthErrorKey, s => Crn.isLengthInRange(s.toUpperCase), args: _*))
      .verifying(verify[String](invalidKey, s => Crn.isValid(s.toUpperCase), args: _*))
      .transform[Crn](s => Crn(s.toUpperCase), _.crn.toUpperCase)

  def ninoNoDuplicates(
    requiredKey: String,
    invalidKey: String,
    duplicates: List[Nino],
    duplicateKey: String,
    args: Any*
  ): Mapping[Nino] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Nino.isValid(s.toUpperCase), args: _*))
      .verifying(verify[String](duplicateKey, !duplicates.map(_.nino).contains(_), args: _*))
      .transform[Nino](s => Nino(s.toUpperCase), _.nino.toUpperCase)
}

object Mappings extends Mappings
