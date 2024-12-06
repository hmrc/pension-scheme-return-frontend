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

import pages.nonsipp.shares._
import models.IdentityType._
import utils.nonsipp.check.SharesCheckStatusUtils.{checkSharesRecord, checkSharesSection}
import org.scalatest.OptionValues
import models._
import pages.nonsipp.common._
import models.IdentitySubject._
import org.scalatest.matchers.must.Matchers
import models.ConditionalYesNo._
import config.RefinedTypes.Max5000
import controllers.ControllerBaseSpec

class SharesCheckStatusUtilsSpec extends ControllerBaseSpec with Matchers with OptionValues {

  private val didSchemeHoldAnySharesTrue = defaultUserAnswers.unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
  private val didSchemeHoldAnySharesFalse = defaultUserAnswers.unsafeSet(DidSchemeHoldAnySharesPage(srn), false)

  private def addSharesBaseAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(CompanyNameRelatedSharesPage(srn, index), name)
      .unsafeSet(SharesCompanyCrnPage(srn, index), conditionalYesNoCrn)
      .unsafeSet(ClassOfSharesPage(srn, index), classOfShares)
      .unsafeSet(HowManySharesPage(srn, index), totalShares)
      .unsafeSet(CostOfSharesPage(srn, index), money)
      .unsafeSet(SharesIndependentValuationPage(srn, index), true)

