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

package forms.mappings

import config.Refined.Max3
import play.api.data.Forms.{of, optional}
import uk.gov.voa.play.form.Condition
import models._
import uk.gov.hmrc.time.TaxYear
import play.api.data.{FieldMapping, Mapping}
import forms.mappings.errors._
import play.api.data.validation.{Constraint, Invalid, Valid}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

trait Mappings extends Formatters with Constraints {

  def text(errorKey: String = "error.required", args: Seq[Any] = Seq.empty): Mapping[String] =
    of(stringFormatter(errorKey, args)).transform[String](_.trim, _.trim)

  def textWithKey(key: String, errorKey: String = "error.required", args: Seq[Any] = Seq.empty): Mapping[String] =
    FieldMapping[String](key = key)(stringFormatterWithKey(key, errorKey, args)).transform[String](_.trim, _.trim)

  def conditional[A](
    l: List[(Condition, Option[Mapping[A]])],
    prePopKey: Option[String]
  )(implicit ev: GenericFormMapper[String, A]): FieldMapping[Option[A]] =
    of(conditionalFormatter[A](l, prePopKey))

  def optionalText(): Mapping[String] =
    of(optionalStringFormatter()).transform[String](_.trim, _.trim)

  def optionalText(invalidKey: String): Mapping[Option[String]] =
    optional(text(invalidKey))

  def int(
    requiredKey: String = "error.required",
    wholeNumberKey: String = "error.wholeNumber",
    nonNumericKey: String = "error.nonNumeric",
    max: (Int, String) = (Int.MaxValue, "error.tooLarge"),
    min: (Int, String) = (0, "error.tooSmall"),
    args: Seq[String] = Seq.empty
  ): FieldMapping[Int] =
    int(IntFormErrors(requiredKey, wholeNumberKey, nonNumericKey, max, min), args)

  def int(
    intFormErrors: IntFormErrors,
    args: Seq[String]
  ): FieldMapping[Int] =
    of(intFormatter(intFormErrors, args))

  def int(
    intFormErrors: IntFormErrors
  ): FieldMapping[Int] =
    of(intFormatter(intFormErrors, Nil))

  def double(
    requiredKey: String = "error.required",
    nonNumericKey: String = "error.nonNumeric",
    max: (Double, String) = (Double.MaxValue, "error.tooLarge"),
    min: (Double, String) = (0d, "error.tooSmall"),
    args: Seq[String] = Seq.empty
  ): FieldMapping[Double] =
    of(doubleFormatter(requiredKey, nonNumericKey, max, min, args))

  def double(
    doubleFormErrors: DoubleFormErrors,
    args: Seq[String]
  ): FieldMapping[Double] =
    of(doubleFormatter(doubleFormErrors, args))

  def double(
    doubleFormErrors: DoubleFormErrors
  ): FieldMapping[Double] =
    of(doubleFormatter(doubleFormErrors, Nil))

  def money(
    moneyFormErrors: MoneyFormErrors,
    args: Seq[String] = Seq.empty
  ): FieldMapping[Money] =
    of(moneyFormatter(moneyFormErrors, args))

  def security(
    securityFormErrors: SecurityFormErrors,
    args: Seq[String] = Seq.empty
  ): FieldMapping[Security] =
    of(securityFormatter(securityFormErrors, args))

