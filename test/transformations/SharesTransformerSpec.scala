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

package transformations

import play.api.test.FakeRequest
import org.scalatest.matchers.must.Matchers
import models.IdentityType.UKCompany
import controllers.TestValues
import models.SchemeHoldShare.{Acquisition, Contribution, Transfer}
import models.TypeOfShares.{ConnectedParty, SponsoringEmployer, Unquoted}
import models.requests.psr._
import eu.timepit.refined.refineMV
import pages.nonsipp.sharesdisposal._
import utils.UserAnswersUtils.UserAnswersOps
import generators.ModelGenerators.allowedAccessRequestGen
import models._
import pages.nonsipp.common.IdentityTypePage
import viewmodels.models.{SectionCompleted, SectionJourneyStatus}
import models.requests.{AllowedAccessRequest, DataRequest}
import org.scalatest.freespec.AnyFreeSpec
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage
import pages.nonsipp.shares._
import play.api.mvc.AnyContentAsEmpty
import org.scalatest.OptionValues
import models.HowSharesDisposed._

class SharesTransformerSpec extends AnyFreeSpec with Matchers with OptionValues with TestValues {

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val transformer = new SharesTransformer

  "SharesTransformer - To Etmp" - {
    "should return None values when userAnswer is empty" in {

      val result = transformer.transformToEtmp(srn = srn, sharesDisposal = false)
      result.optShareTransactions mustBe None
      result.optTotalValueQuotedShares mustBe None
    }

    "should return Quoted share value when quoted share is in userAnswers" in {
      val userAnswers = emptyUserAnswers
        .unsafeSet(TotalValueQuotedSharesPage(srn), money)

      val request = DataRequest(allowedAccessRequest, userAnswers)
      val result = transformer.transformToEtmp(srn = srn, sharesDisposal = false)(request)
      result mustBe Shares(optShareTransactions = None, optTotalValueQuotedShares = Some(money.value))
    }
  }

