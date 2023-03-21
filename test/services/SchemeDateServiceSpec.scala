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
import pages.{AccountingPeriodPage, WhichTaxYearPage}
import play.api.test.FakeRequest
import utils.BaseSpec

import java.time.LocalDate

class SchemeDateServiceSpec extends BaseSpec with ScalaCheckPropertyChecks {

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

  List[(String, DataRequest[_] => Option[LocalDate], DateRange => LocalDate)](
    ("schemeStartDate", service.schemeStartDate(srn)(_), _.from),
    ("schemeEndDate", service.schemeEndDate(srn)(_), _.to)
  ).foreach {
    case (name, action, expected) =>
      s"SchemeDateService.$name" should {

        "return None when nothing is in cache" in {

          val request = DataRequest(allowedAccessRequest, defaultUserAnswers)
          action(request) mustBe None
        }

        s"choose the $name from WhichTaxYearPage answer" when {

          "no accounting periods found" in {

            forAll(dateRangeGen) { range =>
              val userAnswers = defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), range)
              val request = DataRequest(allowedAccessRequest, userAnswers)

              action(request) mustBe Some(expected(range))
            }
          }
        }

        s"choose $name from AccountingPeriodPage answer" when {

          "only 1 is present" in {

            forAll(dateRangeGen, dateRangeGen) { (whichTaxYearPage, accountingPeriod) =>
              val userAnswers = defaultUserAnswers
                .unsafeSet(WhichTaxYearPage(srn), whichTaxYearPage)
                .unsafeSet(AccountingPeriodPage(srn, refineMV(1)), accountingPeriod)
              val request = DataRequest(allowedAccessRequest, userAnswers)

              action(request) mustBe Some(expected(accountingPeriod))
            }
          }

          "only more present" in {

            forAll(dateRangeGen, oldestDateRange, newestDateRange) {
              (whichTaxYearPage, oldestAccountingPeriod, newestAccountingPeriod) =>
                val userAnswers = defaultUserAnswers
                  .unsafeSet(WhichTaxYearPage(srn), whichTaxYearPage)
                  .unsafeSet(AccountingPeriodPage(srn, refineMV(1)), oldestAccountingPeriod)
                  .unsafeSet(AccountingPeriodPage(srn, refineMV(1)), newestAccountingPeriod)
                val request = DataRequest(allowedAccessRequest, userAnswers)

                action(request) mustBe Some(expected(newestAccountingPeriod))
            }
          }
        }
      }
  }
}
