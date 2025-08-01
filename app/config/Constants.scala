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

package config

import java.time.LocalDate

object Constants {

  val UNCHANGED_SESSION_PREFIX = "pure_"
  val PREVIOUS_SUBMITTED_PREFIX = "previous_"

  val psaEnrolmentKey = "HMRC-PODS-ORG"
  val pspEnrolmentKey = "HMRC-PODSPP-ORG"

  val RETURN_PERIODS = "RETURN_PERIODS"
  val SUBMISSION_DATE = "SUBMISSION_DATE"
  val SRN = "SRN"
  val SUBMISSION_VIEWED_FLAG = "SUBMISSION_VIEWED_FLAG"
  val PREPOPULATION_FLAG = "prePopulationFlag"

  val psaIdKey = "PSAID"
  val pspIdKey = "PSPID"

  val PSA = "PSA"
  val PSP = "PSP"

  val TAX_YEAR = "taxYear"
  val VERSION = "version"
  val FB_NUMBER = "fbNumber"

  val delimitedPSA = "DELIMITED_PSAID"
  val detailsNotFound = "no match found"

  val maxAccountingPeriods = 3
  val maxLandOrProperties = 5000
  val maxLandOrPropertyDisposals = 50

  val schemeMembersPageSize = 25
  val maxSchemeMembers = 300
  val maxSharesTransactions = 5000
  val maxDisposalsPerShare = 50
  val maxBondsTransactions = 5000
  val maxDisposalPerBond = 50
  val maxOtherAssetsTransactions = 5000
  val maxDisposalPerOtherAsset = 50

  val pageSize = 25

  val loanPageSize = 25
  val landOrPropertiesSize = 25
  val landOrPropertyDisposalsSize = 25
  val transferInListSize = 25
  val transferOutListSize = 25
  val pclsInListSize = 25
  val surrenderedBenefitsListSize = 25
  val memberContributionsMemberListSize = 25
  val employerContributionsMemberListSize = 25
  val employerContributionsCYASize = 5
  val memberPensionPayments = 25
  val sharesDisposalListSize = 10
  val reportedSharesDisposalListSize = 25
  val bondsDisposalListSize = 10
  val otherAssetsPageSize = 25
  val reportingAssetsDisposalDisposalListSize = 10
  val reportedOtherAssetsDisposalListSize = 25
  val returnsSubmittedPageSize = 5

  val maxLoans = 5000
  val maxCurrencyValue = 999999999.99
  val maxCashInBank = 999999999.99
  val maxAssetValue = 999999999.99
  val maxPCLSAmount = 999999999.99
  val minAssetValue: Double = -999999999.99
  val maxMoneyValue = 999999999.99
  val minMoneyValue: Double = -999999999.99
  val minPosMoneyValue = 0.01
  val zeroMoneyValue = 0.00

  val minSharesHeld = 0
  val minShares = 1
  val maxShares = 9999999

  val minBondsHeld = 0
  val maxBonds = 9999999

  val maxSurrenderedBenefitAmount = 999999999.99
  val minTotalConsiderationAmount = 0.01
  val maxTotalConsiderationAmount = 999999999.99

  val maxMembers = 99999999

  val maxLoanPeriod = 999
  val minLoanPeriod = 1
  val minPercentage: Double = -999.99
  val maxPercentage = 999.99

  val borrowMinPercentage = 0
  val borrowMaxPercentage = 100
  val maxBorrows = 5000
  val borrowPageSize = 25

  val maxInputLength = 35
  val maxTextAreaLength = 160
  val maxOtherDescriptionLength = 35
  val maxNotRelevant = 9999999

  val textAreaRegex = """^[a-zA-Z0-9\-’`'" \t,.@/&()]+$"""
  val inputRegexQROPS = """^([Qq][0-9]{6})$"""
  val inputRegexPSTR = """^([0-9]{8}[A-Za-z]{2})$"""

  val postcodeCharsRegex = """^[a-zA-Z0-9 ]+$"""
  val postcodeFormatRegex =
    """^[A-Z]{1,2}[0-9][0-9A-Z]?\s?[0-9][A-Z]{2}|BFPO\s?[0-9]{1,3}$"""

  val earliestDate: LocalDate = LocalDate.of(1900, 1, 1)

  val defaultFbVersion = "000"
  val incomplete = "Incomplete"
}
