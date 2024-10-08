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

package services

import com.google.inject.ImplementedBy
import config.RefinedTypes.{Max3, OneToThree}
import eu.timepit.refined.refineV
import pages.nonsipp.accountingperiod.AccountingPeriods
import models.DateRange
import models.requests.psr.MinimalRequiredSubmission.nonEmptyListFormat
import models.requests.DataRequest
import cats.data.NonEmptyList
import models.SchemeId.Srn
import pages.nonsipp.WhichTaxYearPage
import play.api.libs.json.Json

import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}

@Singleton
class SchemeDateServiceImpl @Inject() extends SchemeDateService {

  def now(): LocalDateTime = LocalDateTime.now(ZoneId.of("Europe/London"))

  def schemeDate(srn: Srn)(implicit request: DataRequest[_]): Option[DateRange] = {
    val accountingPeriods = request.userAnswers.list(AccountingPeriods(srn))
    if (accountingPeriods.isEmpty) {
      whichTaxYear(srn)
    } else {
      Some(
        DateRange(
          accountingPeriods.sorted.reverse.headOption.get.from,
          accountingPeriods.sorted.headOption.get.to
        )
      )
    }
  }

  def returnPeriods(srn: Srn)(implicit request: DataRequest[_]): Option[NonEmptyList[DateRange]] = {
    val accountingPeriods = request.userAnswers.list(AccountingPeriods(srn))

    if (accountingPeriods.isEmpty) {
      request.userAnswers.get(WhichTaxYearPage(srn)).map(NonEmptyList.one)
    } else {
      NonEmptyList.fromList(accountingPeriods)
    }
  }

  def taxYearOrAccountingPeriods(
    srn: Srn
  )(implicit request: DataRequest[_]): Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]] =
    request.userAnswers.list(AccountingPeriods(srn)) match {
      case Nil => request.userAnswers.get(WhichTaxYearPage(srn)).map(Left(_))
      case head :: rest =>
        NonEmptyList
          .of(head, rest: _*)
          .zipWithIndex
          .traverse {
            case (date, index) => refineV[OneToThree](index + 1).toOption.map(refined => date -> refined)
          }
          .map(Right(_))
    }
  def returnPeriodsAsJsonString(srn: Srn)(implicit request: DataRequest[_]): String =
    Json.prettyPrint(Json.toJson(returnPeriods(srn)))

  def submissionDateAsString(localDateTime: LocalDateTime): String =
    localDateTime.format(DateTimeFormatter.ISO_DATE_TIME)

  private def whichTaxYear(srn: Srn)(implicit request: DataRequest[_]): Option[DateRange] =
    request.userAnswers.get(WhichTaxYearPage(srn))

}

@ImplementedBy(classOf[SchemeDateServiceImpl])
trait SchemeDateService {

  def now(): LocalDateTime

  def schemeStartDate(srn: Srn)(implicit request: DataRequest[_]): Option[LocalDate] =
    schemeDate(srn).map(_.from)

  def schemeEndDate(srn: Srn)(implicit request: DataRequest[_]): Option[LocalDate] =
    schemeDate(srn).map(_.to)

  def schemeDate(srn: Srn)(implicit request: DataRequest[_]): Option[DateRange]

  def returnPeriods(srn: Srn)(implicit request: DataRequest[_]): Option[NonEmptyList[DateRange]]

  def taxYearOrAccountingPeriods(
    srn: Srn
  )(implicit request: DataRequest[_]): Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]

  def returnPeriodsAsJsonString(srn: Srn)(implicit request: DataRequest[_]): String

  def submissionDateAsString(localDateTime: LocalDateTime): String
}
