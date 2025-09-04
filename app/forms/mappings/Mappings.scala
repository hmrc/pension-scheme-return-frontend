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

import uk.gov.voa.play.form.Condition
import models._
import uk.gov.hmrc.time.TaxYear
import play.api.data.{FieldMapping, Mapping}
import forms.mappings.errors._
import play.api.data.Forms.{of, optional}
import config.RefinedTypes.Max3
import play.api.data.validation.{Constraint, Invalid, Valid}
import uk.gov.hmrc.domain.Nino

import java.time.LocalDate

trait Mappings extends Formatters with Constraints {

  def text(errorKey: String = "error.required", args: Seq[Any] = Seq.empty): Mapping[String] =
    of(using stringFormatter(errorKey, args)).transform[String](_.trim, _.trim)

  private def textWithKey(
    key: String,
    errorKey: String,
    args: Seq[Any]
  ): Mapping[String] =
    FieldMapping[String](key = key)(using stringFormatterWithKey(key, errorKey, args)).transform[String](_.trim, _.trim)

  def conditional[A](
    l: List[(Condition, Option[Mapping[A]])],
    prePopKey: Option[String]
  )(implicit ev: GenericFormMapper[String, A]): FieldMapping[Option[A]] =
    of(using conditionalFormatter[A](l, prePopKey))

  private def optionalText(): Mapping[String] =
    of(using optionalStringFormatter()).transform[String](_.trim, _.trim)

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
    of(using intFormatter(intFormErrors, args))

  def int(
    intFormErrors: IntFormErrors
  ): FieldMapping[Int] =
    of(using intFormatter(intFormErrors, Nil))

  def double(
    requiredKey: String = "error.required",
    nonNumericKey: String = "error.nonNumeric",
    max: (Double, String) = (Double.MaxValue, "error.tooLarge"),
    min: (Double, String) = (0d, "error.tooSmall"),
    args: Seq[String] = Seq.empty
  ): FieldMapping[Double] =
    of(using doubleFormatter(requiredKey, nonNumericKey, max, min, args))

  def money(
    moneyFormErrors: MoneyFormErrors,
    args: Seq[String] = Seq.empty
  ): FieldMapping[Money] =
    of(using moneyFormatter(moneyFormErrors, args))

  def optionalMoney(
    moneyFormErrors: MoneyFormErrors,
    args: Seq[String] = Seq.empty
  ): Mapping[Option[Money]] =
    optional(
      money(
        moneyFormErrors,
        args
      )
    )

