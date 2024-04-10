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

import play.api.test.FakeRequest
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.BaseSpec
import play.api.mvc.AnyContentAsEmpty
import cats.data.NonEmptyList
import eu.timepit.refined.refineMV
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import pages.nonsipp.WhichTaxYearPage
import models.{DateRange, NormalMode, UserAnswers}
import models.requests.{AllowedAccessRequest, DataRequest}
import utils.UserAnswersUtils._
import org.scalacheck.Gen

import java.time.LocalDate

class SchemeDateServiceSpec extends BaseSpec with ScalaCheckPropertyChecks {

  import Behaviours._

  val service = new SchemeDateServiceImpl()

  val defaultUserAnswers: UserAnswers = UserAnswers("id")
  val srn = srnGen.sample.value
  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value

  val oldestDateRange: Gen[DateRange] =
    dateRangeWithinRangeGen(
      DateRange(LocalDate.of(2000, 1, 1), LocalDate.of(2010, 1, 1))
    )

  val newestDateRange: Gen[DateRange] =
    dateRangeWithinRangeGen(
      DateRange(LocalDate.of(2011, 1, 1), LocalDate.of(2020, 1, 1))
    )

  def schemeDateTest[A](name: String, action: DataRequest[_] => A, expected: NonEmptyList[DateRange] => A): Behaviours =
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

            action(request) mustBe Some(expected(NonEmptyList.one(range)))
          }
        },
        s"choose $name from AccountingPeriodPage answer when 1 period present".hasBehaviour {

          forAll(dateRangeGen, dateRangeGen) { (whichTaxYearPage, accountingPeriod) =>
            val userAnswers = defaultUserAnswers
              .unsafeSet(WhichTaxYearPage(srn), whichTaxYearPage)
              .unsafeSet(AccountingPeriodPage(srn, refineMV(1), NormalMode), accountingPeriod)

            val request = DataRequest(allowedAccessRequest, userAnswers)

            action(request) mustBe Some(expected(NonEmptyList.one(accountingPeriod)))
          }
        },
        s"choose $name from AccountingPeriodPage answer when multiple exist".hasBehaviour {

          forAll(dateRangeGen, oldestDateRange, newestDateRange) {

            (whichTaxYearPage, oldestAccountingPeriod, newestAccountingPeriod) =>
              val userAnswers = defaultUserAnswers
                .unsafeSet(WhichTaxYearPage(srn), whichTaxYearPage)
                .unsafeSet(AccountingPeriodPage(srn, refineMV(1), NormalMode), newestAccountingPeriod)
                .unsafeSet(AccountingPeriodPage(srn, refineMV(2), NormalMode), oldestAccountingPeriod)

              val request = DataRequest(allowedAccessRequest, userAnswers)

              action(request) mustBe Some(expected(NonEmptyList.of(newestAccountingPeriod, oldestAccountingPeriod)))
          }
        }
      )
    )

  act.like(schemeDateTest("schemeDate", service.schemeDate(srn)(_), _.head))
  act.like(schemeDateTest("schemeStartDate", service.schemeStartDate(srn)(_), _.head.from))
  act.like(schemeDateTest("schemeEndDate", service.schemeEndDate(srn)(_), _.head.to))
  act.like(schemeDateTest("returnPeriods", service.returnPeriods(srn)(_), identity))
}
