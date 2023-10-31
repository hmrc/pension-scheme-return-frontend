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

package services

import cats.data.NonEmptyList
import connectors.PSRConnector
import controllers.TestValues
import models.UserAnswers
import models.requests.psr._
import models.requests.{AllowedAccessRequest, DataRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.JsObject
import play.api.mvc.{AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import services.PsrRetrievalServiceSpec.{assets, loans}
import services.PsrSubmissionServiceSpec.minimalRequiredSubmission
import transformations.{
  LandOrPropertyTransactionsTransformer,
  LoanTransactionsTransformer,
  MinimalRequiredSubmissionTransformer
}
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

class PsrRetrievalServiceSpec extends BaseSpec with TestValues {

  override def beforeEach(): Unit = {
    when(mockReq.schemeDetails).thenReturn(allowedAccessRequest.schemeDetails)
    when(mockReq.pensionSchemeId).thenReturn(allowedAccessRequest.pensionSchemeId)

    reset(mockConnector)
    reset(mockMinimalRequiredSubmissionTransformer)
    reset(mockLoanTransactionsTransformer)
    reset(mockLandOrPropertyTransactionsTransformer)
  }

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val mockConnector = mock[PSRConnector]
  private val mockMinimalRequiredSubmissionTransformer = mock[MinimalRequiredSubmissionTransformer]
  private val mockLoanTransactionsTransformer = mock[LoanTransactionsTransformer]
  private val mockLandOrPropertyTransactionsTransformer = mock[LandOrPropertyTransactionsTransformer]
  private val mockReq = mock[DataRequest[AnyContent]]

  private val service =
    new PsrRetrievalService(
      mockConnector,
      mockMinimalRequiredSubmissionTransformer,
      mockLoanTransactionsTransformer,
      mockLandOrPropertyTransactionsTransformer
    )

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "PSRRetievalService" - {

    "should not getPsrDetails return data when not found" in {
      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any()))
        .thenReturn(Future.successful(None))
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, never).transformFromEtmp(any(), any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
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
              minimalRequiredSubmission,
              false,
              None,
              None
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
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
              minimalRequiredSubmission,
              false,
              Some(loans),
              None
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLoanTransactionsTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
          result mustBe a[UserAnswers]
          result.data.decryptedValue must not be JsObject.empty
      }
    }

    "should getPsrDetails return data when minimal data and assets data were found in etmp" in {
      when(mockMinimalRequiredSubmissionTransformer.transformFromEtmp(any(), any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockLandOrPropertyTransactionsTransformer.transformFromEtmp(any(), any(), any()))
        .thenReturn(Try(defaultUserAnswers))
      when(mockConnector.getStandardPsrDetails(any(), any(), any(), any())(any(), any())).thenReturn(
        Future.successful(
          Some(
            PsrSubmission(
              minimalRequiredSubmission,
              false,
              None,
              Some(assets)
            )
          )
        )
      )
      whenReady(service.getStandardPsrDetails(None, Some(pstr), Some(version))(mockReq, implicitly, implicitly)) {
        result: UserAnswers =>
          verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformFromEtmp(any(), any(), any(), any())
          verify(mockLandOrPropertyTransactionsTransformer, times(1)).transformFromEtmp(any(), any(), any())
          verify(mockLoanTransactionsTransformer, never).transformFromEtmp(any(), any(), any())
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

  val assets: Assets = Assets(
    LandOrProperty(landOrPropertyHeld = true, landOrPropertyTransactions = Seq.empty)
  )
}