  def security(requiredKey: String, invalidKey: String, maxLengthErrorKey: String, args: Any*): Mapping[Security] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Security.isValid(s), args*))
      .verifying(verify[String](maxLengthErrorKey, s => Security.maxLengthCheck(s), args*))
      .transform[Security](s => Security(s), _.security)

  def percentage(
    percentageFormErrors: PercentageFormErrors,
    args: Seq[String] = Seq.empty
  ): FieldMapping[Percentage] =
    of(using percentageFormatter(percentageFormErrors, args))

  def boolean(
    requiredKey: String = "error.required",
    invalidKey: String = "error.boolean",
    args: Seq[String] = Seq.empty
  ): FieldMapping[Boolean] =
    of(using booleanFormatter(requiredKey, invalidKey, args))

  val unit: FieldMapping[Unit] = of(using unitFormatter)

  def enumerable[A](
    requiredKey: String = "error.required",
    invalidKey: String = "error.invalid",
    args: Seq[String] = Seq.empty
  )(implicit ev: Enumerable[A]): FieldMapping[A] =
    of(using enumerableFormatter[A](requiredKey, invalidKey, args))

  def localDate(dateFormErrors: DateFormErrors, args: Seq[String] = Seq.empty): FieldMapping[LocalDate] =
    of(using new LocalDateFormatter(dateFormErrors, args))

  def dateRange(
    startDateErrors: DateFormErrors,
    endDateErrors: DateFormErrors,
    invalidRangeError: String,
    allowedRange: DateRange,
    startDateAllowedDateRangeError: String,
    endDateAllowedDateRangeError: String,
    overlappedStartDateError: String,
    overlappedEndDateError: String,
    duplicateRanges: List[DateRange],
    previousDateRangeError: Option[String],
    index: Max3,
    taxYear: TaxYear,
    errorStartBefore: String,
    errorStartAfter: String,
    errorEndBefore: String,
    errorEndAfter: String
  ): FieldMapping[DateRange] =
    of(using
      new DateRangeFormatter(
        startDateErrors,
        endDateErrors,
        invalidRangeError,
        allowedRange,
        startDateAllowedDateRangeError,
        endDateAllowedDateRangeError,
        overlappedStartDateError,
        overlappedEndDateError,
        duplicateRanges,
        previousDateRangeError,
        index,
        taxYear,
        errorStartBefore,
        errorStartAfter,
        errorEndBefore,
        errorEndAfter
      )
    )

  def verify[A](errorKey: String, pred: A => Boolean, args: Any*): Constraint[A] =
    Constraint[A] { (a: A) =>
      if (pred(a)) Valid
      else Invalid(errorKey, args*)
    }

  def validatedText(
    requiredKey: String,
    regexChecks: List[(Regex, String)],
    maxLength: Int,
    maxLengthErrorKey: String,
    args: Any*
  ): Mapping[String] =
    regexChecks
      .foldLeft(text(requiredKey, args.toList)) { case (mapping, (regex, key)) =>
        mapping.verifying(verify[String](key, _.matches(regex), args*))
      }
      .verifying(verify[String](maxLengthErrorKey, _.length <= maxLength, args*))

  private def validatedOptionalText(
    regexChecks: List[(Regex, String)],
    maxLength: Int,
    maxLengthErrorKey: String,
    args: Any*
  ): Mapping[String] =
    regexChecks
      .foldLeft(optionalText()) { case (mapping, (regex, key)) =>
        mapping.verifying(verify[String](key, s => s.trim.isEmpty || s.matches(regex), args*))
      }
      .verifying(verify[String](maxLengthErrorKey, _.length <= maxLength, args*))

  def validatedText(
    fieldKey: String,
    requiredKey: String,
    regexChecks: List[(Regex, String)],
    maxLength: Int,
    maxLengthErrorKey: String,
    args: Any*
  ): Mapping[String] =
    regexChecks
      .foldLeft(textWithKey(fieldKey, requiredKey, args.toList)) { case (mapping, (regex, key)) =>
        mapping.verifying(verify[String](key, _.filterNot(_.isWhitespace).matches(regex), args*))
      }
      .verifying(verify[String](maxLengthErrorKey, _.length <= maxLength, args*))

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
      .foldLeft(text(requiredKey, args.toList)) { case (mapping, (regex, key)) =>
        mapping.verifying(verify[String](key, _.matches(regex), args*))
      }
      .verifying(isEqual(authorisingPSAID, noMatchKey))
      .verifying(verify[String](maxLengthErrorKey, _.length <= maxLength, args*))

  def input(formErrors: InputFormErrors): Mapping[String] =
    validatedText(
      formErrors.requiredKey,
      formErrors.regexChecks,
      formErrors.max._1,
      formErrors.max._2,
      formErrors.args*
    )

  def input(key: String, formErrors: InputFormErrors): Mapping[String] =
    validatedText(
      key,
      formErrors.requiredKey,
      formErrors.regexChecks,
      formErrors.max._1,
      formErrors.max._2,
      formErrors.args*
    )

  def optionalInput(formErrors: InputFormErrors): Mapping[Option[String]] =
    optional(
      validatedOptionalText(
        formErrors.regexChecks,
        formErrors.max._1,
        formErrors.max._2,
        formErrors.args*
      )
    )

  def nino(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Mapping[Nino] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Nino.isValid(s.filterNot(_.isWhitespace).toUpperCase), args*))
      .transform[Nino](s => Nino(s.filterNot(_.isWhitespace).toUpperCase), _.nino.filterNot(_.isWhitespace).toUpperCase)

  def utr(
    requiredKey: String,
    invalidKey: String,
    args: Any*
  ): Mapping[Utr] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Utr.isValid(s.toUpperCase), args*))
      .transform[Utr](s => Utr(s.toUpperCase), _.utr.toUpperCase)

  def crn(requiredKey: String, invalidKey: String, minMaxLengthErrorKey: String, args: Any*): Mapping[Crn] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Crn.isValid(s.toUpperCase), args*))
      .verifying(verify[String](minMaxLengthErrorKey, s => Crn.isLengthInRange(s.toUpperCase), args*))
      .transform[Crn](s => Crn(s.toUpperCase.replaceAll(" ", "")), _.crn.toUpperCase)

  def ninoNoDuplicates(
    requiredKey: String,
    invalidKey: String,
    duplicates: List[Nino],
    duplicateKey: String,
    args: Any*
  ): Mapping[Nino] =
    text(requiredKey, args.toList)
      .verifying(verify[String](invalidKey, s => Nino.isValid(s.filterNot(_.isWhitespace).toUpperCase), args*))
      .verifying(
        verify[String](
          duplicateKey,
          s =>
            !duplicates
              .map(_.nino.toUpperCase.filterNot(_.isWhitespace))
              .contains(s.toUpperCase.filterNot(_.isWhitespace)),
          args*
        )
      )
      .transform[Nino](s => Nino(s.filterNot(_.isWhitespace).toUpperCase), _.nino.toUpperCase)

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
      .foldLeft(text(inputFormErrors.requiredKey, inputFormErrors.args.toList)) { case (mapping, (regex, key)) =>
        mapping.verifying(
          verify[String](key, _.filterNot(_.isWhitespace).toUpperCase.matches(regex), inputFormErrors.args*)
        )
      }
      .verifying(
        verify[String](
          inputFormErrors.max._2,
          _.filterNot(_.isWhitespace).length <= inputFormErrors.max._1,
          inputFormErrors.args*
        )
      )
      .transform[String](_.filterNot(_.isWhitespace).toUpperCase, _.filterNot(_.isWhitespace).toUpperCase)

  private def nonRequiredPostCode(
    regexChecks: List[(Regex, String)],
    maxLength: Int,
    maxLengthErrorKey: String,
    args: Any*
  ): Mapping[String] =
    regexChecks
      .foldLeft(optionalText()) { case (mapping, (regex, key)) =>
        mapping.verifying(verify[String](key, _.filterNot(_.isWhitespace).toUpperCase.matches(regex), args*))
      }
      .verifying(verify[String](maxLengthErrorKey, _.filterNot(_.isWhitespace).length <= maxLength, args))
      .transform[String](_.filterNot(_.isWhitespace).toUpperCase, _.filterNot(_.isWhitespace).toUpperCase)

  def optionalPostcode(formErrors: InputFormErrors): Mapping[Option[String]] =
    optional(
      nonRequiredPostCode(
        formErrors.regexChecks,
        formErrors.max._1,
        formErrors.max._2,
        formErrors.args*
      )
    )
}

object Mappings extends Mappings
