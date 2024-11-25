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

import play.api.test.FakeRequest
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import connectors.PSRConnector
import controllers.TestValues
import transformations._
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.HeaderCarrier
import models.UserAnswers
import models.requests.{AllowedAccessRequest, DataRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import utils.BaseSpec
import org.mockito.Mockito._
import cats.data.NonEmptyList
import services.PsrRetrievalServiceSpec._
import models.requests.psr._
import config.Constants.PSA

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
    reset(mockLoansTransformer)
    reset(mockAssetsTransformer)
    reset(mockMemberPaymentsTransformer)
    reset(mockSharesTransformer)
    reset(mockDeclarationTransformer)
  }

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val mockConnector = mock[PSRConnector]
  private val mockMinimalRequiredSubmissionTransformer = mock[MinimalRequiredSubmissionTransformer]
  private val mockLoansTransformer = mock[LoansTransformer]
  private val mockMemberPaymentsTransformer = mock[MemberPaymentsTransformer]
  private val mockAssetsTransformer = mock[AssetsTransformer]
  private val mockSharesTransformer = mock[SharesTransformer]
  private val mockDeclarationTransformer = mock[DeclarationTransformer]
  private val mockReq = mock[DataRequest[AnyContent]]

  private val service =
    new PsrRetrievalService(
      mockConnector,
      mockMinimalRequiredSubmissionTransformer,
      mockLoansTransformer,
      mockMemberPaymentsTransformer,
      mockSharesTransformer,
      mockAssetsTransformer,
      mockDeclarationTransformer
    )

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "PSRRetrievalService" - {

    "should not getPsrDetails return data when not found" in {
      when(
        mockConnector.getStandardPsrDetails(any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(Future.successful(None))
      whenReady(
        service.getAndTransformStandardPsrDetails(None, Some(pstr), Some(version), fallbackCall)(
          mockReq,
          implicitly,
          implicitly
        )
      ) { result: UserAnswers =>
        verify(mockMinimalRequiredSubmissionTransformer, never).transformFromEtmp(any(), any(), any(), any())
        verify(mockLoansTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockAssetsTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any(), any(), any())
        verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockDeclarationTransformer, never).transformFromEtmp(any(), any(), any())
        result mustBe a[UserAnswers]
        result.data.decryptedValue mustBe JsObject.empty
      }
    }
    "should getPsrDetails return data when only minimal data was found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(
        mockConnector.getStandardPsrDetails(any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = None,
              membersPayments = None,
              shares = None,
              psrDeclaration = None
            )
          )
        )
      )
      whenReady(
        service.getAndTransformStandardPsrDetails(None, Some(pstr), Some(version), fallbackCall)(
          mockReq,
          implicitly,
          implicitly
        )
      ) { result: UserAnswers =>
        verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
        verify(mockLoansTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockAssetsTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any(), any(), any())
        verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockDeclarationTransformer, never).transformFromEtmp(any(), any(), any())
        result mustBe a[UserAnswers]
        result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and loans data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockLoansTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(
        mockConnector.getStandardPsrDetails(any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = Some(loans),
              assets = None,
              membersPayments = None,
              shares = None,
              psrDeclaration = None
            )
          )
        )
      )
      whenReady(
        service.getAndTransformStandardPsrDetails(None, Some(pstr), Some(version), fallbackCall)(
          mockReq,
          implicitly,
          implicitly
        )
      ) { result: UserAnswers =>
        verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
        verify(mockLoansTransformer, times(1)).transformFromEtmp(any(), any(), any())
        verify(mockAssetsTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any(), any(), any())
        verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockDeclarationTransformer, never).transformFromEtmp(any(), any(), any())
        result mustBe a[UserAnswers]
        result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and assets data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockAssetsTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(
        mockConnector.getStandardPsrDetails(any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = Some(assets),
              membersPayments = None,
              shares = None,
              psrDeclaration = None
            )
          )
        )
      )
      whenReady(
        service.getAndTransformStandardPsrDetails(None, Some(pstr), Some(version), fallbackCall)(
          mockReq,
          implicitly,
          implicitly
        )
      ) { result: UserAnswers =>
        verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
        verify(mockAssetsTransformer, times(1)).transformFromEtmp(any(), any(), any())
        verify(mockLoansTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any(), any(), any())
        verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockDeclarationTransformer, never).transformFromEtmp(any(), any(), any())
        result mustBe a[UserAnswers]
        result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and member payments data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockMemberPaymentsTransformer.transformFromEtmp(any(), any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))

      when(
        mockConnector.getStandardPsrDetails(any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = None,
              membersPayments = Some(memberPayments),
              shares = None,
              psrDeclaration = None
            )
          )
        )
      )
      whenReady(
        service.getAndTransformStandardPsrDetails(None, Some(pstr), Some(version), fallbackCall)(
          mockReq,
          implicitly,
          implicitly
        )
      ) { result: UserAnswers =>
        verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
        verify(mockAssetsTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockLoansTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockMemberPaymentsTransformer, times(1)).transformFromEtmp(any(), any(), any(), any(), any())
        verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockDeclarationTransformer, never).transformFromEtmp(any(), any(), any())
        result mustBe a[UserAnswers]
        result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and shares data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockSharesTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))

      when(
        mockConnector.getStandardPsrDetails(any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = None,
              membersPayments = None,
              shares = Some(shares),
              psrDeclaration = None
            )
          )
        )
      )
      whenReady(
        service.getAndTransformStandardPsrDetails(None, Some(pstr), Some(version), fallbackCall)(
          mockReq,
          implicitly,
          implicitly
        )
      ) { result: UserAnswers =>
        verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
        verify(mockAssetsTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockLoansTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any(), any(), any())
        verify(mockSharesTransformer, times(1)).transformFromEtmp(any(), any(), any())
        verify(mockDeclarationTransformer, never).transformFromEtmp(any(), any(), any())
        result mustBe a[UserAnswers]
        result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and psr-declaration data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockDeclarationTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(
        mockConnector.getStandardPsrDetails(any(), any(), any(), any(), any(), any(), any(), any())(any(), any(), any())
      ).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission = minimalRequiredSubmission,
              checkReturnDates = false,
              loans = None,
              assets = None,
              membersPayments = None,
              shares = None,
              psrDeclaration = Some(psrDeclaration)
            )
          )
        )
      )
      whenReady(
        service.getAndTransformStandardPsrDetails(None, Some(pstr), Some(version), fallbackCall)(
          mockReq,
          implicitly,
          implicitly
        )
      ) { result: UserAnswers =>
        verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
        verify(mockAssetsTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockLoansTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockMemberPaymentsTransformer, never).transformFromEtmp(any(), any(), any(), any(), any())
        verify(mockSharesTransformer, never).transformFromEtmp(any(), any(), any())
        verify(mockDeclarationTransformer, times(1)).transformFromEtmp(any(), any(), any())
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
    ReportDetails(
      fbVersion = None,
      fbstatus = None,
      pstr = "testPstr",
      periodStart = sampleDate,
      periodEnd = sampleDate,
      compilationOrSubmissionDate = None
    ),
    AccountingPeriodDetails(Some("001"), NonEmptyList.of(sampleDate -> sampleDate)),
    SchemeDesignatory(
      Some("001"),
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
    Some("001"),
    schemeHadLoans = true,
    loanTransactions = Seq.empty
  )

  val assets: Assets = Assets(
    optLandOrProperty = Some(
      LandOrProperty(
        Some("001"),
        optLandOrPropertyHeld = Some(true),
        optDisposeAnyLandOrProperty = Some(true),
        landOrPropertyTransactions = Seq.empty
      )
    ),
    optBorrowing = Some(Borrowing(Some("001"), moneyWasBorrowed = true, moneyBorrowed = Seq.empty)),
    optBonds = Some(Bonds(Some("001"), bondsWereAdded = true, bondsWereDisposed = true, bondTransactions = Seq.empty)),
    optOtherAssets = Some(
      OtherAssets(
        Some("001"),
        otherAssetsWereHeld = true,
        otherAssetsWereDisposed = true,
        otherAssetTransactions = Seq.empty
      )
    )
  )

  val memberPayments: MemberPayments = MemberPayments(
    Some("001"),
    memberDetails = List.empty,
    employerContributionMade = Some(true),
    transfersInMade = Some(true),
    transfersOutMade = Some(true),
    unallocatedContribsMade = Some(true),
    unallocatedContribAmount = None,
    memberContributionMade = Some(true),
    lumpSumReceived = Some(true),
    pensionReceived = Some(true),
    surrenderMade = Some(true)
  )

  val shares: Shares = Shares(Some("001"), Some(false), optShareTransactions = None, optTotalValueQuotedShares = None)

  val psrDeclaration: PsrDeclaration = PsrDeclaration(
    submittedBy = PSA,
    submitterId = "A2100005",
    optAuthorisingPSAID = None,
    declaration1 = true,
    declaration2 = true
  )
}