  // Branching on TypeOfSharesHeldPage
  private def addSharesSponsoringEmployerAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer)

  private def addSharesUnquotedAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.Unquoted)
      .unsafeSet(SharesFromConnectedPartyPage(srn, index), true)

  private def addSharesConnectedPartyAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.ConnectedParty)

  // Branching on WhyDoesSchemeHoldSharesPage
  private def addSharesAcquisitionAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)

  private def addSharesContributionAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Contribution)
      .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)

  private def addSharesTransferAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Transfer)

  // Branching on IdentityTypePage
  private def addSharesIndividualAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, SharesSeller), Individual)
      .unsafeSet(IndividualNameOfSharesSellerPage(srn, index), name)
      .unsafeSet(SharesIndividualSellerNINumberPage(srn, index), conditionalYesNoNino)

  private def addSharesUKCompanyAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, SharesSeller), UKCompany)
      .unsafeSet(CompanyNameOfSharesSellerPage(srn, index), name)
      .unsafeSet(CompanyRecipientCrnPage(srn, index, SharesSeller), conditionalYesNoCrn)

  private def addSharesUKPartnershipAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, SharesSeller), UKPartnership)
      .unsafeSet(PartnershipShareSellerNamePage(srn, index), name)
      .unsafeSet(PartnershipRecipientUtrPage(srn, index, SharesSeller), conditionalYesNoUtr)

  private def addSharesOtherAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(IdentityTypePage(srn, index, SharesSeller), Other)
      .unsafeSet(OtherRecipientDetailsPage(srn, index, SharesSeller), otherRecipientDetails)

  // Special case: TotalAssetValuePage is only set when Sponsoring Employer & Acquisition are selected
  private def addSharesSponsoringEmployerAndAcquisitionAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(TypeOfSharesHeldPage(srn, index), TypeOfShares.SponsoringEmployer)
      .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, index), SchemeHoldShare.Acquisition)
      .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, index), localDate)
      .unsafeSet(TotalAssetValuePage(srn, index), money)

  private def addSharesPrePopAnswers(index: Max5000, userAnswers: UserAnswers): UserAnswers =
    userAnswers
      .unsafeSet(SharesTotalIncomePage(srn, index), money)

  "checkSharesSection" - {

    "must be true" - {

      "when didSchemeHoldAnyShares is Some(true) & 1 record is present, which needs checking" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesSponsoringEmployerAnswers(
              index1of5000,
              addSharesContributionAnswers(
                index1of5000,
                didSchemeHoldAnySharesTrue
              )
            )
          )

        checkSharesSection(userAnswers, srn) mustBe true
      }

      "when didSchemeHoldAnyShares is Some(true) & 2 records are present, 1 of which needs checking" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesUnquotedAnswers(
              index1of5000,
              addSharesTransferAnswers(
                index1of5000,
                addSharesBaseAnswers(
                  index2of5000,
                  addSharesConnectedPartyAnswers(
                    index2of5000,
                    addSharesContributionAnswers(
                      index2of5000,
                      addSharesPrePopAnswers(
                        index2of5000,
                        didSchemeHoldAnySharesTrue
                      )
                    )
                  )
                )
              )
            )
          )

        checkSharesSection(userAnswers, srn) mustBe true
      }

      "when didSchemeHoldAnyShares is None & 1 record is present, which needs checking" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesSponsoringEmployerAnswers(
              index1of5000,
              addSharesTransferAnswers(
                index1of5000,
                defaultUserAnswers
              )
            )
          )

        checkSharesSection(userAnswers, srn) mustBe true
      }

      "when didSchemeHoldAnyShares is None & 2 records are present, 1 of which needs checking" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesUnquotedAnswers(
              index1of5000,
              addSharesContributionAnswers(
                index1of5000,
                addSharesBaseAnswers(
                  index2of5000,
                  addSharesConnectedPartyAnswers(
                    index2of5000,
                    addSharesTransferAnswers(
                      index2of5000,
                      addSharesPrePopAnswers(
                        index2of5000,
                        defaultUserAnswers
                      )
                    )
                  )
                )
              )
            )
          )

        checkSharesSection(userAnswers, srn) mustBe true
      }
    }

    "must be false" - {

      "when didSchemeHoldAnyShares is Some(false)" in {
        val userAnswers = didSchemeHoldAnySharesFalse

        checkSharesSection(userAnswers, srn) mustBe false
      }

      "when didSchemeHoldAnyShares is Some(true) & no records are present" in {
        val userAnswers = didSchemeHoldAnySharesTrue

        checkSharesSection(userAnswers, srn) mustBe false
      }

      "when didSchemeHoldAnyShares is Some(true) & 1 record is present, which doesn't need checking" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesUnquotedAnswers(
              index1of5000,
              addSharesContributionAnswers(
                index1of5000,
                didSchemeHoldAnySharesTrue
              )
            )
          ).unsafeRemove(SharesFromConnectedPartyPage(srn, index1of5000))

        checkSharesSection(userAnswers, srn) mustBe false
      }

      "when didSchemeHoldAnyShares is None & no records are present" in {
        val userAnswers = defaultUserAnswers

        checkSharesSection(userAnswers, srn) mustBe false
      }

      "when didSchemeHoldAnyShares is None & 1 record is present, which doesn't need checking" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesUnquotedAnswers(
              index1of5000,
              addSharesTransferAnswers(
                index1of5000,
                defaultUserAnswers
              )
            )
          ).unsafeRemove(SharesFromConnectedPartyPage(srn, index1of5000))

        checkSharesSection(userAnswers, srn) mustBe false
      }
    }
  }

  "checkSharesRecord" - {

    "must be true" - {

      "when pre-pop-cleared answer is missing and all other answers are present" - {

        "(Acquisition & Sponsoring Employer & Individual)" in {
          val userAnswers =
            addSharesBaseAnswers(
              index1of5000,
              addSharesSponsoringEmployerAndAcquisitionAnswers(
                index1of5000,
                addSharesIndividualAnswers(
                  index1of5000,
                  didSchemeHoldAnySharesTrue
                )
              )
            )

          checkSharesRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Acquisition & Unquoted & UKCompany)" in {
          val userAnswers =
            addSharesBaseAnswers(
              index1of5000,
              addSharesUnquotedAnswers(
                index1of5000,
                addSharesAcquisitionAnswers(
                  index1of5000,
                  addSharesUKCompanyAnswers(
                    index1of5000,
                    didSchemeHoldAnySharesTrue
                  )
                )
              )
            )

          checkSharesRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Acquisition & Connected Party & UKPartnership)" in {
          val userAnswers =
            addSharesBaseAnswers(
              index1of5000,
              addSharesConnectedPartyAnswers(
                index1of5000,
                addSharesAcquisitionAnswers(
                  index1of5000,
                  addSharesUKPartnershipAnswers(
                    index1of5000,
                    didSchemeHoldAnySharesTrue
                  )
                )
              )
            )

          checkSharesRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Acquisition & Sponsoring Employer & Other)" in {
          val userAnswers =
            addSharesBaseAnswers(
              index1of5000,
              addSharesSponsoringEmployerAndAcquisitionAnswers(
                index1of5000,
                addSharesOtherAnswers(
                  index1of5000,
                  didSchemeHoldAnySharesTrue
                )
              )
            )

          checkSharesRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Contribution & Unquoted)" in {
          val userAnswers =
            addSharesBaseAnswers(
              index1of5000,
              addSharesUnquotedAnswers(
                index1of5000,
                addSharesContributionAnswers(
                  index1of5000,
                  didSchemeHoldAnySharesTrue
                )
              )
            )

          checkSharesRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Contribution & Sponsoring Employer)" in {
          val userAnswers =
            addSharesBaseAnswers(
              index1of5000,
              addSharesSponsoringEmployerAnswers(
                index1of5000,
                addSharesContributionAnswers(
                  index1of5000,
                  didSchemeHoldAnySharesTrue
                )
              )
            )

          checkSharesRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Transfer & Unquoted)" in {
          val userAnswers =
            addSharesBaseAnswers(
              index1of5000,
              addSharesUnquotedAnswers(
                index1of5000,
                addSharesTransferAnswers(
                  index1of5000,
                  didSchemeHoldAnySharesTrue
                )
              )
            )

          checkSharesRecord(userAnswers, srn, index1of5000) mustBe true
        }

        "(Transfer & Connected Party)" in {
          val userAnswers =
            addSharesBaseAnswers(
              index1of5000,
              addSharesConnectedPartyAnswers(
                index1of5000,
                addSharesTransferAnswers(
                  index1of5000,
                  didSchemeHoldAnySharesTrue
                )
              )
            )

          checkSharesRecord(userAnswers, srn, index1of5000) mustBe true
        }
      }
    }

    "must be false" - {

      "when all answers are missing" in {
        val userAnswers = defaultUserAnswers

        checkSharesRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when 1 other answer is missing" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesSponsoringEmployerAndAcquisitionAnswers(
              index1of5000,
              addSharesIndividualAnswers(
                index1of5000,
                didSchemeHoldAnySharesTrue
              )
            )
          ).unsafeRemove(IdentityTypePage(srn, index1of5000, SharesSeller))

        checkSharesRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when all pre-pop-cleared answers are present" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesSponsoringEmployerAndAcquisitionAnswers(
              index1of5000,
              addSharesIndividualAnswers(
                index1of5000,
                addSharesPrePopAnswers(
                  index1of5000,
                  didSchemeHoldAnySharesTrue
                )
              )
            )
          )

        checkSharesRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when Individual answers are missing" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesSponsoringEmployerAndAcquisitionAnswers(
              index1of5000,
              didSchemeHoldAnySharesTrue
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, SharesSeller), Individual)

        checkSharesRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when UKCompany answers are missing" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesAcquisitionAnswers(
              index1of5000,
              addSharesUnquotedAnswers(
                index1of5000,
                didSchemeHoldAnySharesTrue
              )
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, SharesSeller), UKCompany)

        checkSharesRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when UKPartnership answers are missing" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesAcquisitionAnswers(
              index1of5000,
              addSharesConnectedPartyAnswers(
                index1of5000,
                didSchemeHoldAnySharesTrue
              )
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, SharesSeller), UKPartnership)

        checkSharesRecord(userAnswers, srn, index1of5000) mustBe false
      }

      "when Other answer is missing" in {
        val userAnswers =
          addSharesBaseAnswers(
            index1of5000,
            addSharesSponsoringEmployerAndAcquisitionAnswers(
              index1of5000,
              didSchemeHoldAnySharesTrue
            )
          ).unsafeSet(IdentityTypePage(srn, index1of5000, SharesSeller), Other)

        checkSharesRecord(userAnswers, srn, index1of5000) mustBe false
      }
    }
  }
}
