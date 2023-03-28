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

package services

import com.google.inject.ImplementedBy
import models.DateRange
import models.SchemeId.Srn
import models.requests.DataRequest
import pages.{AccountingPeriods, WhichTaxYearPage}

import java.time.LocalDate
import javax.inject.Inject

class SchemeDateServiceImpl @Inject()() extends SchemeDateService {

  def schemeDate(srn: Srn)(implicit request: DataRequest[_]): Option[DateRange] =
    accountingPeriod(srn).orElse(whichTaxYear(srn))

  private def accountingPeriod(srn: Srn)(implicit request: DataRequest[_]): Option[DateRange] =
    request.userAnswers.get(AccountingPeriods(srn)).flatMap(_.sorted.headOption)

  private def whichTaxYear(srn: Srn)(implicit request: DataRequest[_]): Option[DateRange] =
    request.userAnswers.get(WhichTaxYearPage(srn))
}

@ImplementedBy(classOf[SchemeDateServiceImpl])
trait SchemeDateService {

  def schemeStartDate(srn: Srn)(implicit request: DataRequest[_]): Option[LocalDate] =
    schemeDate(srn).map(_.from)

  def schemeEndDate(srn: Srn)(implicit request: DataRequest[_]): Option[LocalDate] =
    schemeDate(srn).map(_.to)

  def schemeDate(srn: Srn)(implicit request: DataRequest[_]): Option[DateRange]
}
