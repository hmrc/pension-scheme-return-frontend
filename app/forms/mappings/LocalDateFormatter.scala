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
import cats.syntax.all._
import play.api.data.FormError
import forms.mappings.errors.{DateFormErrors, IntFormErrors}

import scala.util.{Failure, Success, Try}

import java.time.{LocalDate, Month}

private[mappings] class LocalDateFormatter(
  dateFormErrors: DateFormErrors,
  args: Seq[String] = Seq.empty
) extends Formatter[LocalDate]
    with Formatters {

  private val fieldKeys: List[String] = List("day", "month", "year")

  private def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] =
    Try(LocalDate.of(year, month, day)) match {
      case Success(date) =>
        Right(date)
      case Failure(_) =>
        Left(Seq(FormError(key, dateFormErrors.invalidDate, args)))
    }

  private def formatDate(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    def int(required: String, max: Int): Formatter[Int] = intFormatter(
      IntFormErrors(
        requiredKey = required,
        wholeNumberKey = dateFormErrors.invalidCharacters,
        nonNumericKey = dateFormErrors.invalidCharacters,
        max = (max, dateFormErrors.invalidDate),
        min = (1, dateFormErrors.invalidDate)
      ),
      args
    )

    def runValidators(input: List[Option[String]]): Either[Seq[FormError], Unit] =
      input.flatten match {
        case Nil => Right(())
        case xs => Left(xs.map(FormError(key, _)))
      }

    val validated = (
      int(dateFormErrors.requiredDay, 31).bind(s"$key.day", data).toValidated,
      MonthFormatter(dateFormErrors.requiredMonth, dateFormErrors.invalidCharacters, args)
        .bind(s"$key.month", data)
        .toValidated,
      int(dateFormErrors.requiredYear, 9999).bind(s"$key.year", data).toValidated
    ).tupled.toEither

    for {
      valid <- validated
      (day, month, year) = valid
      date <- toDate(key, day, month, year)
      _ <- runValidators(dateFormErrors.validators.map(f => f(date)))
    } yield date
  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {

    val fields = fieldKeys.filterNot { field =>
      data.get(s"$key.$field").exists(_.trim.nonEmpty)
    }

    fields match {
      case _ :: _ :: _ :: Nil =>
        Left(List(FormError(key, dateFormErrors.required, args)))
      case f1 :: f2 :: Nil =>
        Left(
          List(
            FormError(s"$key.$f1", dateFormErrors.requiredTwo, List(s"date.$f1.lower", s"date.$f2.lower") ++ args),
            FormError(s"$key.$f2", dateFormErrors.requiredTwo, List(s"date.$f1.lower", s"date.$f2.lower") ++ args)
          )
        )
      case _ =>
        formatDate(key, data).left.map { errors =>
          prioritizeError(errors.map(_.copy(args = args)))
        }
    }
  }

  private def prioritizeError(allErrors: Seq[FormError]): Seq[FormError] = {
    val priorityMapping: Map[String, Int] = Map(
      "required" -> 0,
      "invalid" -> 1
    )

    allErrors
      .sortBy { error =>
        priorityMapping
          .collectFirst {
            case (key, priority) if error.message.contains(key) => priority
          }
          .getOrElse(2) // Default priority for errors not matching "required" or "invalid"
      }
      .headOption
      .toSeq
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day" -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year" -> value.getYear.toString
    )
}

private object MonthFormatter extends Formatters {

  def apply(requiredKey: String, invalidKey: String, args: Seq[String] = Seq.empty): Formatter[Int] =
    new Formatter[Int] {

      private val MinMonth = 1
      private val MaxMonth = 12
      private val MonthAbbreviationLength = 3

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
        data.get(key).map(_.trim).filter(_.nonEmpty) match {

          case None => Left(List(FormError(key, requiredKey, args)))
          case Some(value) =>
            val normalizedString = value.toUpperCase.replaceAll("\\s+", "")

            normalizedString.toIntOption match {
              case Some(number) if number >= MinMonth && number <= MaxMonth => Right(number)
              case _ =>
                Month.values.toList
                  .find(m =>
                    m.toString.toUpperCase == normalizedString ||
                      m.toString.take(MonthAbbreviationLength).toUpperCase == normalizedString
                  )
                  .map(_.getValue)
                  .toRight(List(FormError(key, invalidKey, args)))
            }
        }

      override def unbind(key: String, value: Int): Map[String, String] = Map(key -> value.toString)
    }
}
