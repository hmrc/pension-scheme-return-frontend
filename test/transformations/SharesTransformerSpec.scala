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

package transformations

import play.api.test.FakeRequest
import org.scalatest.matchers.must.Matchers
import models.IdentityType.UKCompany
import controllers.TestValues
import models.SchemeHoldShare.{Acquisition, Contribution, Transfer}
import models.TypeOfShares.{ConnectedParty, SponsoringEmployer, Unquoted}
import utils.IntUtils.given
import pages.nonsipp.sharesdisposal._
import utils.UserAnswersUtils.UserAnswersOps
import generators.ModelGenerators.allowedAccessRequestGen
import models._
import pages.nonsipp.common.{CompanyRecipientCrnPage, IdentityTypePage, PartnershipRecipientUtrPage}
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import models.requests.{AllowedAccessRequest, DataRequest}
import org.scalatest.freespec.AnyFreeSpec
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import pages.nonsipp.shares._
import play.api.mvc.AnyContentAsEmpty
import models.requests.psr._
import config.Constants.PREPOPULATION_FLAG
import org.scalatest.OptionValues
import models.HowSharesDisposed._

class SharesTransformerSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  val allowedAccessRequest: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest()
  ).sample.value
  val allowedAccessRequestPrePopulation: AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(
    FakeRequest()
      .withSession((PREPOPULATION_FLAG, "true"))
  ).sample.value

  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new SharesTransformer

  def sharesBlankTotalIncome(prePopulated: Option[Boolean], recordVersion: Option[String]): Shares =
    Shares(
      recordVersion = recordVersion,
      optDidSchemeHoldAnyShares = None,
      optShareTransactions = Some(
        List(
          ShareTransaction(
            prePopulated = prePopulated,
            typeOfSharesHeld = SponsoringEmployer,
            shareIdentification = ShareIdentification(
              nameOfSharesCompany = "nameOfSharesCompany",
              optCrnNumber = None,
              optReasonNoCRN = Some("optReasonNoCRN"),
              classOfShares = "classOfShares"
            ),
            heldSharesTransaction = HeldSharesTransaction(
              schemeHoldShare = SchemeHoldShare.Acquisition,
              optDateOfAcqOrContrib = Some(localDate),
              totalShares = totalShares,
              optAcquiredFromName = Some("optAcquiredFromName"),
              optPropertyAcquiredFrom =
                Some(PropertyAcquiredFrom(IdentityType.Individual, None, Some(noninoReason), None)),
              optConnectedPartyStatus = None,
              costOfShares = money.value,
              supportedByIndepValuation = true,
              optTotalAssetValue = Some(money.value),
              optTotalDividendsOrReceipts = None
            ),
            optDisposedSharesTransaction = None
          ),
          ShareTransaction(
            prePopulated = None,
            typeOfSharesHeld = TypeOfShares.Unquoted,
            shareIdentification = ShareIdentification(
              nameOfSharesCompany = "nameOfSharesCompany",
              optCrnNumber = None,
              optReasonNoCRN = Some("optReasonNoCRN"),
              classOfShares = "classOfShares"
            ),
            heldSharesTransaction = HeldSharesTransaction(
              schemeHoldShare = SchemeHoldShare.Contribution,
              optDateOfAcqOrContrib = Some(localDate),
              totalShares = totalShares,
              optAcquiredFromName = None,
              optPropertyAcquiredFrom = None,
              optConnectedPartyStatus = Some(true),
              costOfShares = money.value,
              supportedByIndepValuation = true,
              optTotalAssetValue = None,
              optTotalDividendsOrReceipts = None
            ),
            optDisposedSharesTransaction = None
          ),
          ShareTransaction(
            prePopulated = None,
            typeOfSharesHeld = TypeOfShares.ConnectedParty,
            shareIdentification = ShareIdentification(
              nameOfSharesCompany = "nameOfSharesCompany",
              optCrnNumber = None,
              optReasonNoCRN = Some("optReasonNoCRN"),
              classOfShares = "classOfShares"
            ),
            heldSharesTransaction = HeldSharesTransaction(
              schemeHoldShare = SchemeHoldShare.Transfer,
              optDateOfAcqOrContrib = None,
              totalShares = totalShares,
              optAcquiredFromName = None,
              optPropertyAcquiredFrom = None,
              optConnectedPartyStatus = None,
              costOfShares = money.value,
              supportedByIndepValuation = false,
              optTotalAssetValue = None,
              optTotalDividendsOrReceipts = None
            ),
            optDisposedSharesTransaction = None
          )
        )
      ),
      optTotalValueQuotedShares = None
    )

  "SharesTransformer - To Etmp" - {

    "should return None when userAnswer is empty" in {
      val result = transformer.transformToEtmp(srn = srn, initialUA = defaultUserAnswers)
      result mustBe None
    }

    "should omit Record Version when there is a change in userAnswers" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(DidSchemeHoldAnySharesPage(srn), false)
        .unsafeSet(SharesRecordVersionPage(srn), "001")

      val initialUserAnswer = emptyUserAnswers
        .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
        .unsafeSet(SharesRecordVersionPage(srn), "001")

      val result =
        transformer.transformToEtmp(srn = srn, initialUserAnswer)(using DataRequest(allowedAccessRequest, userAnswers))
      result mustBe Some(Shares(None, Some(false), None, None))
    }

    "should omit share transaction details when DidSchemeHoldAnySharesPage was 'No'" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(DidSchemeHoldAnySharesPage(srn), false)
        .unsafeSet(TypeOfSharesHeldPage(srn, 1), SponsoringEmployer)

      val initialUserAnswer = emptyUserAnswers
        .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)

      val result =
        transformer.transformToEtmp(srn = srn, initialUserAnswer)(using DataRequest(allowedAccessRequest, userAnswers))
      result mustBe Some(Shares(None, Some(false), None, None))
    }

    "should omit share transaction details when DidSchemeHoldAnySharesPage was 'Yes' and there are no complete shares" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
        .unsafeSet(TypeOfSharesHeldPage(srn, 1), SponsoringEmployer)

      val initialUserAnswer = emptyUserAnswers
        .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)

      val result =
        transformer.transformToEtmp(srn = srn, initialUserAnswer)(using DataRequest(allowedAccessRequest, userAnswers))
      result mustBe Some(Shares(None, Some(true), None, None))
    }

    "should return recordVersion when there is no change among UAs" - {

      "should return Quoted Share when quoted share is in userAnswers" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(TotalValueQuotedSharesPage(srn), money)
          .unsafeSet(SharesRecordVersionPage(srn), "001")

        val result =
          transformer.transformToEtmp(srn = srn, userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))
        result mustBe Some(
          Shares(
            recordVersion = Some("001"),
            optDidSchemeHoldAnyShares = Some(true),
            optShareTransactions = None,
            optTotalValueQuotedShares = Some(money.value)
          )
        )
      }

      "should return Quoted Share and Share Transactions when all of them are in userAnswers" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
          .unsafeSet(TotalValueQuotedSharesPage(srn), money)
          .unsafeSet(SharesRecordVersionPage(srn), "001")
          .unsafeSet(TypeOfSharesHeldPage(srn, 1), Unquoted)
          .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, 1), Contribution)
          .unsafeSet(CompanyNameRelatedSharesPage(srn, 1), "nameOfSharesCompany")
          .unsafeSet(SharesCompanyCrnPage(srn, 1), ConditionalYesNo.no[String, Crn]("CRN-No-Reason"))
          .unsafeSet(ClassOfSharesPage(srn, 1), "classOfShares")
          .unsafeSet(HowManySharesPage(srn, 1), 123)
          .unsafeSet(CostOfSharesPage(srn, 1), money)
          .unsafeSet(SharesIndependentValuationPage(srn, 1), true)
          .unsafeSet(SharesTotalIncomePage(srn, 1), money)
          .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, 1), localDate)
          .unsafeSet(SharesFromConnectedPartyPage(srn, 1), false)
          .unsafeSet(SharesDisposalPage(srn), true)
          .unsafeSet(HowWereSharesDisposedPage(srn, 1, 1), Redeemed)
          .unsafeSet(HowManyDisposalSharesPage(srn, 1, 1), 123)
          .unsafeSet(WhenWereSharesRedeemedPage(srn, 1, 1), localDate)
          .unsafeSet(HowManySharesRedeemedPage(srn, 1, 1), 123)
          .unsafeSet(TotalConsiderationSharesRedeemedPage(srn, 1, 1), money)
          .unsafeSet(SharesProgress(srn, 1), SectionJourneyStatus.Completed)
          .unsafeSet(SharesDisposalProgress(srn, 1, 1), SectionJourneyStatus.Completed)

        val result =
          transformer.transformToEtmp(srn = srn, userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))
        result mustBe Some(
          Shares(
            recordVersion = Some("001"),
            Some(true),
            optShareTransactions = Some(
              List(
                ShareTransaction(
                  prePopulated = None,
                  typeOfSharesHeld = Unquoted,
                  shareIdentification = ShareIdentification(
                    nameOfSharesCompany = "nameOfSharesCompany",
                    optCrnNumber = None,
                    optReasonNoCRN = Some("CRN-No-Reason"),
                    classOfShares = "classOfShares"
                  ),
                  heldSharesTransaction = HeldSharesTransaction(
                    schemeHoldShare = Contribution,
                    optDateOfAcqOrContrib = Some(localDate),
                    totalShares = 123,
                    optAcquiredFromName = None,
                    optPropertyAcquiredFrom = None,
                    optConnectedPartyStatus = Some(false),
                    costOfShares = money.value,
                    supportedByIndepValuation = true,
                    optTotalAssetValue = None,
                    optTotalDividendsOrReceipts = Some(money.value)
                  ),
                  optDisposedSharesTransaction = Some(
                    Seq(
                      DisposedSharesTransaction(
                        methodOfDisposal = Redeemed.name,
                        optOtherMethod = None,
                        optSalesQuestions = None,
                        optRedemptionQuestions = Some(
                          RedemptionQuestions(
                            dateOfRedemption = localDate,
                            noOfSharesRedeemed = 123,
                            amountReceived = money.value
                          )
                        ),
                        totalSharesNowHeld = 123
                      )
                    )
                  )
                )
              )
            ),
            optTotalValueQuotedShares = Some(money.value)
          )
        )
      }
      "should return Share Transactions in pre-population" in {
        val userAnswers = emptyUserAnswers
          .unsafeSet(SharesRecordVersionPage(srn), "001")
          .unsafeSet(TypeOfSharesHeldPage(srn, 1), Unquoted)
          .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, 1), Contribution)
          .unsafeSet(CompanyNameRelatedSharesPage(srn, 1), "nameOfSharesCompany")
          .unsafeSet(SharesCompanyCrnPage(srn, 1), ConditionalYesNo.no[String, Crn]("CRN-No-Reason"))
          .unsafeSet(ClassOfSharesPage(srn, 1), "classOfShares")
          .unsafeSet(HowManySharesPage(srn, 1), 123)
          .unsafeSet(CostOfSharesPage(srn, 1), money)
          .unsafeSet(SharesIndependentValuationPage(srn, 1), true)
          .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, 1), localDate)
          .unsafeSet(SharesFromConnectedPartyPage(srn, 1), false)
          .unsafeSet(SharePrePopulated(srn, 1), true)
          .unsafeSet(SharesProgress(srn, 1), SectionJourneyStatus.Completed)
          .unsafeSet(TypeOfSharesHeldPage(srn, 2), Unquoted)
          .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, 2), Contribution)
          .unsafeSet(CompanyNameRelatedSharesPage(srn, 2), "nameOfSharesCompany2")
          .unsafeSet(SharesCompanyCrnPage(srn, 2), ConditionalYesNo.no[String, Crn]("CRN-No-Reason"))
          .unsafeSet(ClassOfSharesPage(srn, 2), "classOfShares")
          .unsafeSet(HowManySharesPage(srn, 2), 123)
          .unsafeSet(CostOfSharesPage(srn, 2), money)
          .unsafeSet(SharesIndependentValuationPage(srn, 2), true)
          .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, 2), localDate)
          .unsafeSet(SharesFromConnectedPartyPage(srn, 2), false)
          .unsafeSet(SharePrePopulated(srn, 2), false)
          .unsafeSet(SharesProgress(srn, 2), SectionJourneyStatus.Completed)

        val result = transformer.transformToEtmp(srn = srn, userAnswers)(using
          DataRequest(allowedAccessRequestPrePopulation, userAnswers)
        )
        result mustBe Some(
          Shares(
            recordVersion = Some("001"),
            optDidSchemeHoldAnyShares = None,
            optShareTransactions = Some(
              List(
                ShareTransaction(
                  prePopulated = Some(true),
                  typeOfSharesHeld = Unquoted,
                  shareIdentification = ShareIdentification(
                    nameOfSharesCompany = "nameOfSharesCompany",
                    optCrnNumber = None,
                    optReasonNoCRN = Some("CRN-No-Reason"),
                    classOfShares = "classOfShares"
                  ),
                  heldSharesTransaction = HeldSharesTransaction(
                    schemeHoldShare = Contribution,
                    optDateOfAcqOrContrib = Some(localDate),
                    totalShares = 123,
                    optAcquiredFromName = None,
                    optPropertyAcquiredFrom = None,
                    optConnectedPartyStatus = Some(false),
                    costOfShares = money.value,
                    supportedByIndepValuation = true,
                    optTotalAssetValue = None,
                    optTotalDividendsOrReceipts = None
                  ),
                  optDisposedSharesTransaction = None
                ),
                ShareTransaction(
                  prePopulated = Some(false),
                  typeOfSharesHeld = Unquoted,
                  shareIdentification = ShareIdentification(
                    nameOfSharesCompany = "nameOfSharesCompany2",
                    optCrnNumber = None,
                    optReasonNoCRN = Some("CRN-No-Reason"),
                    classOfShares = "classOfShares"
                  ),
                  heldSharesTransaction = HeldSharesTransaction(
                    schemeHoldShare = Contribution,
                    optDateOfAcqOrContrib = Some(localDate),
                    totalShares = 123,
                    optAcquiredFromName = None,
                    optPropertyAcquiredFrom = None,
                    optConnectedPartyStatus = Some(false),
                    costOfShares = money.value,
                    supportedByIndepValuation = true,
                    optTotalAssetValue = None,
                    optTotalDividendsOrReceipts = None
                  ),
                  optDisposedSharesTransaction = None
                )
              )
            ),
            optTotalValueQuotedShares = None
          )
        )
      }
    }

    "should not include incomplete record" in {

      val incompleteUserAnswers = emptyUserAnswers
        .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
        .unsafeSet(TotalValueQuotedSharesPage(srn), money)
        .unsafeSet(SharesRecordVersionPage(srn), "001")
        .unsafeSet(TypeOfSharesHeldPage(srn, 1), Unquoted)
        .unsafeSet(
          SharesProgress(srn, 1),
          SectionJourneyStatus.InProgress(WhyDoesSchemeHoldSharesPage(srn, 1))
        )

      val request = DataRequest(allowedAccessRequest, incompleteUserAnswers)

      val result = transformer.transformToEtmp(srn, incompleteUserAnswers)(using request)

      result mustBe Some(Shares(Some("001"), Some(true), None, Some(123456.0)))
    }

    "should transform acquisition from a UK Partnership seller with UTR" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
        .unsafeSet(TypeOfSharesHeldPage(srn, 1), SponsoringEmployer)
        .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, 1), Acquisition)
        .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, 1), localDate)
        .unsafeSet(CompanyNameRelatedSharesPage(srn, 1), "nameOfSharesCompany")
        .unsafeSet(SharesCompanyCrnPage(srn, 1), ConditionalYesNo.no[String, Crn]("CRN-No-Reason"))
        .unsafeSet(ClassOfSharesPage(srn, 1), "classOfShares")
        .unsafeSet(HowManySharesPage(srn, 1), 123)
        .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.SharesSeller), IdentityType.UKPartnership)
        .unsafeSet(PartnershipShareSellerNamePage(srn, 1), "partnershipName")
        .unsafeSet(PartnershipRecipientUtrPage(srn, 1, IdentitySubject.SharesSeller), ConditionalYesNo.yes(utr))
        .unsafeSet(CostOfSharesPage(srn, 1), money)
        .unsafeSet(SharesIndependentValuationPage(srn, 1), true)
        .unsafeSet(TotalAssetValuePage(srn, 1), money)
        .unsafeSet(SharesTotalIncomePage(srn, 1), money)
        .unsafeSet(SharesProgress(srn, 1), SectionJourneyStatus.Completed)

      val result = transformer.transformToEtmp(srn, userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))

      val heldTransaction = result.value.optShareTransactions.value.head.heldSharesTransaction
      heldTransaction.optAcquiredFromName mustBe Some("partnershipName")
      heldTransaction.optPropertyAcquiredFrom mustBe Some(
        PropertyAcquiredFrom(
          identityType = IdentityType.UKPartnership,
          idNumber = Some(utr.value),
          reasonNoIdNumber = None,
          otherDescription = None
        )
      )
    }

    "should transform a sale to an Other buyer" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
        .unsafeSet(TypeOfSharesHeldPage(srn, 1), Unquoted)
        .unsafeSet(WhyDoesSchemeHoldSharesPage(srn, 1), Acquisition)
        .unsafeSet(WhenDidSchemeAcquireSharesPage(srn, 1), localDate)
        .unsafeSet(CompanyNameRelatedSharesPage(srn, 1), "nameOfSharesCompany")
        .unsafeSet(SharesCompanyCrnPage(srn, 1), ConditionalYesNo.no[String, Crn]("CRN-No-Reason"))
        .unsafeSet(ClassOfSharesPage(srn, 1), "classOfShares")
        .unsafeSet(HowManySharesPage(srn, 1), 123)
        .unsafeSet(IdentityTypePage(srn, 1, IdentitySubject.SharesSeller), IdentityType.Individual)
        .unsafeSet(IndividualNameOfSharesSellerPage(srn, 1), "sellerName")
        .unsafeSet(SharesIndividualSellerNINumberPage(srn, 1), ConditionalYesNo.no(noninoReason))
        .unsafeSet(CostOfSharesPage(srn, 1), money)
        .unsafeSet(SharesFromConnectedPartyPage(srn, 1), false)
        .unsafeSet(SharesIndependentValuationPage(srn, 1), true)
        .unsafeSet(SharesTotalIncomePage(srn, 1), money)
        .unsafeSet(SharesProgress(srn, 1), SectionJourneyStatus.Completed)
        .unsafeSet(SharesDisposalPage(srn), true)
        .unsafeSet(HowWereSharesDisposedPage(srn, 1, 1), Sold)
        .unsafeSet(WhenWereSharesSoldPage(srn, 1, 1), localDate)
        .unsafeSet(HowManySharesSoldPage(srn, 1, 1), 100)
        .unsafeSet(TotalConsiderationSharesSoldPage(srn, 1, 1), money)
        .unsafeSet(WhoWereTheSharesSoldToPage(srn, 1, 1), IdentityType.Other)
        .unsafeSet(OtherBuyerDetailsPage(srn, 1, 1), RecipientDetails("otherBuyer", "description"))
        .unsafeSet(IsBuyerConnectedPartyPage(srn, 1, 1), true)
        .unsafeSet(IndependentValuationPage(srn, 1, 1), false)
        .unsafeSet(HowManyDisposalSharesPage(srn, 1, 1), 23)
        .unsafeSet(SharesDisposalProgress(srn, 1, 1), SectionJourneyStatus.Completed)

      val result = transformer.transformToEtmp(srn, userAnswers)(using DataRequest(allowedAccessRequest, userAnswers))
      val salesQuestions =
        result.value.optShareTransactions.value.head.optDisposedSharesTransaction.value.head.optSalesQuestions.value

      salesQuestions.nameOfPurchaser mustBe "otherBuyer"
      salesQuestions.purchaserType mustBe PropertyAcquiredFrom(
        identityType = IdentityType.Other,
        idNumber = None,
        reasonNoIdNumber = None,
        otherDescription = Some("description")
      )
    }
  }

  "SharesTransformer - From Etmp" - {
    "when only recordVersion available" in {
      val userAnswers = emptyUserAnswers
      val result = transformer.transformFromEtmp(
        userAnswers,
        srn,
        Shares(
          recordVersion = Some("001"),
          optDidSchemeHoldAnyShares = None,
          optShareTransactions = None,
          optTotalValueQuotedShares = None
        )
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => userAnswers.get(SharesRecordVersionPage(srn)) mustBe Some("001")
      )
    }

    "when shareTransactionList not empty" in {

      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        Shares(
          recordVersion = Some("001"),
          optDidSchemeHoldAnyShares = Some(true),
          optShareTransactions = Some(
            List(
              ShareTransaction(
                prePopulated = None,
                typeOfSharesHeld = SponsoringEmployer,
                shareIdentification = ShareIdentification(
                  nameOfSharesCompany = "nameOfSharesCompany",
                  optCrnNumber = None,
                  optReasonNoCRN = Some("optReasonNoCRN"),
                  classOfShares = "classOfShares"
                ),
                heldSharesTransaction = HeldSharesTransaction(
                  schemeHoldShare = SchemeHoldShare.Acquisition,
                  optDateOfAcqOrContrib = Some(localDate),
                  totalShares = totalShares,
                  optAcquiredFromName = Some("optAcquiredFromName"),
                  optPropertyAcquiredFrom =
                    Some(PropertyAcquiredFrom(IdentityType.Individual, None, Some(noninoReason), None)),
                  optConnectedPartyStatus = None,
                  costOfShares = money.value,
                  supportedByIndepValuation = true,
                  optTotalAssetValue = Some(money.value),
                  optTotalDividendsOrReceipts = Some(money.value)
                ),
                optDisposedSharesTransaction = Some(
                  List(
                    DisposedSharesTransaction(
                      methodOfDisposal = "Sold",
                      optOtherMethod = None,
                      optSalesQuestions = Some(
                        SalesQuestions(
                          dateOfSale = localDate,
                          noOfSharesSold = 123,
                          amountReceived = money.value,
                          nameOfPurchaser = "nameOfPurchaser",
                          purchaserType = PropertyAcquiredFrom(
                            identityType = IdentityType.UKCompany,
                            idNumber = None,
                            reasonNoIdNumber = Some("UKCompanyNoReason"),
                            otherDescription = None
                          ),
                          connectedPartyStatus = true,
                          supportedByIndepValuation = false
                        )
                      ),
                      optRedemptionQuestions = None,
                      totalSharesNowHeld = totalShares
                    ),
                    DisposedSharesTransaction(
                      methodOfDisposal = "Redeemed",
                      optOtherMethod = None,
                      optSalesQuestions = None,
                      optRedemptionQuestions = Some(
                        RedemptionQuestions(
                          dateOfRedemption = localDate,
                          noOfSharesRedeemed = 456,
                          amountReceived = money.value
                        )
                      ),
                      totalSharesNowHeld = totalShares
                    )
                  )
                )
              ),
              ShareTransaction(
                prePopulated = None,
                typeOfSharesHeld = TypeOfShares.Unquoted,
                shareIdentification = ShareIdentification(
                  nameOfSharesCompany = "nameOfSharesCompany",
                  optCrnNumber = None,
                  optReasonNoCRN = Some("optReasonNoCRN"),
                  classOfShares = "classOfShares"
                ),
                heldSharesTransaction = HeldSharesTransaction(
                  schemeHoldShare = SchemeHoldShare.Contribution,
                  optDateOfAcqOrContrib = Some(localDate),
                  totalShares = totalShares,
                  optAcquiredFromName = None,
                  optPropertyAcquiredFrom = None,
                  optConnectedPartyStatus = Some(true),
                  costOfShares = money.value,
                  supportedByIndepValuation = true,
                  optTotalAssetValue = None,
                  optTotalDividendsOrReceipts = Some(money.value)
                ),
                optDisposedSharesTransaction = Some(
                  List(
                    DisposedSharesTransaction(
                      methodOfDisposal = "Transferred",
                      optOtherMethod = None,
                      optSalesQuestions = None,
                      optRedemptionQuestions = None,
                      totalSharesNowHeld = totalShares
                    ),
                    DisposedSharesTransaction(
                      methodOfDisposal = "Other",
                      optOtherMethod = Some("OtherMethod"),
                      optSalesQuestions = None,
                      optRedemptionQuestions = None,
                      totalSharesNowHeld = totalShares
                    )
                  )
                )
              ),
              ShareTransaction(
                prePopulated = None,
                typeOfSharesHeld = TypeOfShares.ConnectedParty,
                shareIdentification = ShareIdentification(
                  nameOfSharesCompany = "nameOfSharesCompany",
                  optCrnNumber = None,
                  optReasonNoCRN = Some("optReasonNoCRN"),
                  classOfShares = "classOfShares"
                ),
                heldSharesTransaction = HeldSharesTransaction(
                  schemeHoldShare = SchemeHoldShare.Transfer,
                  optDateOfAcqOrContrib = None,
                  totalShares = totalShares,
                  optAcquiredFromName = None,
                  optPropertyAcquiredFrom = None,
                  optConnectedPartyStatus = None,
                  costOfShares = money.value,
                  supportedByIndepValuation = false,
                  optTotalAssetValue = None,
                  optTotalDividendsOrReceipts = Some(money.value)
                ),
                optDisposedSharesTransaction = None
              )
            )
          ),
          optTotalValueQuotedShares = Some(money.value)
        )
      )

      result.fold(
        ex => fail(ex.getMessage + "\n" + ex.getStackTrace.mkString("\n")),
        userAnswers => {
          userAnswers.get(SharesRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(DidSchemeHoldAnySharesPage(srn)) mustBe Some(true)

          // share-index-1
          userAnswers.get(SharesCompleted(srn, 1)) mustBe Some(SectionCompleted)
          userAnswers.get(TypeOfSharesHeldPage(srn, 1)) mustBe Some(SponsoringEmployer)
          userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, 1)) mustBe Some(Acquisition)
          userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, 1)) mustBe Some(localDate)
          userAnswers.get(CompanyNameRelatedSharesPage(srn, 1)) mustBe Some("nameOfSharesCompany")
          userAnswers.get(SharesCompanyCrnPage(srn, 1)) mustBe Some(ConditionalYesNo.no("optReasonNoCRN"))
          userAnswers.get(ClassOfSharesPage(srn, 1)) mustBe Some("classOfShares")
          userAnswers.get(HowManySharesPage(srn, 1)) mustBe Some(totalShares)
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.SharesSeller)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(IndividualNameOfSharesSellerPage(srn, 1)) mustBe Some("optAcquiredFromName")
          userAnswers.get(SharesIndividualSellerNINumberPage(srn, 1)) mustBe Some(
            ConditionalYesNo.no(noninoReason)
          )
          userAnswers.get(SharesFromConnectedPartyPage(srn, 1)) mustBe None
          userAnswers.get(CostOfSharesPage(srn, 1)) mustBe Some(money)
          userAnswers.get(SharesIndependentValuationPage(srn, 1)) mustBe Some(true)
          userAnswers.get(TotalAssetValuePage(srn, 1)) mustBe Some(money)
          userAnswers.get(SharesTotalIncomePage(srn, 1)) mustBe Some(money)
          // share-dispose-index-1-1
          userAnswers.get(SharesDisposalProgress(srn, 1, 1)) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWereSharesDisposedPage(srn, 1, 1)) mustBe Some(Sold)
          userAnswers.get(HowManyDisposalSharesPage(srn, 1, 1)) mustBe Some(totalShares)
          userAnswers.get(WhenWereSharesSoldPage(srn, 1, 1)) mustBe Some(localDate)
          userAnswers.get(HowManySharesSoldPage(srn, 1, 1)) mustBe Some(123)
          userAnswers.get(TotalConsiderationSharesSoldPage(srn, 1, 1)) mustBe Some(money)
          userAnswers.get(WhoWereTheSharesSoldToPage(srn, 1, 1)) mustBe Some(UKCompany)
          userAnswers.get(CompanyBuyerNamePage(srn, 1, 1)) mustBe Some("nameOfPurchaser")
          userAnswers.get(CompanyBuyerCrnPage(srn, 1, 1)) mustBe Some(
            ConditionalYesNo.no("UKCompanyNoReason")
          )
          userAnswers.get(IsBuyerConnectedPartyPage(srn, 1, 1)) mustBe Some(true)
          userAnswers.get(IndependentValuationPage(srn, 1, 1)) mustBe Some(false)
          // share-dispose-index-1-2
          userAnswers.get(SharesDisposalProgress(srn, 1, 2)) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWereSharesDisposedPage(srn, 1, 2)) mustBe Some(Redeemed)
          userAnswers.get(HowManyDisposalSharesPage(srn, 1, 2)) mustBe Some(totalShares)
          userAnswers.get(WhenWereSharesRedeemedPage(srn, 1, 2)) mustBe Some(localDate)
          userAnswers.get(HowManySharesRedeemedPage(srn, 1, 2)) mustBe Some(456)
          userAnswers.get(TotalConsiderationSharesRedeemedPage(srn, 1, 2)) mustBe Some(money)

          // share-index-2
          userAnswers.get(SharesCompleted(srn, 2)) mustBe Some(SectionCompleted)
          userAnswers.get(TypeOfSharesHeldPage(srn, 2)) mustBe Some(Unquoted)
          userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, 2)) mustBe Some(Contribution)
          userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, 2)) mustBe Some(localDate)
          userAnswers.get(CompanyNameRelatedSharesPage(srn, 2)) mustBe Some("nameOfSharesCompany")
          userAnswers.get(SharesCompanyCrnPage(srn, 2)) mustBe Some(ConditionalYesNo.no("optReasonNoCRN"))
          userAnswers.get(ClassOfSharesPage(srn, 2)) mustBe Some("classOfShares")
          userAnswers.get(HowManySharesPage(srn, 2)) mustBe Some(totalShares)
          userAnswers.get(IdentityTypePage(srn, 2, IdentitySubject.SharesSeller)) mustBe None
          userAnswers.get(IndividualNameOfSharesSellerPage(srn, 2)) mustBe None
          userAnswers.get(SharesIndividualSellerNINumberPage(srn, 2)) mustBe None
          userAnswers.get(SharesFromConnectedPartyPage(srn, 2)) mustBe Some(true)
          userAnswers.get(CostOfSharesPage(srn, 2)) mustBe Some(money)
          userAnswers.get(SharesIndependentValuationPage(srn, 2)) mustBe Some(true)
          userAnswers.get(TotalAssetValuePage(srn, 2)) mustBe None
          userAnswers.get(SharesTotalIncomePage(srn, 2)) mustBe Some(money)
          // share-dispose-index-2-1
          userAnswers.get(SharesDisposalProgress(srn, 2, 1)) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWereSharesDisposedPage(srn, 2, 1)) mustBe Some(Transferred)
          userAnswers.get(HowManyDisposalSharesPage(srn, 2, 1)) mustBe Some(totalShares)
          // share-dispose-index-2-2
          userAnswers.get(SharesDisposalProgress(srn, 2, 2)) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWereSharesDisposedPage(srn, 2, 2)) mustBe Some(Other("OtherMethod"))
          userAnswers.get(HowManyDisposalSharesPage(srn, 2, 2)) mustBe Some(totalShares)

          // share-index-3
          userAnswers.get(SharesCompleted(srn, 3)) mustBe Some(SectionCompleted)
          userAnswers.get(TypeOfSharesHeldPage(srn, 3)) mustBe Some(ConnectedParty)
          userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, 3)) mustBe Some(Transfer)
          userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, 3)) mustBe None
          userAnswers.get(CompanyNameRelatedSharesPage(srn, 3)) mustBe Some("nameOfSharesCompany")
          userAnswers.get(SharesCompanyCrnPage(srn, 3)) mustBe Some(ConditionalYesNo.no("optReasonNoCRN"))
          userAnswers.get(ClassOfSharesPage(srn, 3)) mustBe Some("classOfShares")
          userAnswers.get(HowManySharesPage(srn, 3)) mustBe Some(totalShares)
          userAnswers.get(IdentityTypePage(srn, 3, IdentitySubject.SharesSeller)) mustBe None
          userAnswers.get(IndividualNameOfSharesSellerPage(srn, 3)) mustBe None
          userAnswers.get(SharesIndividualSellerNINumberPage(srn, 3)) mustBe None
          userAnswers.get(SharesFromConnectedPartyPage(srn, 3)) mustBe None
          userAnswers.get(CostOfSharesPage(srn, 3)) mustBe Some(money)
          userAnswers.get(SharesIndependentValuationPage(srn, 3)) mustBe Some(false)
          userAnswers.get(TotalAssetValuePage(srn, 3)) mustBe None
          userAnswers.get(SharesTotalIncomePage(srn, 3)) mustBe Some(money)
          userAnswers.get(TotalValueQuotedSharesPage(srn)) mustBe Some(money)
        }
      )
    }
    "when it is pre-population" in {

      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        Shares(
          recordVersion = Some("001"),
          optDidSchemeHoldAnyShares = None,
          optShareTransactions = Some(
            List(
              ShareTransaction(
                prePopulated = None,
                typeOfSharesHeld = SponsoringEmployer,
                shareIdentification = ShareIdentification(
                  nameOfSharesCompany = "nameOfSharesCompany",
                  optCrnNumber = None,
                  optReasonNoCRN = Some("optReasonNoCRN"),
                  classOfShares = "classOfShares"
                ),
                heldSharesTransaction = HeldSharesTransaction(
                  schemeHoldShare = SchemeHoldShare.Acquisition,
                  optDateOfAcqOrContrib = Some(localDate),
                  totalShares = totalShares,
                  optAcquiredFromName = Some("optAcquiredFromName"),
                  optPropertyAcquiredFrom =
                    Some(PropertyAcquiredFrom(IdentityType.Individual, None, Some(noninoReason), None)),
                  optConnectedPartyStatus = None,
                  costOfShares = money.value,
                  supportedByIndepValuation = true,
                  optTotalAssetValue = Some(money.value),
                  optTotalDividendsOrReceipts = None
                ),
                optDisposedSharesTransaction = None
              ),
              ShareTransaction(
                prePopulated = None,
                typeOfSharesHeld = TypeOfShares.Unquoted,
                shareIdentification = ShareIdentification(
                  nameOfSharesCompany = "nameOfSharesCompany",
                  optCrnNumber = None,
                  optReasonNoCRN = Some("optReasonNoCRN"),
                  classOfShares = "classOfShares"
                ),
                heldSharesTransaction = HeldSharesTransaction(
                  schemeHoldShare = SchemeHoldShare.Contribution,
                  optDateOfAcqOrContrib = Some(localDate),
                  totalShares = totalShares,
                  optAcquiredFromName = None,
                  optPropertyAcquiredFrom = None,
                  optConnectedPartyStatus = Some(true),
                  costOfShares = money.value,
                  supportedByIndepValuation = true,
                  optTotalAssetValue = None,
                  optTotalDividendsOrReceipts = None
                ),
                optDisposedSharesTransaction = None
              ),
              ShareTransaction(
                prePopulated = None,
                typeOfSharesHeld = TypeOfShares.ConnectedParty,
                shareIdentification = ShareIdentification(
                  nameOfSharesCompany = "nameOfSharesCompany",
                  optCrnNumber = None,
                  optReasonNoCRN = Some("optReasonNoCRN"),
                  classOfShares = "classOfShares"
                ),
                heldSharesTransaction = HeldSharesTransaction(
                  schemeHoldShare = SchemeHoldShare.Transfer,
                  optDateOfAcqOrContrib = None,
                  totalShares = totalShares,
                  optAcquiredFromName = None,
                  optPropertyAcquiredFrom = None,
                  optConnectedPartyStatus = None,
                  costOfShares = money.value,
                  supportedByIndepValuation = false,
                  optTotalAssetValue = None,
                  optTotalDividendsOrReceipts = None
                ),
                optDisposedSharesTransaction = None
              )
            )
          ),
          optTotalValueQuotedShares = None
        )
      )

      result.fold(
        ex => fail(ex.getMessage + "\n" + ex.getStackTrace.mkString("\n")),
        userAnswers => {
          userAnswers.get(SharesRecordVersionPage(srn)) mustBe Some("001")
          userAnswers.get(DidSchemeHoldAnySharesPage(srn)) mustBe None

          // share-index-1
          userAnswers.get(SharesTotalIncomePage(srn, 1)) mustBe Some(Money(0.0))

          // share-index-2
          userAnswers.get(SharesTotalIncomePage(srn, 2)) mustBe Some(Money(0.0))

          // share-index-3
          userAnswers.get(SharesTotalIncomePage(srn, 3)) mustBe Some(Money(0.0))
        }
      )
    }

    "should not default total income to zero when prePopulated entity is not yet checked" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        sharesBlankTotalIncome(prePopulated = Some(false), recordVersion = Some("001"))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => userAnswers.get(SharesTotalIncomePage(srn, 1)) mustBe None
      )
    }

    "should default total income to zero when prePopulated entity is checked" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        sharesBlankTotalIncome(prePopulated = Some(true), recordVersion = Some("001"))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => userAnswers.get(SharesTotalIncomePage(srn, 1)) mustBe Some(Money(0))
      )
    }

    "should default total income to zero when the version of the return is more than 1" in {
      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        sharesBlankTotalIncome(prePopulated = None, recordVersion = Some("002"))
      )
      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => userAnswers.get(SharesTotalIncomePage(srn, 1)) mustBe Some(Money(0))
      )
    }

    "when shareTransactionList has no disposals" in {
      val shares = Shares(
        recordVersion = Some("001"),
        optDidSchemeHoldAnyShares = Some(true),
        optShareTransactions = Some(
          List(
            ShareTransaction(
              prePopulated = None,
              typeOfSharesHeld = SponsoringEmployer,
              shareIdentification = ShareIdentification(
                nameOfSharesCompany = "nameOfSharesCompany",
                optCrnNumber = None,
                optReasonNoCRN = Some("optReasonNoCRN"),
                classOfShares = "classOfShares"
              ),
              heldSharesTransaction = HeldSharesTransaction(
                schemeHoldShare = SchemeHoldShare.Acquisition,
                optDateOfAcqOrContrib = Some(localDate),
                totalShares = totalShares,
                optAcquiredFromName = Some("optAcquiredFromName"),
                optPropertyAcquiredFrom =
                  Some(PropertyAcquiredFrom(IdentityType.Individual, None, Some(noninoReason), None)),
                optConnectedPartyStatus = None,
                costOfShares = money.value,
                supportedByIndepValuation = true,
                optTotalAssetValue = Some(money.value),
                optTotalDividendsOrReceipts = Some(money.value)
              ),
              optDisposedSharesTransaction = None // No disposals
            )
          )
        ),
        optTotalValueQuotedShares = None
      )

      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, shares)

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(SharesDisposalPage(srn)) mustBe Some(false)
          userAnswers.get(SharesDisposalCompleted(srn)) mustBe Some(SectionCompleted)
        }
      )
    }

    "should transform from ETMP for UK Company seller with CRN" in {
      val shares = Shares(
        recordVersion = None,
        optDidSchemeHoldAnyShares = Some(true),
        optShareTransactions = Some(
          List(
            ShareTransaction(
              prePopulated = None,
              typeOfSharesHeld = SponsoringEmployer,
              shareIdentification = ShareIdentification("company", Some(crn.value), None, "class"),
              heldSharesTransaction = HeldSharesTransaction(
                schemeHoldShare = Acquisition,
                optDateOfAcqOrContrib = Some(localDate),
                totalShares = 1,
                optAcquiredFromName = Some("companyName"),
                optPropertyAcquiredFrom = Some(
                  PropertyAcquiredFrom(IdentityType.UKCompany, Some(crn.value), None, None)
                ),
                optConnectedPartyStatus = None,
                costOfShares = money.value,
                supportedByIndepValuation = true,
                optTotalAssetValue = Some(money.value),
                optTotalDividendsOrReceipts = Some(money.value)
              ),
              optDisposedSharesTransaction = None
            )
          )
        ),
        optTotalValueQuotedShares = None
      )

      val result = transformer.transformFromEtmp(emptyUserAnswers, srn, shares)

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(SharesCompanyCrnPage(srn, 1)) mustBe Some(ConditionalYesNo.yes(crn))
          userAnswers.get(IdentityTypePage(srn, 1, IdentitySubject.SharesSeller)) mustBe Some(IdentityType.UKCompany)
          userAnswers.get(CompanyNameOfSharesSellerPage(srn, 1)) mustBe Some("companyName")
          userAnswers.get(CompanyRecipientCrnPage(srn, 1, IdentitySubject.SharesSeller)) mustBe Some(
            ConditionalYesNo.yes(crn)
          )
        }
      )
    }
  }
}
