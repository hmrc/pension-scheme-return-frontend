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

package controllers.nonsipp

import services.SaveService
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import utils.DateTimeUtils
import viewmodels.implicits._
import play.api.mvc.Call
import controllers.{ControllerBaseSpec, ControllerBehaviours, TestUserAnswers}
import views.html.YesNoPageView
import pages.nonsipp.{BasicDetailsCompletedPage, CheckReturnDatesPage}
import forms.YesNoPageFormProvider
import models.{DateRange, NormalMode}
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models.SectionCompleted

class CheckReturnDatesControllerSpec
    extends ControllerBaseSpec
    with ControllerBehaviours
    with ScalaCheckPropertyChecks
    with TestUserAnswers { self =>

  private implicit val mockSaveService: SaveService = mock[SaveService]

  override protected def beforeEach(): Unit =
    MockSaveService.save()

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val checkReturnDatesRoute: String = routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
  lazy val onPageLoad: Call = routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)
  lazy val onSubmit: Call = routes.CheckReturnDatesController.onSubmit(srn, NormalMode)

  "onPageLoad" - {
    val DateRange(fromDate, toDate) = currentReturnTaxYear

    act.like(
      renderView(onPageLoad, currentTaxYearUserAnswers) { implicit app => implicit request =>
        injected[YesNoPageView].apply(
          CheckReturnDatesController.form(new YesNoPageFormProvider()),
          CheckReturnDatesController.viewModel(
            srn,
            NormalMode,
            fromDate,
            toDate
          )
        )
      }
    )

    act.like(
      redirectNextPage(
        onSubmit,
        currentTaxYearUserAnswers,
        "value" -> "true"
      )
    )

    act.like(
      redirectNextPage(
        onSubmit,
        currentTaxYearUserAnswers
          .unsafeSet(CheckReturnDatesPage(srn), false),
        "value" -> "true"
      ).withName("redirect to next page when change from false to true")
    )

    act.like(
      redirectToPage(
        onSubmit,
        controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode),
        currentTaxYearUserAnswersWithFewMembers
          .unsafeSet(CheckReturnDatesPage(srn), true)
          .unsafeSet(BasicDetailsCompletedPage(srn), SectionCompleted),
        "value" -> "true"
      )
    )
  }

  "CheckReturnDates.viewModel" - {

    "contain correct title key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates)
        viewModel.title mustBe Message("checkReturnDates.title")
      }
    }

    "contain correct heading key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates)
        viewModel.heading mustBe Message("checkReturnDates.heading")
      }
    }

    "contain from date" in {

      forAll(date, date) { (fromDate, toDate) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, toDate)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe Some(
          ParagraphMessage(Message("checkReturnDates.description", formattedFromDate, formattedToDate))
        )
      }
    }

    "contain to date" in {

      forAll(date, date) { (fromDate, toDate) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, toDate)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe Some(
          ParagraphMessage(Message("checkReturnDates.description", formattedFromDate, formattedToDate))
        )
      }
    }

    "contain correct legend key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates)
        viewModel.page.legend.value mustBe Message("checkReturnDates.legend")
      }
    }

    "populate the onSubmit with srn and mode" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates)
        viewModel.onSubmit mustBe routes.CheckReturnDatesController.onSubmit(srn, mode)
      }
    }
  }
}
