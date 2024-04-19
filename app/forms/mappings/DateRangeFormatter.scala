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
import config.Refined.Max3
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
  allowedRange: Option[DateRange],
  startDateAllowedDateRangeError: Option[String],
  endDateAllowedDateRangeError: Option[String],
  duplicateRangeError: Option[String],
  duplicateRanges: List[DateRange],
  previousDateRangeError: Option[String],
  index: Max3,
  taxYear: TaxYear,
  errorTaxYear: Option[String]
) extends Formatter[DateRange]
    with Formatters {

  val startDateFormatter = new LocalDateFormatter(startDateErrors)
  val endDateFormatter = new LocalDateFormatter(endDateErrors)

  private def verifyValidRange(key: String, range: DateRange): Either[Seq[FormError], DateRange] =
    if (range.from.isBefore(range.to)) Right(range)
    else Left(List(FormError(s"$key.endDate", invalidRangeError, List(range.from.show, range.to.show))))

  private def verifyRangeBounds(
    key: String,
    date: LocalDate,
    error: Option[String]
  ): Either[Seq[FormError], LocalDate] =
    allowedRange
      .zip(error)
      .map {
        case (range, error) =>
          if (range.contains(date)) Right(date)
          else Left(List(FormError(key, error, List(range.from.show, range.to.show))))
      }
      .getOrElse(Right(date))

  private def verifyUniqueRange(key: String, range: DateRange): Either[Seq[FormError], DateRange] =
    duplicateRangeError
      .flatMap { error =>
        duplicateRanges
          .find(d => d.intersects(range))
          .map { i =>
            Seq(FormError(s"$key.startDate", error, List(i.from.show, i.to.show)))
          }
      }
      .toLeft(range)

  private def verifyTaxYear(key: String, range: DateRange): Either[Seq[FormError], DateRange] =
    errorTaxYear match {
      case Some(error) =>
        if (!range.from.isBefore(taxYear.finishes))
          Left(List(FormError(s"$key.endDate", error, List(range.to.show))))
        else Right(range)
      case _ => Right(range)
    }

  private def verifyPreviousDateRange(key: String, range: DateRange, index: Max3): Either[Seq[FormError], DateRange] =
    if (duplicateRanges.isEmpty || index.value == 1)
      Right(range)
    else
      previousDateRangeError match {
        case Some(error) =>
          val indexDate = duplicateRanges(index.value - 2)
          if (!indexDate.to.plusDays(1).isEqual(range.from))
            Left(Seq(FormError(s"$key.startDate", error, List(duplicateRanges.last.to.show))))
          else
            Right(range)
        case None =>
          Right(range)
      }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], DateRange] =
    for {
      dateRange <- (
        startDateFormatter.bind(s"$key.startDate", data).toValidated,
        endDateFormatter.bind(s"$key.endDate", data).toValidated
      ).mapN(DateRange(_, _)).toEither
      _ <- verifyValidRange(key, dateRange)
      _ <- (
        verifyRangeBounds(s"$key.startDate", dateRange.from, startDateAllowedDateRangeError).toValidated,
        verifyRangeBounds(s"$key.endDate", dateRange.to, endDateAllowedDateRangeError).toValidated
      ).mapN(DateRange(_, _)).toEither
      _ <- verifyTaxYear(key, dateRange)
      _ <- verifyUniqueRange(key, dateRange)
      _ <- verifyPreviousDateRange(key, dateRange, index)
    } yield {
      dateRange
    }

  override def unbind(key: String, value: DateRange): Map[String, String] =
    startDateFormatter.unbind(s"$key.startDate", value.from) ++
      endDateFormatter.unbind(s"$key.endDate", value.to)
}
