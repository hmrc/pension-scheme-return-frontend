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

import forms.mappings.errors.{
  DoubleFormErrors,
  IntFormErrors,
  MoneyFormErrors,
  PercentageFormErrors,
  SecurityFormErrors
}
import models.{Enumerable, Money, Percentage, Security}
import play.api.data.FormError
import play.api.data.format.Formatter

import java.text.DecimalFormat
import scala.util.control.Exception.nonFatalCatch

trait Formatters {

  private[mappings] def stringFormatter(errorKey: String, args: Seq[Any] = Seq.empty): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        data.get(key) match {
          case None => Left(Seq(FormError(key, errorKey, args)))
          case Some(s) if s.trim.isEmpty => Left(Seq(FormError(key, errorKey, args)))
          case Some(s) => Right(s)
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    }

  private[mappings] def booleanFormatter(
    requiredKey: String,
    invalidKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Boolean] =
    new Formatter[Boolean] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .flatMap {
            case "true" => Right(true)
            case "false" => Right(false)
            case _ => Left(Seq(FormError(key, invalidKey, args)))
          }

      def unbind(key: String, value: Boolean) = Map(key -> value.toString)
    }

  private[mappings] val unitFormatter: Formatter[Unit] =
    new Formatter[Unit] {
      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Unit] = Right(())

      override def unbind(key: String, value: Unit): Map[String, String] = Map(key -> "unit")
    }

  private[mappings] def intFormatter(
    errors: IntFormErrors,
    args: Seq[String] = Seq.empty
  ): Formatter[Int] =
    new Formatter[Int] {

      val decimalRegexp = """^-?(\d*\.\d*)$"""
      val intRegex = """^[0-9]+$"""

      private val baseFormatter = stringFormatter(errors.requiredKey, args)

      override def bind(key: String, data: Map[String, String]) =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", ""))
          .map(_.replaceAll(" ", ""))
          .flatMap {
            case s if s.matches(decimalRegexp) =>
              Left(Seq(FormError(key, errors.wholeNumberKey, args)))
            case s =>
              nonFatalCatch
                .either(s.toInt)
                .left
                .map { _ =>
                  if (s.matches(intRegex)) Seq(FormError(key, errors.max._2, args))
                  else Seq(FormError(key, errors.nonNumericKey, args))
                }
          }
          .flatMap[Seq[FormError], Int] { int =>
            val (maxSize, maxError) = errors.max
            val (minSize, minError) = errors.min
            for {
              _ <- if (int > maxSize) Left(Seq(FormError(key, maxError, args))) else Right(int)
              _ <- if (int < minSize) Left(Seq(FormError(key, minError, args))) else Right(int)
            } yield int
          }

      override def unbind(key: String, value: Int) =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def doubleFormatter(
    doubleFormErrors: DoubleFormErrors,
    args: Seq[String]
  ): Formatter[Double] =
    doubleFormatter(
      doubleFormErrors.requiredKey,
      doubleFormErrors.nonNumericKey,
      doubleFormErrors.min,
      doubleFormErrors.max,
      args
    )

  private[mappings] def doubleFormatter(
    requiredKey: String,
    nonNumericKey: String,
    min: Option[(Double, String)] = None,
    max: (Double, String),
    args: Seq[String] = Seq.empty
  ): Formatter[Double] =
    new Formatter[Double] {

      private val baseFormatter = stringFormatter(requiredKey, args)
      private val (maxSize, maxError) = max
      private val decimalRegex = "^%?[0-9]+(\\.[0-9]{1,2})?%?$"

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Double] =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", "").replace("%", ""))
          .flatMap { s =>
            s.toDoubleOption
              .toRight(Seq(FormError(key, nonNumericKey, args)))
              .flatMap { double =>
                val minCheck = min match {
                  case Some((minSize, minError)) if minSize < double => Left(Seq(FormError(key, minError, args)))
                  case _ => Right(double)
                }

                for {
                  _ <- minCheck
                  _ <- if (double > maxSize) Left(Seq(FormError(key, maxError, args))) else Right(double)
                  _ <- if (double.toString.matches(decimalRegex)) Right(double)
                  else Left(Seq(FormError(key, nonNumericKey)))
                } yield double
              }
          }

      override def unbind(key: String, value: Double): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def doubleFormatter(
    requiredKey: String,
    nonNumericKey: String,
    min: (Double, String),
    max: (Double, String),
    args: Seq[String]
  ): Formatter[Double] =
    new Formatter[Double] {

      private val baseFormatter = stringFormatter(requiredKey, args)
      private val (maxSize, maxError) = max
      private val (minSize, minError) = min
      private val decimalRegex = "^%?[0-9]+(\\.[0-9]{1,2})?%?$"

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Double] =
        baseFormatter
          .bind(key, data)
          .map(_.replace(",", "").replace("%", ""))
          .flatMap { s =>
            s.toDoubleOption
              .toRight(Seq(FormError(key, nonNumericKey, args)))
              .flatMap { double =>
                if (double < minSize) Left(Seq(FormError(key, minError, args)))
                else if (double > maxSize) Left(Seq(FormError(key, maxError, args)))
                else if (double.toString().matches(decimalRegex))
                  Right(double)
                else Right(double)
              }
          }

      override def unbind(key: String, value: Double): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def moneyFormatter(
    errors: MoneyFormErrors,
    args: Seq[String] = Seq.empty
  ): Formatter[Money] =
    new Formatter[Money] {

      private val baseFormatter =
        doubleFormatter(errors.requiredKey, errors.nonNumericKey, min = errors.min, max = errors.max, args)
      private val decimalRegex = "^-?\\d+(\\.\\d{1,2})?$"

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Money] =
        baseFormatter
          .bind(key, data.view.mapValues(_.replace("£", "")).toMap)
          .flatMap { double =>
            if (BigDecimal(double).toString().matches(decimalRegex))
              Right(Money(double, new DecimalFormat("#,##0.00").format(double)))
            else
              Left(Seq(FormError(key, errors.nonNumericKey, args)))
          }

      override def unbind(key: String, value: Money): Map[String, String] =
        Map(key -> value.displayAs)
    }

  private[mappings] def percentageFormatter(
    errors: PercentageFormErrors,
    args: Seq[String] = Seq.empty
  ): Formatter[Percentage] =
    new Formatter[Percentage] {

      private val baseFormatter =
        doubleFormatter(errors.requiredKey, errors.nonNumericKey, min = errors.min, max = errors.max, args)
      private val decimalRegex = """^%?-?\d+(?:\.\d{1,2})?$"""

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Percentage] =
        baseFormatter
          .bind(key, data.view.mapValues(_.replace("%", "")).toMap)
          .flatMap { double =>
            if (double.toString().matches(decimalRegex))
              Right(Percentage(double, data(key).replace("%", "")))
            else
              Left(Seq(FormError(key, errors.nonNumericKey, args)))
          }

      override def unbind(key: String, value: Percentage): Map[String, String] =
        Map(key -> value.displayAs)
    }

  private[mappings] def securityFormatter(
    errors: SecurityFormErrors,
    args: Seq[String] = Seq.empty
  ): Formatter[Security] =
    new Formatter[Security] {

      private val baseFormatter =
        doubleFormatter(errors.requiredKey, errors.nonNumericKey, min = None, errors.max, args)
      private val decimalRegex = "^-?\\d+(\\.\\d{1,2})?$"

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Security] =
        baseFormatter
          .bind(key, data.view.toMap)
          .flatMap { double =>
            if (BigDecimal(double).toString().matches(decimalRegex))
              Right(Security(double.toString))
            else
              Left(Seq(FormError(key, errors.nonNumericKey, args)))
          }

      override def unbind(key: String, value: Security): Map[String, String] =
        Map(key -> value.security)
    }

  private[mappings] def enumerableFormatter[A](requiredKey: String, invalidKey: String, args: Seq[String] = Seq.empty)(
    implicit ev: Enumerable[A]
  ): Formatter[A] =
    new Formatter[A] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], A] =
        baseFormatter.bind(key, data).flatMap { str =>
          ev.get(str)
            .map(Right.apply)
            .getOrElse(Left(Seq(FormError(key, invalidKey, args))))
        }

      override def unbind(key: String, value: A): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }
}
