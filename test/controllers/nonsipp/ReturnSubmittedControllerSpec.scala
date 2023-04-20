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

package controllers.nonsipp

import cats.data.NonEmptyList
import config.Refined.OneToThree
import controllers.ControllerBaseSpec
import controllers.nonsipp.ReturnSubmittedController.viewModel
import eu.timepit.refined._
import models.DateRange
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.accountingperiod.AccountingPeriodPage
import pages.nonsipp.{ReturnSubmittedPage, WhichTaxYearPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import viewmodels.models.SubmissionViewModel
import views.html.SubmissionView

import java.time.LocalDateTime

class ReturnSubmittedControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.ReturnSubmittedController.onPageLoad(srn)

  private val returnPeriod1 = dateRangeGen.sample.value
  private val returnPeriod2 = dateRangeGen.sample.value
  private val returnPeriod3 = dateRangeGen.sample.value
  private val submissionDateTime = localDateTimeGen.sample.value

  private val pensionSchemeEnquiriesUrl =
    "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/pension-scheme-enquiries"
  private val mpsDashboardUrl = "http://localhost:8204/manage-pension-schemes/overview"

  private val mockDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(
      bind[SchemeDateService].toInstance(mockDateService)
    )

  override def beforeEach(): Unit = {
    reset(mockDateService)
    when(mockDateService.now()).thenReturn(submissionDateTime)
  }

  "ReturnSubmittedController" - {

    List(
      ("tax year return period", NonEmptyList.one(returnPeriod1)),
      ("single accounting period", NonEmptyList.one(returnPeriod1)),
      ("multiple accounting periods", NonEmptyList.of(returnPeriod1, returnPeriod2, returnPeriod3))
    ).foreach {
      case (testName, returnPeriods) =>
        s"on arriving to page but tax year or accounting periods are not set - $testName" - {
          act.like(
            redirectToPage(onPageLoad, controllers.routes.JourneyRecoveryController.onPageLoad())
              .before(setReturnPeriods(None))
          )
        }

        s"on arriving to page for the first time - $testName" - {
          act.like(renderView(onPageLoad) { implicit app => implicit request =>
            injected[SubmissionView].apply(buildViewModel(returnPeriods, submissionDateTime))
          }.before(setReturnPeriods(Some(returnPeriods))))
        }

        s"on return to page, showing initial submission date - $testName" - {
          val initialSubmissionDateTime = localDateTimeGen.sample.value
          act.like(renderPrePopView(onPageLoad, ReturnSubmittedPage(srn), initialSubmissionDateTime) {
            implicit app => implicit request =>
              injected[SubmissionView].apply(buildViewModel(returnPeriods, initialSubmissionDateTime))
          }.before(setReturnPeriods(Some(returnPeriods))))
        }
    }

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))
  }

  private def buildViewModel(
    returnPeriods: NonEmptyList[DateRange],
    submissionDate: LocalDateTime
  ): SubmissionViewModel =
    viewModel(
      schemeName,
      email,
      returnPeriods,
      submissionDate,
      pensionSchemeEnquiriesUrl,
      mpsDashboardUrl
    )

  private def setReturnPeriods(periods: Option[NonEmptyList[DateRange]]) =
    when(mockDateService.returnPeriods(any())(any())).thenReturn(periods)
}