  "SharesTransformer - From Etmp" - {
    "when shareTransactionList None" in {
      val userAnswers = emptyUserAnswers
      val result = transformer.transformFromEtmp(
        userAnswers,
        srn,
        Shares(optShareTransactions = None, optTotalValueQuotedShares = None)
      )
      result.fold(ex => fail(ex.getMessage), userAnswers => userAnswers mustBe userAnswers)
    }

    "when shareTransactionList not empty" in {

      val result = transformer.transformFromEtmp(
        emptyUserAnswers,
        srn,
        Shares(
          optShareTransactions = Some(
            List(
              ShareTransaction(
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
                  totalDividendsOrReceipts = money.value
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
                  totalDividendsOrReceipts = money.value
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
                  totalDividendsOrReceipts = money.value
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
          userAnswers.get(DidSchemeHoldAnySharesPage(srn)) mustBe Some(true)

          // share-index-1
          userAnswers.get(SharesCompleted(srn, refineMV(1))) mustBe Some(SectionCompleted)
          userAnswers.get(TypeOfSharesHeldPage(srn, refineMV(1))) mustBe Some(SponsoringEmployer)
          userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, refineMV(1))) mustBe Some(Acquisition)
          userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, refineMV(1))) mustBe Some(localDate)
          userAnswers.get(CompanyNameRelatedSharesPage(srn, refineMV(1))) mustBe Some("nameOfSharesCompany")
          userAnswers.get(SharesCompanyCrnPage(srn, refineMV(1))) mustBe Some(ConditionalYesNo.no("optReasonNoCRN"))
          userAnswers.get(ClassOfSharesPage(srn, refineMV(1))) mustBe Some("classOfShares")
          userAnswers.get(HowManySharesPage(srn, refineMV(1))) mustBe Some(totalShares)
          userAnswers.get(IdentityTypePage(srn, refineMV(1), IdentitySubject.SharesSeller)) mustBe Some(
            IdentityType.Individual
          )
          userAnswers.get(IndividualNameOfSharesSellerPage(srn, refineMV(1))) mustBe Some("optAcquiredFromName")
          userAnswers.get(SharesIndividualSellerNINumberPage(srn, refineMV(1))) mustBe Some(
            ConditionalYesNo.no(noninoReason)
          )
          userAnswers.get(SharesFromConnectedPartyPage(srn, refineMV(1))) mustBe None
          userAnswers.get(CostOfSharesPage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(SharesIndependentValuationPage(srn, refineMV(1))) mustBe Some(true)
          userAnswers.get(TotalAssetValuePage(srn, refineMV(1))) mustBe Some(money)
          userAnswers.get(SharesTotalIncomePage(srn, refineMV(1))) mustBe Some(money)
          // share-dispose-index-1-1
          userAnswers.get(SharesDisposalProgress(srn, refineMV(1), refineMV(1))) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWereSharesDisposedPage(srn, refineMV(1), refineMV(1))) mustBe Some(Sold)
          userAnswers.get(HowManyDisposalSharesPage(srn, refineMV(1), refineMV(1))) mustBe Some(totalShares)
          userAnswers.get(WhenWereSharesSoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(localDate)
          userAnswers.get(HowManySharesSoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(123)
          userAnswers.get(TotalConsiderationSharesSoldPage(srn, refineMV(1), refineMV(1))) mustBe Some(money)
          userAnswers.get(WhoWereTheSharesSoldToPage(srn, refineMV(1), refineMV(1))) mustBe Some(UKCompany)
          userAnswers.get(CompanyBuyerNamePage(srn, refineMV(1), refineMV(1))) mustBe Some("nameOfPurchaser")
          userAnswers.get(CompanyBuyerCrnPage(srn, refineMV(1), refineMV(1))) mustBe Some(
            ConditionalYesNo.no("UKCompanyNoReason")
          )
          userAnswers.get(IsBuyerConnectedPartyPage(srn, refineMV(1), refineMV(1))) mustBe Some(true)
          userAnswers.get(IndependentValuationPage(srn, refineMV(1), refineMV(1))) mustBe Some(false)
          // share-dispose-index-1-2
          userAnswers.get(SharesDisposalProgress(srn, refineMV(1), refineMV(2))) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWereSharesDisposedPage(srn, refineMV(1), refineMV(2))) mustBe Some(Redeemed)
          userAnswers.get(HowManyDisposalSharesPage(srn, refineMV(1), refineMV(2))) mustBe Some(totalShares)
          userAnswers.get(WhenWereSharesRedeemedPage(srn, refineMV(1), refineMV(2))) mustBe Some(localDate)
          userAnswers.get(HowManySharesRedeemedPage(srn, refineMV(1), refineMV(2))) mustBe Some(456)
          userAnswers.get(TotalConsiderationSharesRedeemedPage(srn, refineMV(1), refineMV(2))) mustBe Some(money)

          // share-index-2
          userAnswers.get(SharesCompleted(srn, refineMV(2))) mustBe Some(SectionCompleted)
          userAnswers.get(TypeOfSharesHeldPage(srn, refineMV(2))) mustBe Some(Unquoted)
          userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, refineMV(2))) mustBe Some(Contribution)
          userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, refineMV(2))) mustBe Some(localDate)
          userAnswers.get(CompanyNameRelatedSharesPage(srn, refineMV(2))) mustBe Some("nameOfSharesCompany")
          userAnswers.get(SharesCompanyCrnPage(srn, refineMV(2))) mustBe Some(ConditionalYesNo.no("optReasonNoCRN"))
          userAnswers.get(ClassOfSharesPage(srn, refineMV(2))) mustBe Some("classOfShares")
          userAnswers.get(HowManySharesPage(srn, refineMV(2))) mustBe Some(totalShares)
          userAnswers.get(IdentityTypePage(srn, refineMV(2), IdentitySubject.SharesSeller)) mustBe None
          userAnswers.get(IndividualNameOfSharesSellerPage(srn, refineMV(2))) mustBe None
          userAnswers.get(SharesIndividualSellerNINumberPage(srn, refineMV(2))) mustBe None
          userAnswers.get(SharesFromConnectedPartyPage(srn, refineMV(2))) mustBe Some(true)
          userAnswers.get(CostOfSharesPage(srn, refineMV(2))) mustBe Some(money)
          userAnswers.get(SharesIndependentValuationPage(srn, refineMV(2))) mustBe Some(true)
          userAnswers.get(TotalAssetValuePage(srn, refineMV(2))) mustBe None
          userAnswers.get(SharesTotalIncomePage(srn, refineMV(2))) mustBe Some(money)
          // share-dispose-index-2-1
          userAnswers.get(SharesDisposalProgress(srn, refineMV(2), refineMV(1))) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWereSharesDisposedPage(srn, refineMV(2), refineMV(1))) mustBe Some(Transferred)
          userAnswers.get(HowManyDisposalSharesPage(srn, refineMV(2), refineMV(1))) mustBe Some(totalShares)
          // share-dispose-index-2-2
          userAnswers.get(SharesDisposalProgress(srn, refineMV(2), refineMV(2))) mustBe Some(
            SectionJourneyStatus.Completed
          )
          userAnswers.get(HowWereSharesDisposedPage(srn, refineMV(2), refineMV(2))) mustBe Some(Other("OtherMethod"))
          userAnswers.get(HowManyDisposalSharesPage(srn, refineMV(2), refineMV(2))) mustBe Some(totalShares)

          // share-index-3
          userAnswers.get(SharesCompleted(srn, refineMV(3))) mustBe Some(SectionCompleted)
          userAnswers.get(TypeOfSharesHeldPage(srn, refineMV(3))) mustBe Some(ConnectedParty)
          userAnswers.get(WhyDoesSchemeHoldSharesPage(srn, refineMV(3))) mustBe Some(Transfer)
          userAnswers.get(WhenDidSchemeAcquireSharesPage(srn, refineMV(3))) mustBe None
          userAnswers.get(CompanyNameRelatedSharesPage(srn, refineMV(3))) mustBe Some("nameOfSharesCompany")
          userAnswers.get(SharesCompanyCrnPage(srn, refineMV(3))) mustBe Some(ConditionalYesNo.no("optReasonNoCRN"))
          userAnswers.get(ClassOfSharesPage(srn, refineMV(3))) mustBe Some("classOfShares")
          userAnswers.get(HowManySharesPage(srn, refineMV(3))) mustBe Some(totalShares)
          userAnswers.get(IdentityTypePage(srn, refineMV(3), IdentitySubject.SharesSeller)) mustBe None
          userAnswers.get(IndividualNameOfSharesSellerPage(srn, refineMV(3))) mustBe None
          userAnswers.get(SharesIndividualSellerNINumberPage(srn, refineMV(3))) mustBe None
          userAnswers.get(SharesFromConnectedPartyPage(srn, refineMV(3))) mustBe None
          userAnswers.get(CostOfSharesPage(srn, refineMV(3))) mustBe Some(money)
          userAnswers.get(SharesIndependentValuationPage(srn, refineMV(3))) mustBe Some(false)
          userAnswers.get(TotalAssetValuePage(srn, refineMV(3))) mustBe None
          userAnswers.get(SharesTotalIncomePage(srn, refineMV(3))) mustBe Some(money)
          userAnswers.get(TotalValueQuotedSharesPage(srn)) mustBe Some(money)
        }
      )
    }
  }
}
