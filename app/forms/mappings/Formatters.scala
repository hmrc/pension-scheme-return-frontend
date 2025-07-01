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
import models.GenericFormMapper.StringFieldMapper
import uk.gov.voa.play.form.Condition
import models._
import play.api.data.{FormError, Mapping}
import forms.mappings.errors._

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

  // like stringFormatter, but targets a specific field key
  private[mappings] def stringFormatterWithKey(
    fieldKey: String,
    errorKey: String,
    args: Seq[Any] = Seq.empty
  ): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        data.get(fieldKey) match {
          case None => Left(Seq(FormError(fieldKey, errorKey, args)))
          case Some(s) if s.trim.isEmpty => Left(Seq(FormError(fieldKey, errorKey, args)))
          case Some(s) => Right(s)
        }

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value)
    }

  private[mappings] def conditionalFormatter[A](l: List[(Condition, Option[Mapping[A]])], prePopKey: Option[String])(
    implicit ev: StringFieldMapper[A]
  ): Formatter[Option[A]] =
    new Formatter[Option[A]] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[A]] =
        l.collectFirst {
          case (condition, Some(mapping)) if condition(data) =>
            mapping.bind(data).map(Some(_))
        }.getOrElse(Right(None))

      override def unbind(key: String, value: Option[A]): Map[String, String] =
        value
          .flatMap(
            ev.from(_).map { value =>
              val key = prePopKey.fold("conditional")(k => s"$k-conditional")
              Map(key -> value)
            }
          )
          .getOrElse(Map.empty)
    }

  private[mappings] def optionalStringFormatter(): Formatter[String] =
    new Formatter[String] {

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] =
        Right(data.getOrElse(key, "").trim)

      override def unbind(key: String, value: String): Map[String, String] =
        Map(key -> value.trim)
    }

  private[mappings] def booleanFormatter(
    requiredKey: String,
    invalidKey: String,
    args: Seq[String] = Seq.empty
  ): Formatter[Boolean] =
    new Formatter[Boolean] {

      private val baseFormatter = stringFormatter(requiredKey, args)

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Boolean] =
        baseFormatter
          .bind(key, data)
          .flatMap {
            case "true" => Right(true)
            case "false" => Right(false)
            case _ => Left(Seq(FormError(key, invalidKey, args)))
          }

      def unbind(key: String, value: Boolean): Map[String, String] = Map(key -> value.toString)
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

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Int] =
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
                .fold(
                  _ =>
                    if (s.matches(intRegex)) {
                      Left(Seq(FormError(key, errors.max._2, args)))
                    } else {
                      Left(Seq(FormError(key, errors.nonNumericKey, args)))
                    },
                  value =>
                    if (value > errors.max._1) {
                      Left(Seq(FormError(key, errors.max._2, args)))
                    } else if (value < errors.min._1 && errors.min._1 == 0) {
                      // deliberately displaying nonNumericKey error message here
                      Left(Seq(FormError(key, errors.nonNumericKey, args)))
                    } else if (value < errors.min._1) {
                      Left(Seq(FormError(key, errors.min._2, args)))
                    } else {
                      Right(value)
                    }
                )
          }
          .flatMap { int =>
            errors.max match {
              case (max, error) if int > max =>
                Left(Seq(FormError(key, error, args)))
              case _ =>
                Right(int)
            }
          }

      override def unbind(key: String, value: Int): Map[String, String] =
        baseFormatter.unbind(key, value.toString)
    }

  private[mappings] def doubleFormatter(
    requiredKey: String,
    nonNumericKey: String,
    max: (Double, String),
    min: (Double, String),
    args: Seq[String] = Seq.empty
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
                if (double > maxSize) {
                  Left(Seq(FormError(key, maxError, args)))
                } else if (double < minSize && minSize == 0) {
                  // deliberately displaying nonNumericKey error message here
                  Left(Seq(FormError(key, nonNumericKey, args)))
                } else if (double < minSize) {
                  Left(Seq(FormError(key, minError, args)))
                } else if (double.toString.matches(decimalRegex)) {
                  Right(double)
                } else {
                  Right(double)
                }
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
        doubleFormatter(errors.requiredKey, errors.nonNumericKey, errors.max, errors.min, args)
      private val decimalRegex = """^-?\d{1,9}(?:\.\d{1,2})?$"""

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Money] =
        baseFormatter
          .bind(key, data.view.mapValues(_.replace("£", "").filterNot(_.isWhitespace)).toMap)
          .flatMap { double =>
            if (BigDecimal(double).bigDecimal.toPlainString.matches(decimalRegex)) {
              Right(Money(double))
            } else {
              Left(Seq(FormError(key, errors.nonNumericKey, args)))
            }
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
        doubleFormatter(errors.requiredKey, errors.nonNumericKey, errors.max, errors.min, args)
      private val decimalRegex = """^%?-?\d+(?:\.\d{1,2})?$"""

      override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Percentage] =
        baseFormatter
          .bind(key, data.view.mapValues(_.replace("%", "").filterNot(_.isWhitespace)).toMap)
          .flatMap { double =>
            if (double.toString.matches(decimalRegex)) {
              Right(Percentage(double, data(key).replace("%", "").filterNot(_.isWhitespace)))
            } else {
              Left(Seq(FormError(key, errors.nonNumericKey, args)))
            }
          }

      override def unbind(key: String, value: Percentage): Map[String, String] =
        Map(key -> value.displayAs)
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