  def security(requiredKey: String, invalidKey: String, maxLengthErrorKey: String, args: Any*): Mapping[Security] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Security.isValid(s), args: _*))
      .verifying(verify[String](maxLengthErrorKey, s => Security.maxLengthCheck(s), args: _*))
      .transform[Security](s => Security(s), _.security)

  def percentage(
    percentageFormErrors: PercentageFormErrors,
    args: Seq[String] = Seq.empty
  ): FieldMapping[Percentage] =
    of(percentageFormatter(percentageFormErrors, args))

  def boolean(
    requiredKey: String = "error.required",
    invalidKey: String = "error.boolean",
    args: Seq[String] = Seq.empty
  ): FieldMapping[Boolean] =
    of(booleanFormatter(requiredKey, invalidKey, args))

  val unit: FieldMapping[Unit] = of(unitFormatter)

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
    duplicateRanges: List[DateRange],
    previousDateRangeError: Option[String],
    index: Max3,
    taxYear: TaxYear,
    errorTaxYear: Option[String]
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
        duplicateRanges,
        previousDateRangeError,
        index,
        taxYear,
        errorTaxYear
      )
    )

  def verify[A](errorKey: String, pred: A => Boolean, args: Any*): Constraint[A] =
    Constraint[A] { (a: A) =>
      if (pred(a)) Valid
      else Invalid(errorKey, args: _*)
    }

  def validatedText(
    requiredKey: String,
    regexChecks: List[(Regex, String)],
    maxLength: Int,
    maxLengthErrorKey: String,
    args: Any*
  ): Mapping[String] =
    regexChecks
      .foldLeft(text(requiredKey, args.toList)) {
        case (mapping, (regex, key)) =>
          mapping.verifying(verify[String](key, _.matches(regex), args: _*))
      }
      .verifying(verify[String](maxLengthErrorKey, _.length <= maxLength, args: _*))

  def validatedText(
    fieldKey: String,
    requiredKey: String,
    regexChecks: List[(Regex, String)],
    maxLength: Int,
    maxLengthErrorKey: String,
    args: Any*
  ): Mapping[String] =
    regexChecks
      .foldLeft(textWithKey(fieldKey, requiredKey, args.toList)) {
        case (mapping, (regex, key)) =>
          mapping.verifying(verify[String](key, _.matches(regex), args: _*))
      }
      .verifying(verify[String](maxLengthErrorKey, _.length <= maxLength, args: _*))

  def validatedPsaId(
    requiredKey: String,
    regexChecks: List[(Regex, String)],
    maxLength: Int,
    maxLengthErrorKey: String,
    authorisingPSAID: Option[String],
    noMatchKey: String,
    args: Any*
  ): Mapping[String] =
    regexChecks
      .foldLeft(text(requiredKey, args.toList)) {
        case (mapping, (regex, key)) =>
          mapping.verifying(verify[String](key, _.matches(regex), args: _*))
      }
      .verifying(isEqual(authorisingPSAID, noMatchKey))
      .verifying(verify[String](maxLengthErrorKey, _.length <= maxLength, args: _*))

  def input(formErrors: InputFormErrors): Mapping[String] =
    validatedText(
      formErrors.requiredKey,
      formErrors.regexChecks,
      formErrors.max._1,
      formErrors.max._2,
      formErrors.args: _*
    )

  def input(key: String, formErrors: InputFormErrors): Mapping[String] =
    validatedText(
      key,
      formErrors.requiredKey,
      formErrors.regexChecks,
      formErrors.max._1,
      formErrors.max._2,
      formErrors.args: _*
    )

  def optionalInput(formErrors: InputFormErrors): Mapping[Option[String]] =
    optional(input(formErrors))

  def nino(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Mapping[Nino] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Nino.isValid(s.toUpperCase), args: _*))
      .transform[Nino](s => Nino(s.toUpperCase), _.nino.toUpperCase)

  def utr(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Mapping[Utr] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Utr.isValid(s.toUpperCase), args: _*))
      .transform[Utr](s => Utr(s.toUpperCase), _.utr.toUpperCase)

  def crn(requiredKey: String, invalidKey: String, minMaxLengthErrorKey: String, args: Any*): Mapping[Crn] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Crn.isValid(s.toUpperCase), args: _*))
      .verifying(verify[String](minMaxLengthErrorKey, s => Crn.isLengthInRange(s.toUpperCase), args: _*))
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

  private def country(countryOptions: Seq[SelectInput], errorKey: String): Constraint[String] =
    Constraint { input =>
      countryOptions
        .find(_.value == input)
        .map(_ => Valid)
        .getOrElse(Invalid(errorKey))
    }

  def select(countryOptions: Seq[SelectInput], requiredKey: String, invalidKey: String): Mapping[String] =
    text(requiredKey)
      .verifying(country(countryOptions, invalidKey))

  def postCode(inputFormErrors: InputFormErrors): Mapping[String] =
    inputFormErrors.regexChecks
      .foldLeft(text(inputFormErrors.requiredKey, inputFormErrors.args.toList)) {
        case (mapping, (regex, key)) =>
          mapping.verifying(verify[String](key, _.toUpperCase.matches(regex), inputFormErrors.args: _*))
      }
      .verifying(verify[String](inputFormErrors.max._2, _.length <= inputFormErrors.max._1, inputFormErrors.args: _*))
      .transform[String](_.toUpperCase, _.toUpperCase)

}

object Mappings extends Mappings
