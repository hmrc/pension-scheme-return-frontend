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

package services

import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import connectors.PSRConnector
import controllers.TestValues
import models.requests.psr._
import transformations._
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.HeaderCarrier
import models.UserAnswers
import models.requests.{AllowedAccessRequest, DataRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import services.PsrSubmissionServiceSpec.minimalRequiredSubmission
import play.api.test.FakeRequest
import utils.BaseSpec
import org.mockito.Mockito._
import cats.data.NonEmptyList
import services.PsrRetrievalServiceSpec._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

import java.time.LocalDate

class PsrRetrievalServiceSpec extends BaseSpec with TestValues {

  override def beforeEach(): Unit = {
    when(mockReq.schemeDetails).thenReturn(allowedAccessRequest.schemeDetails)
    when(mockReq.pensionSchemeId).thenReturn(allowedAccessRequest.pensionSchemeId)

    reset(mockConnector)
    reset(mockMinimalRequiredSubmissionTransformer)
    reset(mockLoanTransactionsTransformer)
    reset(mockLandOrPropertyTransactionsTransformer)
    reset(mockMoneyBorrowedTransformer)
    reset(mockMemberPaymentsTransformer)
    reset(mockSharesTransformer)
    reset(mockBondTransactionsTransformer)
    reset(mockOtherAssetTransactionsTransformer)
  }

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val mockConnector = mock[PSRConnector]
  private val mockMinimalRequiredSubmissionTransformer = mock[MinimalRequiredSubmissionTransformer]
  private val mockLoanTransactionsTransformer = mock[LoanTransactionsTransformer]
  private val mockLandOrPropertyTransactionsTransformer = mock[LandOrPropertyTransactionsTransformer]
  private val mockMoneyBorrowedTransformer = mock[MoneyBorrowedTransformer]
  private val mockMemberPaymentsTransformer = mock[MemberPaymentsTransformer]
  private val mockSharesTransformer = mock[SharesTransformer]
  private val mockBondTransactionsTransformer = mock[BondTransactionsTransformer]
  private val mockOtherAssetTransactionsTransformer = mock[OtherAssetTransactionsTransformer]
  private val mockReq = mock[DataRequest[AnyContent]]

  private val service =
    new PsrRetrievalService(
      mockConnector,
      mockMinimalRequiredSubmissionTransformer,
      mockLoanTransactionsTransformer,
      mockLandOrPropertyTransactionsTransformer,
      mockMoneyBorrowedTransformer,
      mockMemberPaymentsTransformer,
      mockSharesTransformer,
      mockBondTransactionsTransformer,
      mockOtherAssetTransactionsTransformer
    )

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "PSRRetrievalService" - {

    "should not getPsrDetails return data when not found" in {
      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, never).transformFromEtmp(any(), any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMoneyBorrowedTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          result mustBe a[UserAnswers]
          result.data.decryptedValue mustBe JsObject.empty
      }
    }
    "should getPsrDetails return data when only minimal data was found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any())).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = None,
              membersPayments = None,
              shares = None
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMoneyBorrowedTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          result mustBe a[UserAnswers]
          result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and loans data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockLoanTransactionsTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any())).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = Some(loans),
              assets = None,
              membersPayments = None,
              shares = None
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLoanTransactionsTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMoneyBorrowedTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          result mustBe a[UserAnswers]
          result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and assets with only LandOrProperty data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockLandOrPropertyTransactionsTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any())).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = Some(assetsWithLandOrProperty),
              membersPayments = None,
              shares = None
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockMoneyBorrowedTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          result mustBe a[UserAnswers]
          result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and assets with only borrowing data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockMoneyBorrowedTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any())).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = Some(assetsWithBorrowing),
              membersPayments = None,
              shares = None
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMoneyBorrowedTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          result mustBe a[UserAnswers]
          result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and member payments data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockMemberPaymentsTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))

      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any())).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = None,
              membersPayments = Some(memberPayments),
              shares = None
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMoneyBorrowedTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMemberPaymentsTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          result mustBe a[UserAnswers]
          result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and shares data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockSharesTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))

      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any())).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = None,
              membersPayments = None,
              shares = Some(shares)
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMoneyBorrowedTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockSharesTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockBondTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          result mustBe a[UserAnswers]
          result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and assets with only bonds data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockBondTransactionsTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any())).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = Some(assetsWithBonds),
              membersPayments = None,
              shares = None
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMoneyBorrowedTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondTransactionsTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          result mustBe a[UserAnswers]
          result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and assets with only other-assets data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockOtherAssetTransactionsTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any())).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = Some(assetsWithOtherAssets),
              membersPayments = None,
              shares = None
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMoneyBorrowedTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockBondTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockOtherAssetTransactionsTransformer, times(1)).transformFromEtmp(any(), any(), any())
          result mustBe a[UserAnswers]
          result.data.decryptedValue must not be JsObject.empty
      }
    }
  }
}

object PsrRetrievalServiceSpec {
  val captor: ArgumentCaptor[PsrSubmission] = ArgumentCaptor.forClass(classOf[PsrSubmission])

  val sampleDate: LocalDate = LocalDate.now
  val minimalRequiredSubmission: MinimalRequiredSubmission = MinimalRequiredSubmission(
    ReportDetails("testPstr", sampleDate, sampleDate),
    NonEmptyList.of(sampleDate -> sampleDate),
    SchemeDesignatory(
      openBankAccount = true,
      Some("reasonForNoBankAccount"),
      1,
      2,
      3,
      None,
      None,
      None,
      None,
      None
    )
  )
  val loans: Loans = Loans(
    schemeHadLoans = true,
    loanTransactions = Seq.empty
  )

  val assetsWithLandOrProperty: Assets = Assets(
    optLandOrProperty = Some(
      LandOrProperty(
        landOrPropertyHeld = true,
        disposeAnyLandOrProperty = true,
        landOrPropertyTransactions = Seq.empty
      )
    ),
    optBorrowing = None,
    optBonds = None,
    optOtherAssets = None
  )

  val assetsWithBorrowing: Assets = Assets(
    optLandOrProperty = None,
    optBorrowing = Some(Borrowing(moneyWasBorrowed = true, moneyBorrowed = Seq.empty)),
    optBonds = None,
    optOtherAssets = None
  )

  val assetsWithBonds: Assets = Assets(
    optLandOrProperty = None,
    optBorrowing = None,
    optBonds = Some(Bonds(bondsWereAdded = true, bondsWereDisposed = true, bondTransactions = Seq.empty)),
    optOtherAssets = None
  )

  val assetsWithOtherAssets: Assets = Assets(
    optLandOrProperty = None,
    optBorrowing = None,
    optBonds = None,
    optOtherAssets =
      Some(OtherAssets(otherAssetsWereHeld = true, otherAssetsWereDisposed = true, otherAssetTransactions = Seq.empty))
  )

  val memberPayments: MemberPayments = MemberPayments(
    memberDetails = List.empty,
    employerContributionsDetails = SectionDetails(made = true, completed = true),
    transfersInCompleted = true,
    transfersOutCompleted = true,
    unallocatedContribsMade = true,
    unallocatedContribAmount = None,
    memberContributionMade = true,
    lumpSumReceived = true,
    pensionReceived = true,
    benefitsSurrenderedDetails = SectionDetails(made = true, completed = true)
  )

  val shares: Shares = Shares(optShareTransactions = None, optTotalValueQuotedShares = None)
}
