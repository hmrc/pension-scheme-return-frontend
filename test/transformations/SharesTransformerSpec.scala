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

import controllers.TestValues
import eu.timepit.refined.refineMV
import generators.ModelGenerators.allowedAccessRequestGen
import models.SchemeHoldShare.{Acquisition, Contribution, Transfer}
import models.TypeOfShares.{ConnectedParty, SponsoringEmployer, Unquoted}
import models.requests.psr._
import models.requests.{AllowedAccessRequest, DataRequest}
import models.{ConditionalYesNo, IdentitySubject, IdentityType, PropertyAcquiredFrom, SchemeHoldShare, TypeOfShares}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify}
import org.mockito.MockitoSugar.{mock, reset, when}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues}
import pages.nonsipp.common.IdentityTypePage
import pages.nonsipp.shares._
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import viewmodels.models.SectionCompleted

class SharesTransformerSpec
    extends AnyFreeSpec
    with Matchers
    with OptionValues
    with TestValues
    with BeforeAndAfterEach {

  override def beforeEach(): Unit =
    reset(mockShareTransactionTransformer)

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val mockShareTransactionTransformer: ShareTransactionTransformer = mock[ShareTransactionTransformer]
  private val transformer = new SharesTransformer(mockShareTransactionTransformer)

  "SharesTransformer - To Etmp" - {
    "should call shareTransactionTransformer" in {
      when(mockShareTransactionTransformer.transformToEtmp(any())(any()))
        .thenReturn(Some(List.empty))
      val result: Shares = transformer.transformToEtmp(srn)
      result mustBe Shares(Some(List.empty))
      verify(mockShareTransactionTransformer, times(1)).transformToEtmp(any())(any())
    }
  }

  "SharesTransformer - From Etmp" - {
    "when shareTransactionList Empty" in {
      val userAnswers = emptyUserAnswers
      val result = transformer.transformFromEtmp(
        userAnswers,
        srn,
        Shares(optShareTransactions = None)
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
                )
              )
            )
          )
        )
      )

      result.fold(
        ex => fail(ex.getMessage),
        userAnswers => {
          userAnswers.get(DidSchemeHoldAnySharesPage(srn)) mustBe Some(true)

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
        }
      )
    }
  }
}