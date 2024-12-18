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

package utils.nonsipp.check

import models.IdentityType._
import utils.nonsipp.check.LoansCheckStatusUtils.{checkLoansRecord, checkLoansSection}
import org.scalatest.OptionValues
import models._
import pages.nonsipp.common._
import models.SponsoringOrConnectedParty.{ConnectedParty, Neither, Sponsoring}
import org.scalatest.matchers.must.Matchers
import models.ConditionalYesNo._
import config.RefinedTypes.Max5000
import controllers.ControllerBaseSpec
import pages.nonsipp.loansmadeoroutstanding._
import models.IdentitySubject.LoanRecipient

class LoansCheckStatusUtilsSpec extends ControllerBaseSpec with Matchers with OptionValues {

  private val conditionalYesSecurity: ConditionalYes[Security] = ConditionalYesNo.yes(security)
  private val conditionalYesArrears: ConditionalYes[Money] = ConditionalYesNo.yes(money)

  private val schemeHadLoansTrue = defaultUserAnswers.unsafeSet(LoansMadeOrOutstandingPage(srn), true)
  private val schemeHadLoansFalse = defaultUserAnswers.unsafeSet(LoansMadeOrOutstandingPage(srn), false)

  private def addLoansBaseAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(DatePeriodLoanPage(srn, index), (localDate, money, loanPeriod))
      .unsafeSet(AreRepaymentsInstalmentsPage(srn, index), true)
      .unsafeSet(SecurityGivenForLoanPage(srn, index), conditionalYesSecurity)

  private def addLoansPartialAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(AmountOfTheLoanPage(srn, index), partialAmountOfTheLoan)
      .unsafeSet(InterestOnLoanPage(srn, index), partialInterestOnLoan)

