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
import models.audit.{PSRCompileAuditEvent, PSRStartAuditEvent}
import play.api.mvc.AnyContentAsEmpty
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}
import viewmodels.models.TaskListCipViewModel.writeListTaskListLevel1
import controllers.TestValues
import config.FrontendAppConfig
import config.Constants.{PSA, PSP}
import play.api.libs.json.Json
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import utils.BaseSpec
import org.mockito.Mockito.{reset, when}

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
    "sendEvent for PSA" in {

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
        "schemeName" -> schemeName,
        "schemeAdministratorName" -> "testAdminName",
        "pensionSchemeAdministratorId" -> psaId.value,
        "pensionSchemeTaxReference" -> pstr,
        "affinityGroup" -> "testAffinity",
        "credentialRole(PSA/PSP)" -> PSA,
        "taxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "howManyMembers" -> "1",
        "howManyDeferredMembers" -> "2",
        "howManyPensionerMembers" -> "3"
      )

      dataEvent.auditSource mustEqual testAppName
      dataEvent.auditType mustEqual "PensionSchemeReturnStarted"
      dataEvent.detail mustEqual expectedDataEvent
    }

    "sendEvent for PSP" in {

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
        "schemeName" -> schemeName,
        "schemePractitionerName" -> "testAdminName",
        "pensionSchemePractitionerId" -> pspId.value,
        "pensionSchemeTaxReference" -> pstr,
        "affinityGroup" -> "testAffinity",
        "credentialRole(PSA/PSP)" -> PSP,
        "taxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "howManyMembers" -> "1",
        "howManyDeferredMembers" -> "2",
        "howManyPensionerMembers" -> "3"
      )

      dataEvent.auditSource mustEqual testAppName
      dataEvent.auditType mustEqual "PensionSchemeReturnStarted"
      dataEvent.detail mustEqual expectedDataEvent
    }

    "sendExtendedEvent for PSA" in {

      val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      when(mockAuditConnector.sendExtendedEvent(captor.capture())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val auditEvent = PSRCompileAuditEvent(
        schemeName,
        "testAdminName",
        psaId.value,
        pstr,
        "testAffinity",
        PSA,
        dateRange,
        taskList = Json.toJson(taskListInAuditEvent)
      )

      service.sendExtendedEvent(auditEvent).futureValue

      val extendedDataEvent = captor.getValue
      val expectedExtendedDataEvent = Json.obj(
        "schemeName" -> schemeName,
        "schemeAdministratorName" -> "testAdminName",
        "pensionSchemeAdministratorId" -> psaId.value,
        "pensionSchemeTaxReference" -> pstr,
        "affinityGroup" -> "testAffinity",
        "credentialRole(PSA/PSP)" -> PSA,
        "taxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "sections" -> Json.toJson(taskListInAuditEvent)
      )

      extendedDataEvent.auditSource mustEqual testAppName
      extendedDataEvent.auditType mustEqual "PensionSchemeReturnCompiled"
      extendedDataEvent.detail mustEqual expectedExtendedDataEvent
    }

    "sendExtendedEvent for PSP" in {

      val captor: ArgumentCaptor[ExtendedDataEvent] = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])

      when(mockAuditConnector.sendExtendedEvent(captor.capture())(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val auditEvent = PSRCompileAuditEvent(
        schemeName,
        "testAdminName",
        pspId.value,
        pstr,
        "testAffinity",
        PSP,
        dateRange,
        taskList = Json.toJson(taskListInAuditEvent)
      )

      service.sendExtendedEvent(auditEvent).futureValue

      val extendedDataEvent = captor.getValue
      val expectedExtendedDataEvent = Json.obj(
        "schemeName" -> schemeName,
        "schemePractitionerName" -> "testAdminName",
        "pensionSchemePractitionerId" -> pspId.value,
        "pensionSchemeTaxReference" -> pstr,
        "affinityGroup" -> "testAffinity",
        "credentialRole(PSA/PSP)" -> PSP,
        "taxYear" -> s"${dateRange.from.getYear}-${dateRange.to.getYear}",
        "sections" -> Json.toJson(taskListInAuditEvent)
      )

      extendedDataEvent.auditSource mustEqual testAppName
      extendedDataEvent.auditType mustEqual "PensionSchemeReturnCompiled"
      extendedDataEvent.detail mustEqual expectedExtendedDataEvent
    }
  }
}
