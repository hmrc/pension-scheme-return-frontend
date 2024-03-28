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

import play.api.test.FakeRequest
import models.audit.PSRStartAuditEvent
import utils.BaseSpec
import play.api.mvc.AnyContentAsEmpty
import uk.gov.hmrc.play.audit.model.DataEvent
import controllers.TestValues
import config.FrontendAppConfig
import config.Constants.{PSA, PSP}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class AuditServiceSpec extends BaseSpec with TestValues {

  private val mockConfig = mock[FrontendAppConfig]
  private val mockAuditConnector = mock[AuditConnector]

  implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

  val service = new AuditService(mockConfig, mockAuditConnector)

  private val testAppName = "test-app-name"

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockConfig.appName).thenReturn(testAppName)
    reset(mockAuditConnector)
  }

  "AuditService" - {
    "PSRStartAuditEvent for PSA" in {

      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      when(mockAuditConnector.sendEvent(captor.capture())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val auditEvent = PSRStartAuditEvent(
        schemeName,
        "testAdminName",
        psaId.value,
        pstr,
        "testAffinity",
        PSA,
        dateRange,
        1,
        2,
        3
      )

      service.sendEvent(auditEvent).futureValue

      val dataEvent = captor.getValue
      val expectedDataEvent = Map(
        "SchemeName" -> schemeName,
        "SchemeAdministratorName" -> "testAdminName",
        "PensionSchemeAdministratorId" -> psaId.value,
        "PensionSchemeTaxReference" -> pstr,
        "AffinityGroup" -> "testAffinity",
        "CredentialRole(PSA/PSP)" -> PSA,
        "TaxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "HowManyMembers" -> "1",
        "HowManyDeferredMembers" -> "2",
        "HowManyPensionerMembers" -> "3"
      )

      dataEvent.auditSource mustEqual testAppName
      dataEvent.auditType mustEqual "PensionSchemeReturnStarted"
      dataEvent.detail mustEqual expectedDataEvent
    }

    "PSRStartAuditEvent for PSP" in {

      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      when(mockAuditConnector.sendEvent(captor.capture())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val auditEvent = PSRStartAuditEvent(
        schemeName,
        "testAdminName",
        pspId.value,
        pstr,
        "testAffinity",
        PSP,
        dateRange,
        1,
        2,
        3
      )

      service.sendEvent(auditEvent).futureValue

      val dataEvent = captor.getValue
      val expectedDataEvent = Map(
        "SchemeName" -> schemeName,
        "SchemePractitionerName" -> "testAdminName",
        "PensionSchemePractitionerId" -> pspId.value,
        "PensionSchemeTaxReference" -> pstr,
        "AffinityGroup" -> "testAffinity",
        "CredentialRole(PSA/PSP)" -> PSP,
        "TaxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "HowManyMembers" -> "1",
        "HowManyDeferredMembers" -> "2",
        "HowManyPensionerMembers" -> "3"
      )

      dataEvent.auditSource mustEqual testAppName
      dataEvent.auditType mustEqual "PensionSchemeReturnStarted"
      dataEvent.detail mustEqual expectedDataEvent
    }
  }
}
