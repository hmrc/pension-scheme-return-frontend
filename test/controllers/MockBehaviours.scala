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

package controllers

import services.{PsrSubmissionService, SaveService, SchemeDateService}
import org.mockito.verification.VerificationMode
import org.mockito.Mockito.{verify, when}
import connectors.{EmailConnector, EmailSent, EmailStatus}
import config.RefinedTypes.Max3
import cats.data.NonEmptyList
import org.mockito.stubbing.OngoingStubbing
import models.{DateRange, UserAnswers}
import org.mockito.{ArgumentCaptor, ArgumentMatchers, Mockito}
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future

trait MockBehaviours {

  object MockSaveService {
    def capture(captor: ArgumentCaptor[UserAnswers])(implicit mock: SaveService): Future[Unit] =
      verify(mock).save(captor.capture())(using any(), any())

    def save()(implicit mock: SaveService): OngoingStubbing[Future[Unit]] =
      when(mock.save(any())(using any(), any())).thenReturn(Future.successful(()))
  }

  object MockSchemeDateService {
    def taxYearOrAccountingPeriods(
      returns: Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]
    )(implicit
      mock: SchemeDateService
    ): OngoingStubbing[Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]] =
      when(mock.taxYearOrAccountingPeriods(any())(using any())).thenReturn(returns)

    def returnPeriods(
      returns: Option[NonEmptyList[DateRange]]
    )(implicit mock: SchemeDateService): OngoingStubbing[Option[NonEmptyList[DateRange]]] =
      when(mock.returnPeriods(any())(using any())).thenReturn(returns)
  }

  object MockPsrSubmissionService {

    def submitPsrDetails()(implicit mock: PsrSubmissionService): OngoingStubbing[Future[Option[Unit]]] =
      when(mock.submitPsrDetails(any(), any(), any())(using any(), any(), any()))
        .thenReturn(Future.successful(Some(())))

    def submitPsrDetailsWithUA()(implicit mock: PsrSubmissionService): OngoingStubbing[Future[Option[Unit]]] =
      when(mock.submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any()))
        .thenReturn(Future.successful(Some(())))

    def submitPsrDetailsBypassed()(implicit mock: PsrSubmissionService): OngoingStubbing[Future[Option[Unit]]] =
      when(mock.submitPsrDetailsBypassed(any(), any())(using any(), any(), any()))
        .thenReturn(Future.successful(Some(())))

    object verify {
      def submitPsrDetails(v: VerificationMode)(implicit mock: PsrSubmissionService): Future[Option[Unit]] =
        Mockito.verify(mock, v).submitPsrDetails(any(), any(), any())(using any(), any(), any())

      def submitPsrDetailsWithUA(v: VerificationMode)(implicit mock: PsrSubmissionService): Future[Option[Unit]] =
        Mockito.verify(mock, v).submitPsrDetailsWithUA(any(), any(), any())(using any(), any(), any())
    }
  }

  object MockEmailConnector {
    def sendEmail(testEmailAddress: String, testTemplateId: String)(implicit
      mockEmailConnector: EmailConnector
    ): OngoingStubbing[Future[EmailStatus]] =
      when(
        mockEmailConnector.sendEmail(
          psaOrPsp = any(),
          requestId = any(),
          psaOrPspId = any(),
          pstr = any(),
          emailAddress = ArgumentMatchers.eq(testEmailAddress),
          templateId = ArgumentMatchers.eq(testTemplateId),
          templateParams = any(),
          reportVersion = any(),
          schemeName = any(),
          taxYear = any(),
          userName = any()
        )(using any(), any())
      ).thenReturn(Future.successful(EmailSent))

  }
}
