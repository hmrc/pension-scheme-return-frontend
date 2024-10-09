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

import play.api.data.format.Formatter
import cats.data.Validated._
import config.RefinedTypes.Max3
import play.api.Logger
import cats.syntax.all._
import uk.gov.hmrc.time.TaxYear
import play.api.data.FormError
import forms.mappings.errors.DateFormErrors
import utils.DateTimeUtils.localDateShow
import models.DateRange

import java.time.LocalDate

private[mappings] class DateRangeFormatter(
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
) extends Formatter[DateRange]
    with Formatters {

  private val logger = Logger(getClass)

  val startDateFormatter = new LocalDateFormatter(startDateErrors)
  val endDateFormatter = new LocalDateFormatter(endDateErrors)

  private def verifyValidRange(key: String, range: DateRange): Either[Seq[FormError], DateRange] =
    if (range.from.isBefore(range.to)) Right(range)
    else Left(List(FormError(s"$key.endDate", invalidRangeError, List(range.from.show, range.to.show))))

  // Checks that the provided date is within the allowedRange DateRange
  private def verifyRangeBounds(
    key: String,
    date: LocalDate,
    error: String
  ): Either[Seq[FormError], LocalDate] =
    if (allowedRange.contains(date)) Right(date)
    else {
      logger.info(s"[verifyRangeBounds] provided date ${date.show} is not within range ${allowedRange.toString}")
      Left(List(FormError(key, error, List(allowedRange.from.show, allowedRange.to.show))))
    }

  private def verifyUniqueRange(key: String, range: DateRange): Either[Seq[FormError], DateRange] = {

    val validateStartDate: Either[Seq[FormError], DateRange] =
      duplicateRanges
        .find(_.contains(range.from))
        .map(
          startError =>
            Seq(FormError(s"$key.startDate", overlappedStartDateError, List(startError.from.show, startError.to.show)))
        )
        .toLeft(range)

    val validateEndDate: Either[Seq[FormError], DateRange] =
      duplicateRanges
        .find(_.contains(range.to))
        .map(
          endError =>
            Seq(FormError(s"$key.endDate", overlappedEndDateError, List(endError.from.show, endError.to.show)))
        )
        .toLeft(range)

    validateStartDate *> validateEndDate
  }

  // Verify provided date is after the start of the tax year or before the end of the tax year
  private def verifyTaxYear(key: String, range: DateRange): Either[Seq[FormError], DateRange] =
    if (range.from.isBefore(allowedRange.from)) {
      logger.info(
        s"[verifyTaxYear] provided start date ${range.from.show} is before the allowed date range ${allowedRange.from.show}"
      )
      Left(List(FormError(s"$key.startDate", errorStartAfter, List(allowedRange.from.show))))
    } else if (range.from.isAfter(taxYear.finishes)) {
      logger.info(s"[verifyTaxYear] provided start date ${range.from.show} is after the end of the tax year")
      Left(List(FormError(s"$key.startDate", errorStartBefore, List(taxYear.finishes.show))))
    } else if (range.to.isBefore(allowedRange.from)) {
      logger.info(s"[verifyTaxYear] provided end date ${range.to.show} is before the start of the tax year")
      Left(List(FormError(s"$key.endDate", errorEndAfter, List(allowedRange.from.minusDays(1).show))))
    } else if (range.to.isAfter(taxYear.finishes)) {
      logger.info(s"[verifyTaxYear] provided end date ${range.to.show} is after the end of the tax year")
      Left(List(FormError(s"$key.endDate", errorEndBefore, List(taxYear.finishes.plusDays(1).show))))
    } else Right(range)

  private def verifyPreviousDateRange(key: String, range: DateRange, index: Max3): Either[Seq[FormError], DateRange] =
    if (duplicateRanges.isEmpty || index.value == 1)
      Right(range)
    else
      previousDateRangeError match {
        case Some(error) =>
          val indexDate = duplicateRanges(index.value - 2)
          if (!indexDate.to.plusDays(1).isEqual(range.from)) {
            logger.info(s"[verifyPreviousDateRange] provided date ${range.from.show} is a duplicate date}")
            Left(Seq(FormError(s"$key.startDate", error, List(duplicateRanges.last.to.plusDays(1).show))))
          } else {
            Right(range)
          }
        case None =>
          Right(range)
      }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateRange] =
    for {
      dateRange <- (
        startDateFormatter.bind(s"$key.startDate", data).toValidated,
        endDateFormatter.bind(s"$key.endDate", data).toValidated
      ).mapN(DateRange(_, _)).toEither
      _ <- verifyTaxYear(key, dateRange)
      _ <- verifyValidRange(key, dateRange)
      _ <- verifyUniqueRange(key, dateRange)
      _ <- verifyPreviousDateRange(key, dateRange, index)
      _ <- verifyRangeBounds(s"$key.startDate", dateRange.from, startDateAllowedDateRangeError)
      _ <- verifyRangeBounds(s"$key.endDate", dateRange.to, endDateAllowedDateRangeError)
    } yield {
      dateRange
    }

  override def unbind(key: String, value: DateRange): Map[String, String] =
    startDateFormatter.unbind(s"$key.startDate", value.from) ++
      endDateFormatter.unbind(s"$key.endDate", value.to)
}
