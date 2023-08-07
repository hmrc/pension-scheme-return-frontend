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

import eu.timepit.refined.refineMV
import models.NormalMode
import org.scalacheck.Arbitrary
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import pages.nonsipp.schemedesignatory.HowMuchCashPage

trait PageGenerators {

  implicit lazy val arbitraryCheckReturnDatesPage: Arbitrary[CheckReturnDatesPage] =
    Arbitrary(ModelGenerators.srnGen.map(CheckReturnDatesPage))

  implicit lazy val arbitraryAccountingPeriodPage: Arbitrary[AccountingPeriodPage] =
    Arbitrary(ModelGenerators.srnGen.map(AccountingPeriodPage(_, refineMV(1), NormalMode)))

  implicit lazy val arbitraryHowMuchCashPage: Arbitrary[HowMuchCashPage] =
    Arbitrary(ModelGenerators.srnGen.map(HowMuchCashPage))
}
