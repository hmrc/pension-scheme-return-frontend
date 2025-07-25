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

import services.PrePopulationServiceSpec._
import controllers.TestValues
import config.FrontendAppConfig
import prepop._
import utils.UserAnswersUtils.UserAnswersOps
import play.api.libs.json._
import models.backend.responses.{PsrVersionsForYearsResponse, PsrVersionsResponse, ReportStatus}
import models.UserAnswers
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import utils.BaseSpec
import org.mockito.Mockito._

import scala.util.Success

import java.time.{LocalDate, LocalDateTime}

class PrePopulationServiceSpec extends BaseSpec with TestValues {

  lazy val mockLandOrPropertyPrePopulationProcessor: LandOrPropertyPrePopulationProcessor =
    mock[LandOrPropertyPrePopulationProcessor]
  lazy val mockMemberPrePopulationProcessor: MemberPrePopulationProcessor =
    mock[MemberPrePopulationProcessor]
  lazy val mockSharesPrePopulationProcessor: SharesPrePopulationProcessor =
    mock[SharesPrePopulationProcessor]
  lazy val mockLoansPrePopulationProcessor: LoansPrePopulationProcessor =
    mock[LoansPrePopulationProcessor]
  lazy val mockBondsPrePopulationProcessor: BondsPrePopulationProcessor =
    mock[BondsPrePopulationProcessor]
  lazy val mockLoansProgressPrePopulationProcessor: LoansProgressPrePopulationProcessor =
    mock[LoansProgressPrePopulationProcessor]
  lazy val mockOtherAssetsPrePopulationProcessor: OtherAssetsPrePopulationProcessor =
    mock[OtherAssetsPrePopulationProcessor]
  lazy val mockSharesProgressPrePopulationProcessor: SharesProgressPrePopulationProcessor =
    mock[SharesProgressPrePopulationProcessor]
  lazy val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]

  private val service = new PrePopulationService(
    mockLandOrPropertyPrePopulationProcessor,
    mockMemberPrePopulationProcessor,
    mockSharesPrePopulationProcessor,
    mockLoansPrePopulationProcessor,
    mockBondsPrePopulationProcessor,
    mockLoansProgressPrePopulationProcessor,
    mockOtherAssetsPrePopulationProcessor,
    mockSharesProgressPrePopulationProcessor,
    mockConfig
  )

  override def beforeEach(): Unit = {
    reset(mockLandOrPropertyPrePopulationProcessor)
    reset(mockMemberPrePopulationProcessor)
    reset(mockSharesPrePopulationProcessor)
    reset(mockLoansPrePopulationProcessor)
    reset(mockBondsPrePopulationProcessor)
    reset(mockLoansProgressPrePopulationProcessor)
    reset(mockOtherAssetsPrePopulationProcessor)
    reset(mockSharesProgressPrePopulationProcessor)
    reset(mockConfig)
  }

  "PrePopulationService" - {

    "findLastSubmittedPsrFbInPreviousYears" - {

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
        when(mockConfig.prePopulationEnabled).thenReturn(true)
        val result = service.findLastSubmittedPsrFbInPreviousYears(
          versionsForYears = versionsForYears,
          yearFrom = LocalDate.of(2023, 4, 6)
        )
        result mustBe Some("4")
      }

      "should not find the latest submitted Psr Fb In Previous Years when versionsForYears contains at least one but pre-population is disabled" in {
        when(mockConfig.prePopulationEnabled).thenReturn(false)
        val result = service.findLastSubmittedPsrFbInPreviousYears(
          versionsForYears = versionsForYears,
          yearFrom = LocalDate.of(2023, 4, 6)
        )
        result mustBe None
      }
    }

    "buildPrePopulatedUserAnswers" - {
      "when pre population is enabled" - {
        "should build prePopulated data by calling each journey processor" in {
          val (baseReturnUA: UserAnswers, currentUa: UserAnswers) = setUpData(prePopulationEnabled = true)

          val result = service.buildPrePopulatedUserAnswers(baseReturnUA, currentUa)(srn)
          result.isSuccess mustBe true
          result.get.data.decryptedValue mustBe Json
            .parse("""
              |{
              |  "current": "dummy-current-data",
              |  "lop": "dummy-lop-data",
              |  "member": "dummy-member-data",
              |  "shares": "dummy-shares-data",
              |  "loans": "dummy-loans-data",
              |  "bonds": "dummy-bonds-data",
              |  "loansProgress": "dummy-loans-progress-data",
              |  "otherAssets": "dummy-other-assets-data",
              |  "sharesProgress": "dummy-shares-progress-data"
              |}
              |""".stripMargin)
            .as[JsObject]

          verify(mockLandOrPropertyPrePopulationProcessor, times(1)).clean(any(), any())(any())
          verify(mockMemberPrePopulationProcessor, times(1)).clean(any(), any())(any())
          verify(mockSharesPrePopulationProcessor, times(1)).clean(any(), any())(any())
          verify(mockLoansPrePopulationProcessor, times(1)).clean(any(), any())(any())
          verify(mockBondsPrePopulationProcessor, times(1)).clean(any(), any())(any())
          verify(mockLoansProgressPrePopulationProcessor, times(1)).clean(any(), any())
          verify(mockOtherAssetsPrePopulationProcessor, times(1)).clean(any(), any())(any())
          verify(mockSharesProgressPrePopulationProcessor, times(1)).clean(any(), any())
        }
      }
      "when pre population is disabled" - {
        "should not build prePopulated data by calling each journey processor" in {
          val (baseReturnUA: UserAnswers, currentUa: UserAnswers) = setUpData(prePopulationEnabled = false)

          val result = service.buildPrePopulatedUserAnswers(baseReturnUA, currentUa)(srn)
          result.isSuccess mustBe true
          result.get.data.decryptedValue mustBe Json
            .parse("""
              |{
              |  "current": "dummy-current-data"
              |}
              |""".stripMargin)
            .as[JsObject]

          verify(mockLandOrPropertyPrePopulationProcessor, never()).clean(any(), any())(any())
          verify(mockMemberPrePopulationProcessor, never()).clean(any(), any())(any())
          verify(mockSharesPrePopulationProcessor, never()).clean(any(), any())(any())
          verify(mockLoansPrePopulationProcessor, never()).clean(any(), any())(any())
          verify(mockBondsPrePopulationProcessor, never()).clean(any(), any())(any())
          verify(mockLoansProgressPrePopulationProcessor, never()).clean(any(), any())
          verify(mockOtherAssetsPrePopulationProcessor, never()).clean(any(), any())(any())
          verify(mockSharesProgressPrePopulationProcessor, never()).clean(any(), any())
        }
      }
    }
  }

  private def setUpData(prePopulationEnabled: Boolean) = {
    val baseReturnUA = emptyUserAnswers
    val currentUa = emptyUserAnswers.unsafeSet(__ \ "current", JsString("dummy-current-data"))
    val lopUa = currentUa.unsafeSet(__ \ "lop", JsString("dummy-lop-data"))
    val memberUa = lopUa.unsafeSet(__ \ "member", JsString("dummy-member-data"))
    val sharesUa = memberUa.unsafeSet(__ \ "shares", JsString("dummy-shares-data"))
    val loansUa = sharesUa.unsafeSet(__ \ "loans", JsString("dummy-loans-data"))
    val bondsUa = loansUa.unsafeSet(__ \ "bonds", JsString("dummy-bonds-data"))
    val loansProgressUa = bondsUa.unsafeSet(__ \ "loansProgress", JsString("dummy-loans-progress-data"))
    val otherAssetsUa = loansProgressUa.unsafeSet(__ \ "otherAssets", JsString("dummy-other-assets-data"))
    val sharesProgressUa = otherAssetsUa.unsafeSet(__ \ "sharesProgress", JsString("dummy-shares-progress-data"))

    when(
      mockLandOrPropertyPrePopulationProcessor
        .clean(ArgumentMatchers.eq(baseReturnUA), ArgumentMatchers.eq(currentUa))(ArgumentMatchers.eq(srn))
    ).thenReturn(Success(lopUa))
    when(
      mockMemberPrePopulationProcessor
        .clean(ArgumentMatchers.eq(baseReturnUA), ArgumentMatchers.eq(lopUa))(ArgumentMatchers.eq(srn))
    ).thenReturn(Success(memberUa))
    when(
      mockSharesPrePopulationProcessor
        .clean(ArgumentMatchers.eq(baseReturnUA), ArgumentMatchers.eq(memberUa))(ArgumentMatchers.eq(srn))
    ).thenReturn(Success(sharesUa))
    when(
      mockLoansPrePopulationProcessor
        .clean(ArgumentMatchers.eq(baseReturnUA), ArgumentMatchers.eq(sharesUa))(ArgumentMatchers.eq(srn))
    ).thenReturn(Success(loansUa))
    when(
      mockBondsPrePopulationProcessor
        .clean(ArgumentMatchers.eq(baseReturnUA), ArgumentMatchers.eq(loansUa))(ArgumentMatchers.eq(srn))
    ).thenReturn(Success(bondsUa))
    when(
      mockLoansProgressPrePopulationProcessor
        .clean(ArgumentMatchers.eq(baseReturnUA), ArgumentMatchers.eq(bondsUa))
    ).thenReturn(Success(loansProgressUa))
    when(
      mockOtherAssetsPrePopulationProcessor
        .clean(ArgumentMatchers.eq(baseReturnUA), ArgumentMatchers.eq(loansProgressUa))(ArgumentMatchers.eq(srn))
    ).thenReturn(Success(otherAssetsUa))
    when(
      mockSharesProgressPrePopulationProcessor
        .clean(ArgumentMatchers.eq(baseReturnUA), ArgumentMatchers.eq(otherAssetsUa))
    ).thenReturn(Success(sharesProgressUa))
    when(mockConfig.prePopulationEnabled).thenReturn(prePopulationEnabled)
    (baseReturnUA, currentUa)
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

  val versionsForYears: Seq[PsrVersionsForYearsResponse] =
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
