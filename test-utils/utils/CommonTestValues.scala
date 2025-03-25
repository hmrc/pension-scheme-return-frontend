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

import play.api.mvc.Call
import cats.data.NonEmptyList
import models.requests.psr._
import play.api.libs.json.{JsValue, Json}
import models.backend.responses._

import java.time.{LocalDate, LocalDateTime}

trait CommonTestValues {
  val commonPstr = "testPstr"
  val commonSrn = "S0000000042"
  val commonStartDate = "2022-04-06"
  val commonEndDate = "2024-04-06"
  val commonVersion = "001"
  val commonFbNumber = "123456785011"
  val commonFeedback = "feedbackUrl"
  val commonFallbackUrl = "fallbackUrl"
  val commonFallbackCall: Call = Call("GET", commonFallbackUrl)
  val commonUserName = "userName"
  val commonSchemeName = "schemeName"

  val minimalSubmissionData: PsrSubmission = PsrSubmission(
    MinimalRequiredSubmission(
      reportDetails = ReportDetails(
        fbVersion = None,
        fbstatus = None,
        pstr = "00000042IN",
        periodStart = LocalDate.parse("2023-04-06"),
        periodEnd = LocalDate.parse("2024-04-05"),
        compilationOrSubmissionDate = None
      ),
      accountingPeriodDetails = AccountingPeriodDetails(
        recordVersion = Some("001"),
        accountingPeriods = NonEmptyList((LocalDate.parse("2023-04-06"), LocalDate.parse("2024-04-05")), List())
      ),
      schemeDesignatory = SchemeDesignatory(
        recordVersion = Some("001"),
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
    shares = None,
    psrDeclaration = None
  )

  val minimalSubmissionJson: JsValue = Json.parse(
    """{
      |  "minimalRequiredSubmission": {
      |    "reportDetails": {
      |      "pstr": "00000042IN",
      |      "periodStart": "2023-04-06",
      |      "periodEnd": "2024-04-05"
      |    },
      |    "accountingPeriodDetails": {
      |      "recordVersion": "001",
      |      "accountingPeriods": [
      |        [
      |          "2023-04-06",
      |          "2024-04-05"
      |        ]
      |      ]
      |    },
      |    "schemeDesignatory": {
      |      "recordVersion": "001",
      |      "openBankAccount": true,
      |      "activeMembers": 23,
      |      "deferredMembers": 45,
      |      "pensionerMembers": 6,
      |      "totalPayments": 74
      |    }
      |  },
      |  "checkReturnDates": true
      |}""".stripMargin
  )
  val versionsResponse: Seq[PsrVersionsResponse] = {
    Seq(
      PsrVersionsResponse(
        startDate = None,
        reportFormBundleNumber = commonFbNumber.replace('1', '2'),
        reportVersion = commonVersion.toInt + 1,
        reportStatus = ReportStatus.SubmittedAndSuccessfullyProcessed,
        compilationOrSubmissionDate = LocalDateTime.parse("2020-04-07T12:00:00.000"),
        reportSubmitterDetails = Some(
          ReportSubmitterDetails(
            reportSubmittedBy = "PSP",
            organisationOrPartnershipDetails = None,
            individualDetails = Some(IndividualDetails("first", None, "last"))
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
      ),
      PsrVersionsResponse(
        startDate = None,
        reportFormBundleNumber = commonFbNumber,
        reportVersion = commonVersion.toInt,
        reportStatus = ReportStatus.SubmittedAndSuccessfullyProcessed,
        compilationOrSubmissionDate = LocalDateTime.parse("2020-04-06T12:00:00.000"),
        reportSubmitterDetails = Some(
          ReportSubmitterDetails(
            reportSubmittedBy = "PSP",
            organisationOrPartnershipDetails = Some(
              OrganisationOrPartnershipDetails(
                "pspOrgName"
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
  val compiledVersionsResponse: PsrVersionsResponse = PsrVersionsResponse(
    startDate = None,
    reportFormBundleNumber = commonFbNumber.replace('1', '3'),
    reportVersion = commonVersion.toInt + 2,
    reportStatus = ReportStatus.ReportStatusCompiled,
    compilationOrSubmissionDate = LocalDateTime.parse("2020-04-08T12:00:00.000"),
    reportSubmitterDetails = None,
    psaDetails = None
  )

  val versionsResponseInProgress: Seq[PsrVersionsResponse] = {
    Seq(compiledVersionsResponse) ++ versionsResponse
  }

  val versionsForYearsResponse: Seq[PsrVersionsForYearsResponse] = {
    Seq(
      PsrVersionsForYearsResponse(
        startDate = "2020-04-06",
        data = versionsResponse
      )
    )
  }

  val versionsForYearsInProgressResponse: Seq[PsrVersionsForYearsResponse] = {
    Seq(
      PsrVersionsForYearsResponse(
        startDate = "2020-04-06",
        data = versionsResponseInProgress
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

  val getVersionsForYearsJson: JsValue = Json.parse(
    """
      |[
      |    {
      |        "startDate": "2020-04-06",
      |        "data": [
      |           {
      |                "reportFormBundleNumber": "223456785022",
      |                "reportVersion": 2,
      |                "reportStatus": "SubmittedAndSuccessfullyProcessed",
      |                "compilationOrSubmissionDate": "2020-04-07T12:00:00",
      |                "reportSubmitterDetails": {
      |                    "reportSubmittedBy": "PSP",
      |                    "individualDetails": {
      |                        "firstName": "first",
      |                        "lastName": "last"
      |                    }
      |                },
      |                "psaDetails": {
      |                    "psaOrganisationOrPartnershipDetails": {
      |                        "organisationOrPartnershipName": "psaOrgName"
      |                    }
      |                }
      |            },
      |            {
      |                "reportFormBundleNumber": "123456785011",
      |                "reportVersion": 1,
      |                "reportStatus": "SubmittedAndSuccessfullyProcessed",
      |                "compilationOrSubmissionDate": "2020-04-06T12:00:00",
      |                "reportSubmitterDetails": {
      |                    "reportSubmittedBy": "PSP",
      |                    "organisationOrPartnershipDetails": {
      |                        "organisationOrPartnershipName": "pspOrgName"
      |                    }
      |                },
      |                "psaDetails": {
      |                    "psaOrganisationOrPartnershipDetails": {
      |                        "organisationOrPartnershipName": "psaOrgName"
      |                    }
      |                }
      |            }
      |        ]
      |    }
      |]
      |""".stripMargin
  )
  val getVersionsForYearsJsonWithInvalidFirstName: JsValue = Json.parse(
    """
      |[
      |    {
      |        "startDate": "2020-04-06",
      |        "data": [
      |           {
      |                "reportFormBundleNumber": "223456785022",
      |                "reportVersion": 2,
      |                "reportStatus": "SubmittedAndSuccessfullyProcessed",
      |                "compilationOrSubmissionDate": "2020-04-07T12:00:00",
      |                "reportSubmitterDetails": {
      |                    "reportSubmittedBy": "PSP",
      |                    "individualDetails": {
      |                        "firstName": 1,
      |                        "lastName": "last"
      |                    }
      |                },
      |                "psaDetails": {
      |                    "psaOrganisationOrPartnershipDetails": {
      |                        "organisationOrPartnershipName": "psaOrgName"
      |                    }
      |                }
      |            },
      |            {
      |                "reportFormBundleNumber": "123456785011",
      |                "reportVersion": 1,
      |                "reportStatus": "SubmittedAndSuccessfullyProcessed",
      |                "compilationOrSubmissionDate": "2020-04-06T12:00:00",
      |                "reportSubmitterDetails": {
      |                    "reportSubmittedBy": "PSP",
      |                    "organisationOrPartnershipDetails": {
      |                        "organisationOrPartnershipName": "pspOrgName"
      |                    }
      |                },
      |                "psaDetails": {
      |                    "psaOrganisationOrPartnershipDetails": {
      |                        "organisationOrPartnershipName": "psaOrgName"
      |                    }
      |                }
      |            }
      |        ]
      |    }
      |]
      |""".stripMargin
  )
  val getVersionsForYears403Json: JsValue = Json.parse(
    """
      |{
      |    "statusCode": 403,
      |    "message": "returned 403. Response body: '{\"failures\":[{\"code\":\"PERIOD_START_DATE_NOT_IN_RANGE\",\"reason\":\"The remote endpoint has indicated that Period Start Date cannot be in the future.\"}]}'"
      |}
      |""".stripMargin
  )

  val getVersions503Json: JsValue = Json.parse(
    """
      |{
      |    "statusCode": 503,
      |    "message": "returned 503"
      |}
      |""".stripMargin
  )
  val getVersionsForYearsNotFoundJson: JsValue = Json.parse(
    """
      |    {
      |        "startDate": "2020-04-06",
      |        "data": []
      |    }
      |""".stripMargin
  )
  val getVersionsJson: JsValue = Json.parse(
    """
      |[
      |{
      |        "reportFormBundleNumber": "223456785022",
      |        "reportVersion": 2,
      |        "reportStatus": "SubmittedAndSuccessfullyProcessed",
      |        "compilationOrSubmissionDate": "2020-04-07T12:00:00",
      |        "reportSubmitterDetails": {
      |            "reportSubmittedBy": "PSP",
      |            "individualDetails": {
      |                "firstName": "first",
      |                "lastName": "last"
      |            }
      |        },
      |        "psaDetails": {
      |            "psaOrganisationOrPartnershipDetails": {
      |                "organisationOrPartnershipName": "psaOrgName"
      |            }
      |        }
      |    },
      |    {
      |        "reportFormBundleNumber": "123456785011",
      |        "reportVersion": 1,
      |        "reportStatus": "SubmittedAndSuccessfullyProcessed",
      |        "compilationOrSubmissionDate": "2020-04-06T12:00:00",
      |        "reportSubmitterDetails": {
      |            "reportSubmittedBy": "PSP",
      |            "organisationOrPartnershipDetails": {
      |                "organisationOrPartnershipName": "pspOrgName"
      |            }
      |        },
      |        "psaDetails": {
      |            "psaOrganisationOrPartnershipDetails": {
      |                "organisationOrPartnershipName": "psaOrgName"
      |            }
      |        }
      |    }
      |]
      |""".stripMargin
  )
}