  // Branching on IdentityTypePage
  private def addLoansIndividualAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LoanRecipient), Individual)
      .unsafeSet(IndividualRecipientNamePage(srn, index), name)
      .unsafeSet(IndividualRecipientNinoPage(srn, index), conditionalYesNoNino)
      .unsafeSet(IsIndividualRecipientConnectedPartyPage(srn, index), true)

  private def addLoansUKCompanyAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LoanRecipient), UKCompany)
      .unsafeSet(CompanyRecipientNamePage(srn, index), name)
      .unsafeSet(CompanyRecipientCrnPage(srn, index, LoanRecipient), conditionalYesNoCrn)
      .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index), Sponsoring)

  private def addLoansUKPartnershipAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LoanRecipient), UKPartnership)
      .unsafeSet(PartnershipRecipientNamePage(srn, index), name)
      .unsafeSet(PartnershipRecipientUtrPage(srn, index, LoanRecipient), conditionalYesNoUtr)
      .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index), ConnectedParty)

  private def addLoansOtherAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, LoanRecipient), Other)
      .unsafeSet(OtherRecipientDetailsPage(srn, index, LoanRecipient), otherRecipientDetails)
      .unsafeSet(RecipientSponsoringEmployerConnectedPartyPage(srn, index), Neither)

  private def addLoansPrePopAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(AmountOfTheLoanPage(srn, index), amountOfTheLoan)
      .unsafeSet(InterestOnLoanPage(srn, index), interestOnLoan)
      .unsafeSet(ArrearsPrevYears(srn, index), true)
      .unsafeSet(OutstandingArrearsOnLoanPage(srn, index), conditionalYesArrears)

  "checkLoansSection" - {

    "must be true" - {

      "when schemeHadLoans is Some(true) & 1 record is present, which needs checking" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansIndividualAnswers(
              index1of5000,
              addLoansPartialAnswers(
                index1of5000,
                schemeHadLoansTrue
              )
            )
          )

        checkLoansSection(userAnswers, srn) mustBe true
      }

      "when schemeHadLoans is Some(true) & 2 records are present, 1 of which needs checking" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansUKCompanyAnswers(
              index1of5000,
              addLoansPartialAnswers(
                index1of5000,
                addLoansBaseAnswers(
                  index2of5000,
                  addLoansUKPartnershipAnswers(
                    index2of5000,
                    addLoansPrePopAnswers(
                      index2of5000,
                      schemeHadLoansTrue
                    )
                  )
                )
              )
            )
          )

        checkLoansSection(userAnswers, srn) mustBe true
      }

      "when schemeHadLoans is None & 1 record is present, which needs checking" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansOtherAnswers(
              index1of5000,
              addLoansPartialAnswers(
                index1of5000,
                defaultUserAnswers
              )
            )
          )

        checkLoansSection(userAnswers, srn) mustBe true
      }

      "when schemeHadLoans is None & 2 records are present, 1 of which needs checking" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansIndividualAnswers(
              index1of5000,
              addLoansPartialAnswers(
                index1of5000,
                addLoansBaseAnswers(
                  index2of5000,
                  addLoansUKCompanyAnswers(
                    index2of5000,
                    addLoansPrePopAnswers(
                      index2of5000,
                      defaultUserAnswers
                    )
                  )
                )
              )
            )
          )

        checkLoansSection(userAnswers, srn) mustBe true
      }
    }

    "must be false" - {

      "when schemeHadLoans is Some(false)" in {
        val userAnswers = schemeHadLoansFalse

        checkLoansSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & no records are present" in {
        val userAnswers = schemeHadLoansTrue

        checkLoansSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is Some(true) & 1 record is present which doesn't need checking" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansUKPartnershipAnswers(
              index1of5000,
              addLoansPrePopAnswers(
                index1of5000,
                schemeHadLoansTrue
              )
            )
          )

        checkLoansSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is None & no records are present" in {
        val userAnswers = defaultUserAnswers

        checkLoansSection(userAnswers, srn) mustBe false
      }

      "when schemeHadLoans is None & 1 record is present which doesn't need checking" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansOtherAnswers(
              index1of5000,
              addLoansPrePopAnswers(
                index1of5000,
                defaultUserAnswers
              )
            )
          )

        checkLoansSection(userAnswers, srn) mustBe false
      }
    }
  }

  "checkLoansRecord" - {

    "must be true" - {

      "when all pre-pop-cleared answers are missing & all other answers are present (Individual)" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansIndividualAnswers(
              index1of5000,
              addLoansPartialAnswers(
                index1of5000,
                schemeHadLoansTrue
              )
            )
          )

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe true
      }

      "when all pre-pop-cleared answers are missing & all other answers are present (UKCompany)" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansUKCompanyAnswers(
              index1of5000,
              addLoansPartialAnswers(
                index1of5000,
                schemeHadLoansTrue
              )
            )
          )

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe true
      }

      "when all pre-pop-cleared answers are missing & all other answers are present (UKPartnership)" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansUKPartnershipAnswers(
              index1of5000,
              addLoansPartialAnswers(
                index1of5000,
                schemeHadLoansTrue
              )
            )
          )

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe true
      }

      "when all pre-pop-cleared answers are missing & all other answers are present (Other)" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansOtherAnswers(
              index1of5000,
              addLoansPartialAnswers(
                index1of5000,
                schemeHadLoansTrue
              )
            )
          )

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe true
      }

      "when some pre-pop-cleared answers are present & some are missing" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansIndividualAnswers(
              index1of5000,
              addLoansPrePopAnswers(
                index1of5000,
                schemeHadLoansTrue
              )
            )
          ).unsafeRemove(ArrearsPrevYears(srn, index1of5000))
            .unsafeRemove(OutstandingArrearsOnLoanPage(srn, index1of5000))

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe true
      }
    }

    "must be false" - {

      "when all answers are missing" in {
        val userAnswers = defaultUserAnswers

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when 1 other answer is missing" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansUKCompanyAnswers(
              index1of5000,
              addLoansPartialAnswers(
                index1of5000,
                schemeHadLoansTrue
              )
            )
          ).unsafeRemove(IdentityTypePage(srn, index1of5000, LoanRecipient))

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when all pre-pop-cleared answers are present" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansUKPartnershipAnswers(
              index1of5000,
              addLoansPrePopAnswers(
                index1of5000,
                schemeHadLoansTrue
              )
            )
          )

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when Individual answers are missing" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansPartialAnswers(
              index1of5000,
              schemeHadLoansTrue
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, LoanRecipient), Individual)

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when UKCompany answers are missing" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansPartialAnswers(
              index1of5000,
              schemeHadLoansTrue
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, LoanRecipient), UKCompany)

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when UKPartnership answers are missing" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansPartialAnswers(
              index1of5000,
              schemeHadLoansTrue
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, LoanRecipient), UKPartnership)

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when Other answers are missing" in {
        val userAnswers =
          addLoansBaseAnswers(
            index1of5000,
            addLoansPartialAnswers(
              index1of5000,
              schemeHadLoansTrue
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, LoanRecipient), Other)

        checkLoansRecord(userAnswers, srn, index1of5000) mustBe false
      }
    }
  }
}
