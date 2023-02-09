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

package generators

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen.{alphaNumChar, alphaNumStr, alphaStr, choose, chooseNum, listOfN, nonEmptyListOf}
import play.api.mvc.Call
import viewmodels.DisplayMessage.SimpleMessage

import java.time.{Instant, LocalDate, ZoneOffset}

trait BasicGenerators {

  def genIntersperseString(gen: Gen[String],
                           value: String,
                           frequencyV: Int = 1,
                           frequencyN: Int = 10): Gen[String] = {

    val genValue: Gen[Option[String]] = Gen.frequency(frequencyN -> None, frequencyV -> Gen.const(Some(value)))

    for {
      seq1 <- gen
      seq2 <- Gen.listOfN(seq1.length, genValue)
    } yield {
      seq1.toSeq.zip(seq2).foldLeft("") {
        case (acc, (n, Some(v))) =>
          acc + n + v
        case (acc, (n, _)) =>
          acc + n
      }
    }
  }

  def intsInRangeWithCommas(min: Int, max: Int): Gen[String] = {
    val numberGen = choose[Int](min, max).map(_.toString)
    genIntersperseString(numberGen, ",")
  }

  def intsLargerThanMaxValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x > Int.MaxValue)

  def intsSmallerThanMinValue: Gen[BigInt] =
    arbitrary[BigInt] suchThat (x => x < Int.MinValue)

  def nonNumerics: Gen[String] =
    alphaStr suchThat (_.nonEmpty)

  def decimals: Gen[String] =
    arbitrary[BigDecimal]
      .suchThat(_.abs < Int.MaxValue)
      .suchThat(!_.isValidInt)
      .map(_.bigDecimal.toPlainString)

  def intsBelowValue(value: Int): Gen[Int] =
    arbitrary[Int] suchThat (_ < value)

  def intsAboveValue(value: Int): Gen[Int] =
    arbitrary[Int] suchThat (_ > value)

  def intsOutsideRange(min: Int, max: Int): Gen[Int] =
    arbitrary[Int] suchThat (x => x < min || x > max)

  def nonBooleans: Gen[String] =
    arbitrary[String]
      .suchThat(_.nonEmpty)
      .suchThat(_ != "true")
      .suchThat(_ != "false")

  def nonEmptyString: Gen[String] =
    for {
      c <- alphaNumChar
      s <- alphaNumStr
    } yield s"$c$s"

  def stringsWithMaxLength(maxLength: Int): Gen[String] =
    for {
      length <- choose(1, maxLength)
      chars <- listOfN(length, arbitrary[Char])
    } yield chars.mkString

  def stringsLongerThan(minLength: Int): Gen[String] = for {
    maxLength <- Gen.const((minLength * 2).max(100))
    length    <- Gen.chooseNum(minLength + 1, maxLength)
    chars     <- listOfN(length, arbitrary[Char])
  } yield chars.mkString

  def stringsExceptSpecificValues(excluded: Seq[String]): Gen[String] =
    nonEmptyString suchThat (!excluded.contains(_))

  def stringContains(value: String): Gen[String] =
    for {
      s      <- nonEmptyString
      i      <- chooseNum(0, s.length)
      (l, r) = s.splitAt(i)
    } yield s"$l$value$r"

  def oneOf[T](xs: Seq[Gen[T]]): Gen[T] =
    Gen.oneOf(xs).flatMap(identity)

  def datesBetween(min: LocalDate, max: LocalDate): Gen[LocalDate] = {

    def toMillis(date: LocalDate): Long =
      date.atStartOfDay.atZone(ZoneOffset.UTC).toInstant.toEpochMilli

    Gen.choose(toMillis(min), toMillis(max)).map {
      millis =>
        Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate
    }
  }

  val earliestDate: LocalDate = LocalDate.of(1970, 1, 1)
  val latestDate: LocalDate = LocalDate.of(3000, 12, 31)

  def date: Gen[LocalDate] =
    datesBetween(earliestDate, latestDate)

  val nonEmptySimpleMessage: Gen[SimpleMessage] = nonEmptyString.map(SimpleMessage(_))

  def tupleOf[A, B](genA: Gen[A], genB: Gen[B]): Gen[(A, B)] =
    for {
      a <- genA
      b <- genB
    } yield a -> b

  val boolean: Gen[Boolean] =
    Gen.oneOf(true, false)

  val topLevelDomain: Gen[String] =
    Gen.oneOf("com", "gov.uk", "co.uk", "net", "org", "io")

  val email: Gen[String] =
    for {
      username  <- nonEmptyString
      domain    <- nonEmptyString
      topDomain <- topLevelDomain
    } yield s"$username@$domain.$topDomain"

  val ipAddress: Gen[String] =
    for {
      a <- choose(1, 255)
      b <- choose(0, 255)
      c <- choose(0, 255)
      d <- choose(0, 255)
    } yield s"$a.$b.$c.$d"

  val relativeUrl: Gen[String] =
    nonEmptyListOf(nonEmptyString).map(_.mkString("/", "/", "/"))

  val httpMethod: Gen[String] = Gen.oneOf("GET", "POST")
  val call: Gen[Call] = {
    for {
      method <- httpMethod
      url    <- relativeUrl
    } yield Call(method, url)
  }
}