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

import eu.timepit.refined.refineMV
import models.requests.DataRequest
import models.{DateRange, UserAnswers}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.nonsipp.WhichTaxYearPage
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import play.api.test.FakeRequest
import utils.BaseSpec
import utils.UserAnswersUtils._

import java.time.LocalDate

class SchemeDateServiceSpec extends BaseSpec with ScalaCheckPropertyChecks {

  import Behaviours._

  val service = new SchemeDateServiceImpl()

  val defaultUserAnswers = UserAnswers("id")
  val srn = srnGen.sample.value
  val allowedAccessRequest = allowedAccessRequestGen(FakeRequest()).sample.value

  val oldestDateRange =
    dateRangeWithinRangeGen(
      DateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2010, 1, 1))
    )

  val newestDateRange =
    dateRangeWithinRangeGen(
      DateRange(LocalDate.of(2011, 1, 1), LocalDate.of(2020, 1, 1))
    )

  def schemeDateTest[A](name: String, action: DataRequest[_] => A, expected: DateRange => A): Behaviours =
    MultipleBehaviourTests(
      s"SchemeDateService.$name",
      List(
        "return None when nothing is in cache".hasBehaviour {

          val request = DataRequest(allowedAccessRequest, defaultUserAnswers)
          action(request) mustBe None
        },
        s"choose the date from WhichTaxYearPage answer when no accounting periods exist".hasBehaviour {

          forAll(dateRangeGen) { range =>
            val userAnswers = defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), range)
            val request = DataRequest(allowedAccessRequest, userAnswers)

            action(request) mustBe Some(expected(range))
          }
        },
        s"choose $name from AccountingPeriodPage answer when 1 period present".hasBehaviour {

          forAll(dateRangeGen, dateRangeGen) { (whichTaxYearPage, accountingPeriod) =>
            val userAnswers = defaultUserAnswers
              .unsafeSet(WhichTaxYearPage(srn), whichTaxYearPage)
              .unsafeSet(AccountingPeriodPage(srn, refineMV(1)), accountingPeriod)

            val request = DataRequest(allowedAccessRequest, userAnswers)

            action(request) mustBe Some(expected(accountingPeriod))
          }
        },
        s"choose $name from AccountingPeriodPage answer when multiple exist".hasBehaviour {

          forAll(dateRangeGen, oldestDateRange, newestDateRange) {

            (whichTaxYearPage, oldestAccountingPeriod, newestAccountingPeriod) =>
              val userAnswers = defaultUserAnswers
                .unsafeSet(WhichTaxYearPage(srn), whichTaxYearPage)
                .unsafeSet(AccountingPeriodPage(srn, refineMV(1)), newestAccountingPeriod)
                .unsafeSet(AccountingPeriodPage(srn, refineMV(2)), oldestAccountingPeriod)

              val request = DataRequest(allowedAccessRequest, userAnswers)

              action(request) mustBe Some(expected(newestAccountingPeriod))
          }
        }
      )
    )

  act.like(schemeDateTest("schemeDate", service.schemeDate(srn)(_), identity))
  act.like(schemeDateTest("schemeStartDate", service.schemeStartDate(srn)(_), _.from))
  act.like(schemeDateTest("schemeEndDate", service.schemeEndDate(srn)(_), _.to))

}
