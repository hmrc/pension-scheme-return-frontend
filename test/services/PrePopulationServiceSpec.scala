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

import utils.BaseSpec
import services.PrePopulationServiceSpec._
import models.backend.responses.{PsrVersionsForYearsResponse, PsrVersionsResponse, ReportStatus}

import java.time.{LocalDate, LocalDateTime}

class PrePopulationServiceSpec extends BaseSpec {

  private val service = new PrePopulationService()

  "PrePopulationService" - {

    "should return None when versionsForYears is empty" in {

      val result = service.findLastSubmittedPsrFbInPreviousYears(
        versionsForYears = Seq.empty,
        yearFrom = LocalDate.of(2023, 4, 6)
      )
      result mustBe None
    }

    "should return None when versionsForYears not contains versions before the start date" in {

      val result = service.findLastSubmittedPsrFbInPreviousYears(
        versionsForYears = currentAndFutureYears,
        yearFrom = LocalDate.of(2023, 4, 6)
      )
      result mustBe None
    }

    "should return None when versionsForYears not contains any submitted in previous years" in {

      val result = service.findLastSubmittedPsrFbInPreviousYears(
        versionsForYears = containsPreviousYearsButNotSubmitted,
        yearFrom = LocalDate.of(2023, 4, 6)
      )
      result mustBe None
    }

    "should find the latest submitted Psr Fb In Previous Years when versionsForYears contains at least one" in {

      val result = service.findLastSubmittedPsrFbInPreviousYears(
        versionsForYears = versionsForYears,
        yearFrom = LocalDate.of(2023, 4, 6)
      )
      result mustBe Some("4")
    }
  }
}

object PrePopulationServiceSpec {

  val currentAndFutureYears: Seq[PsrVersionsForYearsResponse] = Seq(
    PsrVersionsForYearsResponse(
      startDate = "2023-04-06",
      data = Seq(
        PsrVersionsResponse(
          startDate = None,
          reportFormBundleNumber = "7",
          reportVersion = 1,
          reportStatus = ReportStatus.SubmittedAndSuccessfullyProcessed,
          compilationOrSubmissionDate = LocalDateTime.parse("2020-04-08T12:00:00.000"),
          reportSubmitterDetails = None,
          psaDetails = None
        )
      )
    ),
    PsrVersionsForYearsResponse(
      startDate = "2024-04-06",
      data = Seq(
        PsrVersionsResponse(
          startDate = None,
          reportFormBundleNumber = "1",
          reportVersion = 1,
          reportStatus = ReportStatus.SubmittedAndSuccessfullyProcessed,
          compilationOrSubmissionDate = LocalDateTime.parse("2020-04-08T12:00:00.000"),
          reportSubmitterDetails = None,
          psaDetails = None
        )
      )
    )
  )

  val containsPreviousYearsButNotSubmitted: Seq[PsrVersionsForYearsResponse] = Seq(
    PsrVersionsForYearsResponse(
      startDate = "2022-04-06",
      data = Seq(
        PsrVersionsResponse(
          startDate = None,
          reportFormBundleNumber = "6",
          reportVersion = 1,
          reportStatus = ReportStatus.ReportStatusCompiled,
          compilationOrSubmissionDate = LocalDateTime.parse("2020-04-08T12:00:00.000"),
          reportSubmitterDetails = None,
          psaDetails = None
        )
      )
    )
  ) ++ currentAndFutureYears

  val versionsForYears: Seq[PsrVersionsForYearsResponse] = {
    containsPreviousYearsButNotSubmitted ++
      Seq(
        PsrVersionsForYearsResponse(
          startDate = "2020-04-06",
          data = Seq(
            PsrVersionsResponse(
              startDate = None,
              reportFormBundleNumber = "2",
              reportVersion = 2,
              reportStatus = ReportStatus.SubmittedAndSuccessfullyProcessed,
              compilationOrSubmissionDate = LocalDateTime.parse("2020-04-08T12:00:00.000"),
              reportSubmitterDetails = None,
              psaDetails = None
            )
          )
        ),
        PsrVersionsForYearsResponse(
          startDate = "2021-04-06",
          data = Seq(
            PsrVersionsResponse(
              startDate = None,
              reportFormBundleNumber = "3",
              reportVersion = 1,
              reportStatus = ReportStatus.ReportStatusCompiled,
              compilationOrSubmissionDate = LocalDateTime.parse("2020-04-08T12:00:00.000"),
              reportSubmitterDetails = None,
              psaDetails = None
            ),
            PsrVersionsResponse(
              startDate = None,
              reportFormBundleNumber = "4",
              reportVersion = 2,
              reportStatus = ReportStatus.SubmittedAndSuccessfullyProcessed,
              compilationOrSubmissionDate = LocalDateTime.parse("2020-04-08T12:00:00.000"),
              reportSubmitterDetails = None,
              psaDetails = None
            ),
            PsrVersionsResponse(
              startDate = None,
              reportFormBundleNumber = "5",
              reportVersion = 3,
              reportStatus = ReportStatus.ReportStatusCompiled,
              compilationOrSubmissionDate = LocalDateTime.parse("2020-04-08T12:00:00.000"),
              reportSubmitterDetails = None,
              psaDetails = None
            )
          )
        )
      )
  }
}
