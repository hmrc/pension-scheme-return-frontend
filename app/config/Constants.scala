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

import java.time.LocalDate

object Constants {

  val psaEnrolmentKey = "HMRC-PODS-ORG"
  val pspEnrolmentKey = "HMRC-PODSPP-ORG"

  val psaIdKey = "PSAID"
  val pspIdKey = "PSPID"

  val delimitedPSA = "DELIMITED_PSAID"
  val detailsNotFound = "no match found"

  val maxSchemeBankAccounts = 10
  val maxAccountingPeriods = 3
  val maxLandOrProperties = 5000
  val maxLandOrPropertyDisposals = 50

  val schemeMembersPageSize = 25
  val maxSchemeMembers = 300

  val loanPageSize = 25
  val landOrPropertiesSize = 25
  val landOrPropertyDisposalsSize = 25
  val transferInListSize = 25
  val employerContributionsMemberListSize = 25

  val maxLoans = 9999999
  val maxCurrencyValue = 999999999.99
  val maxCashInBank = 999999999.99
  val maxAssetValue = 999999999.99
  val minAssetValue = -999999999.99
  val maxMoneyValue = 999999999.99
  val minMoneyValue = -999999999.99

  val unallocatedContributionMin = 0.01

  val maxMembers = 99999999

  val maxLoanPeriod = 999
  val minPercentage = -999.99
  val maxPercentage = 999.99

  val borrowMinPercentage = 0
  val borrowMaxPercentage = 100
  val maxBorrows = 5000
  val borrowPageSize = 25

  val maxInputLength = 35
  val maxTextAreaLength = 160
  val maxTitleNumberLength = 35

  val textAreaRegex = """^[a-zA-Z0-9\-'" \t\r\n,.@/]+$"""
  val titleNumberRegex = """^[a-zA-Z]{2,3}[0-9]+$"""

  val postcodeCharsRegex = """^[a-zA-Z0-9 ]+$"""
  val postcodeFormatRegex =
    """^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,3}$"""

  val earliestDate = LocalDate.of(1900, 1, 1)
}
