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
import models.requests.psr._
import models.requests.{AllowedAccessRequest, DataRequest}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.CheckReturnDatesPage
import pages.nonsipp.bonds.UnregulatedOrConnectedBondsHeldPage
import pages.nonsipp.bondsdisposal.BondsDisposalPage
import pages.nonsipp.landorproperty.LandOrPropertyHeldPage
import pages.nonsipp.landorpropertydisposal.LandOrPropertyDisposalPage
import pages.nonsipp.loansmadeoroutstanding.LoansMadeOrOutstandingPage
import pages.nonsipp.moneyborrowed.MoneyBorrowedPage
import pages.nonsipp.shares.DidSchemeHoldAnySharesPage
import pages.nonsipp.sharesdisposal.SharesDisposalPage
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import services.PsrSubmissionServiceSpec.{captor, minimalRequiredSubmission}
import transformations._
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseSpec
import utils.UserAnswersUtils.UserAnswersOps

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PsrSubmissionServiceSpec extends BaseSpec with TestValues {

  override def beforeEach(): Unit = {
    reset(mockConnector)
    reset(mockMinimalRequiredSubmissionTransformer)
    reset(mockLoanTransactionsTransformer)
    reset(mockLandOrPropertyTransactionsTransformer)
    reset(mockMoneyBorrowedTransformer)
    reset(mockMemberPaymentsTransformerTransformer)
    reset(mockSharesTransformer)
    reset(mockBondTransactionsTransformer)
  }

  val allowedAccessRequest
    : AllowedAccessRequest[AnyContentAsEmpty.type] = allowedAccessRequestGen(FakeRequest()).sample.value
  implicit val request: DataRequest[AnyContentAsEmpty.type] = DataRequest(allowedAccessRequest, defaultUserAnswers)

  private val mockConnector = mock[PSRConnector]
  private val mockMinimalRequiredSubmissionTransformer = mock[MinimalRequiredSubmissionTransformer]
  private val mockLoanTransactionsTransformer = mock[LoanTransactionsTransformer]
  private val mockLandOrPropertyTransactionsTransformer = mock[LandOrPropertyTransactionsTransformer]
  private val mockMoneyBorrowedTransformer = mock[MoneyBorrowedTransformer]
  private val mockMemberPaymentsTransformerTransformer = mock[MemberPaymentsTransformer]
  private val mockSharesTransformer = mock[SharesTransformer]
  private val mockBondTransactionsTransformer = mock[BondTransactionsTransformer]

  private val service =
    new PsrSubmissionService(
      mockConnector,
      mockMinimalRequiredSubmissionTransformer,
      mockLoanTransactionsTransformer,
      mockLandOrPropertyTransactionsTransformer,
      mockMemberPaymentsTransformerTransformer,
      mockMoneyBorrowedTransformer,
      mockSharesTransformer,
      mockBondTransactionsTransformer
    )

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  "PSRSubmissionService" - {

    List(true, false).foreach(
      checkReturnDatesAnswer =>
        s"should submitPsrDetails request when only minimalRequiredSubmission object is exist and checkReturnDates is $checkReturnDatesAnswer" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), checkReturnDatesAnswer)
          val request = DataRequest(allowedAccessRequest, userAnswers)
          when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any())(any()))
            .thenReturn(Some(minimalRequiredSubmission))
          when(mockConnector.submitPsrDetails(any())(any(), any())).thenReturn(Future.successful(()))

          whenReady(service.submitPsrDetails(srn)(implicitly, implicitly, request)) { result: Option[Unit] =>
            verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any())(any())
            verify(mockLoanTransactionsTransformer, never).transformToEtmp(any())(any())
            verify(mockLandOrPropertyTransactionsTransformer, never).transformToEtmp(any(), any())(any())
            verify(mockMoneyBorrowedTransformer, never).transformToEtmp(any())(any())
            verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
            verify(mockBondTransactionsTransformer, never).transformToEtmp(any(), any())(any())
            verify(mockConnector, times(1)).submitPsrDetails(captor.capture())(any(), any())

            captor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
            captor.getValue.checkReturnDates mustBe checkReturnDatesAnswer
            captor.getValue.loans mustBe None
            captor.getValue.assets mustBe None
            captor.getValue.shares mustBe None
            result mustBe Some(())
          }
        }
    )

    List(true, false).foreach(
      loansMadeOrOutstanding =>
        s"submitPsrDetails request successfully when LoansMadeOrOutstandingPage is $loansMadeOrOutstanding" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), false)
            .unsafeSet(LoansMadeOrOutstandingPage(srn), loansMadeOrOutstanding)
          val request = DataRequest(allowedAccessRequest, userAnswers)

          when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any())(any()))
            .thenReturn(Some(minimalRequiredSubmission))
          when(mockLoanTransactionsTransformer.transformToEtmp(any())(any())).thenReturn(List.empty)
          when(mockConnector.submitPsrDetails(any())(any(), any())).thenReturn(Future.successful(()))

          whenReady(service.submitPsrDetails(srn)(implicitly, implicitly, request)) { result: Option[Unit] =>
            verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any())(any())
            verify(mockLoanTransactionsTransformer, times(1)).transformToEtmp(any())(any())
            verify(mockLandOrPropertyTransactionsTransformer, never).transformToEtmp(any(), any())(any())
            verify(mockMoneyBorrowedTransformer, never).transformToEtmp(any())(any())
            verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
            verify(mockBondTransactionsTransformer, never).transformToEtmp(any(), any())(any())
            verify(mockConnector, times(1)).submitPsrDetails(captor.capture())(any(), any())

            captor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
            captor.getValue.checkReturnDates mustBe false
            captor.getValue.loans mustBe Some(Loans(schemeHadLoans = loansMadeOrOutstanding, List.empty))
            captor.getValue.assets mustBe None
            captor.getValue.shares mustBe None
            result mustBe Some(())
          }
        }
    )

    List(true, false).foreach(
      landOrPropertyHeld =>
        s"submitPsrDetails request successfully when LandOrPropertyHeldPage is $landOrPropertyHeld" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), false)
            .unsafeSet(LandOrPropertyHeldPage(srn), landOrPropertyHeld)
          val request = DataRequest(allowedAccessRequest, userAnswers)

          when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any())(any()))
            .thenReturn(Some(minimalRequiredSubmission))
          when(mockLandOrPropertyTransactionsTransformer.transformToEtmp(any(), any())(any())).thenReturn(List.empty)
          when(mockConnector.submitPsrDetails(any())(any(), any())).thenReturn(Future.successful(()))

          whenReady(service.submitPsrDetails(srn)(implicitly, implicitly, request)) {
            result: Option[Unit] =>
              verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any())(any())
              verify(mockLandOrPropertyTransactionsTransformer, times(1)).transformToEtmp(any(), any())(any())
              verify(mockLoanTransactionsTransformer, never).transformToEtmp(any())(any())
              verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockMoneyBorrowedTransformer, never).transformToEtmp(any())(any())
              verify(mockBondTransactionsTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockConnector, times(1)).submitPsrDetails(captor.capture())(any(), any())

              captor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
              captor.getValue.checkReturnDates mustBe false
              captor.getValue.loans mustBe None
              captor.getValue.assets mustBe Some(
                Assets(
                  optLandOrProperty = Some(
                    LandOrProperty(
                      landOrPropertyHeld = landOrPropertyHeld,
                      disposeAnyLandOrProperty = false,
                      List.empty
                    )
                  ),
                  optBorrowing = None,
                  optBonds = None
                )
              )
              captor.getValue.shares mustBe None
              result mustBe Some(())
          }
        }
    )

    List(true, false).foreach(
      landOrPropertyDisposal =>
        s"submitPsrDetails request successfully when LandOrPropertyDisposalPage is $landOrPropertyDisposal" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), false)
            .unsafeSet(LandOrPropertyHeldPage(srn), true)
            .unsafeSet(LandOrPropertyDisposalPage(srn), landOrPropertyDisposal)
          val request = DataRequest(allowedAccessRequest, userAnswers)

          when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any())(any()))
            .thenReturn(Some(minimalRequiredSubmission))
          when(mockLandOrPropertyTransactionsTransformer.transformToEtmp(any(), any())(any())).thenReturn(List.empty)
          when(mockConnector.submitPsrDetails(any())(any(), any())).thenReturn(Future.successful(()))

          whenReady(service.submitPsrDetails(srn)(implicitly, implicitly, request)) {
            result: Option[Unit] =>
              verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any())(any())
              verify(mockLandOrPropertyTransactionsTransformer, times(1)).transformToEtmp(any(), any())(any())
              verify(mockLoanTransactionsTransformer, never).transformToEtmp(any())(any())
              verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockMoneyBorrowedTransformer, never).transformToEtmp(any())(any())
              verify(mockBondTransactionsTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockConnector, times(1)).submitPsrDetails(captor.capture())(any(), any())

              captor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
              captor.getValue.checkReturnDates mustBe false
              captor.getValue.loans mustBe None
              captor.getValue.assets mustBe Some(
                Assets(
                  optLandOrProperty = Some(
                    LandOrProperty(
                      landOrPropertyHeld = true,
                      disposeAnyLandOrProperty = landOrPropertyDisposal,
                      List.empty
                    )
                  ),
                  optBorrowing = None,
                  optBonds = None
                )
              )
              captor.getValue.shares mustBe None
              result mustBe Some(())
          }
        }
    )

    List(true, false).foreach(
      moneyBorrowed =>
        s"submitPsrDetails request successfully when MoneyBorrowedPage is $moneyBorrowed" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), false)
            .unsafeSet(MoneyBorrowedPage(srn), moneyBorrowed)
          val request = DataRequest(allowedAccessRequest, userAnswers)

          when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any())(any()))
            .thenReturn(Some(minimalRequiredSubmission))
          when(mockMoneyBorrowedTransformer.transformToEtmp(any())(any())).thenReturn(List.empty)
          when(mockConnector.submitPsrDetails(any())(any(), any())).thenReturn(Future.successful(()))

          whenReady(service.submitPsrDetails(srn)(implicitly, implicitly, request)) {
            result: Option[Unit] =>
              verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any())(any())
              verify(mockLoanTransactionsTransformer, never).transformToEtmp(any())(any())
              verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockLandOrPropertyTransactionsTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockMoneyBorrowedTransformer, times(1)).transformToEtmp(any())(any())
              verify(mockBondTransactionsTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockConnector, times(1)).submitPsrDetails(captor.capture())(any(), any())

              captor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
              captor.getValue.checkReturnDates mustBe false
              captor.getValue.loans mustBe None
              captor.getValue.assets mustBe Some(
                Assets(
                  optLandOrProperty = None,
                  optBorrowing = Some(Borrowing(moneyWasBorrowed = moneyBorrowed, List.empty)),
                  optBonds = None
                )
              )
              captor.getValue.shares mustBe None
              result mustBe Some(())
          }
        }
    )

    List(true, false).foreach(
      didSchemeHoldAnyShares =>
        s"submitPsrDetails request successfully when DidSchemeHoldAnySharesPage is $didSchemeHoldAnyShares" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), false)
            .unsafeSet(DidSchemeHoldAnySharesPage(srn), didSchemeHoldAnyShares)
          val request = DataRequest(allowedAccessRequest, userAnswers)

          when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any())(any()))
            .thenReturn(Some(minimalRequiredSubmission))
          when(mockSharesTransformer.transformToEtmp(any(), any())(any()))
            .thenReturn(Shares(optShareTransactions = None, optTotalValueQuotedShares = None))
          when(mockConnector.submitPsrDetails(any())(any(), any())).thenReturn(Future.successful(()))

          whenReady(service.submitPsrDetails(srn)(implicitly, implicitly, request)) { result: Option[Unit] =>
            verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any())(any())
            verify(mockLoanTransactionsTransformer, never).transformToEtmp(any())(any())
            verify(mockSharesTransformer, times(1)).transformToEtmp(srn, sharesDisposal = false)(request)
            verify(mockLandOrPropertyTransactionsTransformer, never).transformToEtmp(any(), any())(any())
            verify(mockMoneyBorrowedTransformer, never).transformToEtmp(any())(any())
            verify(mockBondTransactionsTransformer, never).transformToEtmp(any(), any())(any())
            verify(mockConnector, times(1)).submitPsrDetails(captor.capture())(any(), any())

            captor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
            captor.getValue.checkReturnDates mustBe false
            captor.getValue.loans mustBe None
            captor.getValue.assets mustBe None
            captor.getValue.shares mustBe Some(Shares(optShareTransactions = None, optTotalValueQuotedShares = None))
            result mustBe Some(())
          }
        }
    )

    List(true, false).foreach(
      didSchemeDisposeAnyShares =>
        s"submitPsrDetails request successfully when SharesDisposalPage is $didSchemeDisposeAnyShares" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), false)
            .unsafeSet(DidSchemeHoldAnySharesPage(srn), true)
            .unsafeSet(SharesDisposalPage(srn), didSchemeDisposeAnyShares)
          val request = DataRequest(allowedAccessRequest, userAnswers)

          when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any())(any()))
            .thenReturn(Some(minimalRequiredSubmission))
          when(mockSharesTransformer.transformToEtmp(any(), any())(any()))
            .thenReturn(Shares(optShareTransactions = None, optTotalValueQuotedShares = None))
          when(mockConnector.submitPsrDetails(any())(any(), any())).thenReturn(Future.successful(()))

          whenReady(service.submitPsrDetails(srn)(implicitly, implicitly, request)) {
            result: Option[Unit] =>
              verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any())(any())
              verify(mockLoanTransactionsTransformer, never).transformToEtmp(any())(any())
              verify(mockSharesTransformer, times(1))
                .transformToEtmp(srn, didSchemeDisposeAnyShares)(request)
              verify(mockLandOrPropertyTransactionsTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockMoneyBorrowedTransformer, never).transformToEtmp(any())(any())
              verify(mockBondTransactionsTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockConnector, times(1)).submitPsrDetails(captor.capture())(any(), any())

              captor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
              captor.getValue.checkReturnDates mustBe false
              captor.getValue.loans mustBe None
              captor.getValue.assets mustBe None
              captor.getValue.shares mustBe Some(Shares(optShareTransactions = None, optTotalValueQuotedShares = None))
              result mustBe Some(())
          }
        }
    )

    List(true, false).foreach(
      unregulatedOrConnectedBondsHeld =>
        s"submitPsrDetails request successfully when UnregulatedOrConnectedBondsHeldPage is $unregulatedOrConnectedBondsHeld" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), false)
            .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), unregulatedOrConnectedBondsHeld)
          val request = DataRequest(allowedAccessRequest, userAnswers)

          when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any())(any()))
            .thenReturn(Some(minimalRequiredSubmission))
          when(mockBondTransactionsTransformer.transformToEtmp(any(), any())(any())).thenReturn(List.empty)
          when(mockConnector.submitPsrDetails(any())(any(), any())).thenReturn(Future.successful(()))

          whenReady(service.submitPsrDetails(srn)(implicitly, implicitly, request)) {
            result: Option[Unit] =>
              verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any())(any())
              verify(mockLandOrPropertyTransactionsTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockLoanTransactionsTransformer, never).transformToEtmp(any())(any())
              verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockMoneyBorrowedTransformer, never).transformToEtmp(any())(any())
              verify(mockBondTransactionsTransformer, times(1)).transformToEtmp(srn, bondsDisposal = false)(request)
              verify(mockConnector, times(1)).submitPsrDetails(captor.capture())(any(), any())

              captor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
              captor.getValue.checkReturnDates mustBe false
              captor.getValue.loans mustBe None
              captor.getValue.assets mustBe Some(
                Assets(
                  optLandOrProperty = None,
                  optBorrowing = None,
                  optBonds = Some(
                    Bonds(
                      bondsWereAdded = unregulatedOrConnectedBondsHeld,
                      bondsWereDisposed = false,
                      bondTransactions = List.empty
                    )
                  )
                )
              )
              captor.getValue.shares mustBe None
              result mustBe Some(())
          }
        }
    )

    List(true, false).foreach(
      bondsDisposal =>
        s"submitPsrDetails request successfully when BondsDisposalPage is $bondsDisposal" in {
          val userAnswers = defaultUserAnswers
            .unsafeSet(CheckReturnDatesPage(srn), false)
            .unsafeSet(UnregulatedOrConnectedBondsHeldPage(srn), true)
            .unsafeSet(BondsDisposalPage(srn), bondsDisposal)
          val request = DataRequest(allowedAccessRequest, userAnswers)

          when(mockMinimalRequiredSubmissionTransformer.transformToEtmp(any())(any()))
            .thenReturn(Some(minimalRequiredSubmission))
          when(mockBondTransactionsTransformer.transformToEtmp(any(), any())(any())).thenReturn(List.empty)
          when(mockConnector.submitPsrDetails(any())(any(), any())).thenReturn(Future.successful(()))

          whenReady(service.submitPsrDetails(srn)(implicitly, implicitly, request)) {
            result: Option[Unit] =>
              verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any())(any())
              verify(mockLandOrPropertyTransactionsTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockLoanTransactionsTransformer, never).transformToEtmp(any())(any())
              verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
              verify(mockMoneyBorrowedTransformer, never).transformToEtmp(any())(any())
              verify(mockBondTransactionsTransformer, times(1)).transformToEtmp(srn, bondsDisposal)(request)
              verify(mockConnector, times(1)).submitPsrDetails(captor.capture())(any(), any())

              captor.getValue.minimalRequiredSubmission mustBe minimalRequiredSubmission
              captor.getValue.checkReturnDates mustBe false
              captor.getValue.loans mustBe None
              captor.getValue.assets mustBe Some(
                Assets(
                  optLandOrProperty = None,
                  optBorrowing = None,
                  optBonds = Some(
                    Bonds(
                      bondsWereAdded = true,
                      bondsWereDisposed = bondsDisposal,
                      bondTransactions = List.empty
                    )
                  )
                )
              )
              captor.getValue.shares mustBe None
              result mustBe Some(())
          }
        }
    )

    "shouldn't submitPsrDetails request when userAnswer is empty" in {

      whenReady(service.submitPsrDetails(srn)) { result: Option[Unit] =>
        verify(mockMinimalRequiredSubmissionTransformer, times(1)).transformToEtmp(any())(any())
        verify(mockLoanTransactionsTransformer, never).transformToEtmp(any())(any())
        verify(mockLandOrPropertyTransactionsTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockMoneyBorrowedTransformer, never).transformToEtmp(any())(any())
        verify(mockSharesTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockMemberPaymentsTransformerTransformer, never).transformToEtmp(any(), any())
        verify(mockBondTransactionsTransformer, never).transformToEtmp(any(), any())(any())
        verify(mockConnector, never).submitPsrDetails(any())(any(), any())
        result mustBe None
      }
    }
  }
}

object PsrSubmissionServiceSpec {
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
}
