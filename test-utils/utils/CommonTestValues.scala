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

package utils

import cats.data.NonEmptyList
import models.requests.psr._
import play.api.libs.json.{JsValue, Json}
import models.backend.responses._

import java.time.{LocalDate, LocalDateTime}

trait CommonTestValues {
  val commonPstr = "testPstr"
  val commonStartDate = "2022-04-06"
  val commonEndDate = "2024-04-06"
  val commonVersion = "001"
  val commonFbNumber = "123456785011"

  val minimalSubmissionData: PsrSubmission = PsrSubmission(
    MinimalRequiredSubmission(
      reportDetails = ReportDetails("00000042IN", LocalDate.parse("2023-04-06"), LocalDate.parse("2024-04-05")),
      accountingPeriods = NonEmptyList((LocalDate.parse("2023-04-06"), LocalDate.parse("2024-04-05")), List()),
      schemeDesignatory = SchemeDesignatory(
        openBankAccount = true,
        reasonForNoBankAccount = None,
        activeMembers = 23,
        deferredMembers = 45,
        pensionerMembers = 6,
        totalAssetValueStart = None,
        totalAssetValueEnd = None,
        totalCashStart = None,
        totalCashEnd = None,
        totalPayments = Some(74.0)
      )
    ),
    checkReturnDates = true,
    loans = None,
    assets = None,
    membersPayments = None,
    shares = None
  )

  val minimalSubmissionJson: JsValue = Json.parse(
    """
      |{
      |  "minimalRequiredSubmission": {
      |    "reportDetails": {
      |      "pstr": "00000042IN",
      |      "periodStart": "2023-04-06",
      |      "periodEnd": "2024-04-05"
      |    },
      |    "accountingPeriods": [
      |      [
      |        "2023-04-06",
      |        "2024-04-05"
      |      ]
      |    ],
      |    "schemeDesignatory": {
      |      "openBankAccount": true,
      |      "activeMembers": 23,
      |      "deferredMembers": 45,
      |      "pensionerMembers": 6,
      |      "totalPayments": 74
      |    }
      |  },
      |  "checkReturnDates": true
      |}
      |
      |""".stripMargin
  )
  val versionsResponse: Seq[PsrVersionsResponse] = {
    Seq(
      PsrVersionsResponse(
        startDate = Some(LocalDate.parse("2020-04-06")),
        reportFormBundleNumber = commonFbNumber,
        reportVersion = commonVersion.toInt,
        reportStatus = ReportStatus.SubmittedAndSuccessfullyProcessed,
        compilationOrSubmissionDate = LocalDateTime.parse("2020-04-06T12:00:00.000"),
        reportSubmitterDetails = Some(
          ReportSubmitterDetails(
            reportSubmittedBy = "PSP",
            organisationOrPartnershipDetails = Some(
              OrganisationOrPartnershipDetails(
                "psaOrgName"
              )
            ),
            individualDetails = None
          )
        ),
        psaDetails = Some(
          PsaDetails(
            psaOrganisationOrPartnershipDetails = Some(
              PsaOrganisationOrPartnershipDetails(
                "psaOrgName"
              )
            ),
            psaIndividualDetails = None
          )
        )
      )
    )
  }
  val versionsForYearsResponse: Seq[PsrVersionsForYearsResponse] = {
    Seq(
      PsrVersionsForYearsResponse(
        startDate = "2020-04-06",
        data = versionsResponse
      )
    )
  }

  val overviewResponse: Seq[OverviewResponse] = {
    Seq(
      OverviewResponse(
        periodStartDate = LocalDate.parse("2022-04-06"),
        periodEndDate = LocalDate.parse("2023-04-05"),
        numberOfVersions = Some(0),
        submittedVersionAvailable = Some(YesNo.No),
        compiledVersionAvailable = Some(YesNo.No),
        ntfDateOfIssue = Some(LocalDate.parse("2022-12-06")),
        psrDueDate = Some(LocalDate.parse("2023-03-31")),
        psrReportType = Some(PsrReportType.Standard),
        tpssReportPresent = None
      ),
      OverviewResponse(
        periodStartDate = LocalDate.parse("2023-04-06"),
        periodEndDate = LocalDate.parse("2024-04-05"),
        numberOfVersions = Some(2),
        submittedVersionAvailable = Some(YesNo.Yes),
        compiledVersionAvailable = Some(YesNo.Yes),
        ntfDateOfIssue = Some(LocalDate.parse("2021-12-06")),
        psrDueDate = Some(LocalDate.parse("2022-03-31")),
        psrReportType = Some(PsrReportType.Sipp),
        tpssReportPresent = None
      )
    )
  }
  val overview400Json: JsValue = Json.parse(
    """
      |{
      |    "statusCode": 400,
      |    "message": "Missing parameter: toDate"
      |}
      |""".stripMargin
  )
  val overviewJson: JsValue = Json.parse(
    """
      |[
      |    {
      |        "periodStartDate": "2022-04-06",
      |        "periodEndDate": "2023-04-05",
      |        "numberOfVersions": 0,
      |        "submittedVersionAvailable": "No",
      |        "compiledVersionAvailable": "No",
      |        "ntfDateOfIssue": "2022-12-06",
      |        "psrDueDate": "2023-03-31",
      |        "psrReportType": "Standard"
      |    },
      |    {
      |        "periodStartDate": "2023-04-06",
      |        "periodEndDate": "2024-04-05",
      |        "numberOfVersions": 2,
      |        "submittedVersionAvailable": "Yes",
      |        "compiledVersionAvailable": "Yes",
      |        "ntfDateOfIssue": "2021-12-06",
      |        "psrDueDate": "2022-03-31",
      |        "psrReportType": "SIPP"
      |    }
      |]
      |""".stripMargin
  )
}