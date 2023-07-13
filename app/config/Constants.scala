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

package config

object Constants {

  val psaEnrolmentKey = "HMRC-PODS-ORG"
  val pspEnrolmentKey = "HMRC-PODSPP-ORG"

  val psaIdKey = "PSAID"
  val pspIdKey = "PSPID"

  val delimitedPSA = "DELIMITED_PSAID"
  val detailsNotFound = "no match found"

  val maxSchemeBankAccounts = 10
  val maxAccountingPeriods = 3

  val schemeMembersPageSize = 25
  val maxSchemeMembers = 300

  val maxCurrencyValue = 999999999.99
  val maxCashInBank = 999999999.99
  val maxAssetValue = 999999999.99
  val maxMoneyValue = 999999999.99

  val maxMembers = 999999

  val maxLoanPeriod = 999
  val maxPercentage = 100.0
}
