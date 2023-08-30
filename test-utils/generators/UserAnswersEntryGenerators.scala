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

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import pages.nonsipp.landorproperty.LandPropertyIndividualSellersNamePage
import play.api.libs.json.{JsValue, Json}

trait UserAnswersEntryGenerators extends PageGenerators {

  implicit lazy val arbitraryLandPropertyIndividualSellersNameUserAnswersEntry
    : Arbitrary[(LandPropertyIndividualSellersNamePage.type, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[LandPropertyIndividualSellersNamePage.type]
        value <- arbitrary[String].suchThat(_.nonEmpty).map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryCheckReturnDatesUserAnswersEntry: Arbitrary[(CheckReturnDatesPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[CheckReturnDatesPage]
        value <- arbitrary[Boolean].map(Json.toJson(_))
      } yield (page, value)
    }

  implicit lazy val arbitraryAccountingPeriodUserAnswersEntry: Arbitrary[(AccountingPeriodPage, JsValue)] =
    Arbitrary {
      for {
        page <- arbitrary[AccountingPeriodPage]
        value <- ModelGenerators.dateRangeGen.map(Json.toJson(_))
      } yield (page, value)
    }
}
