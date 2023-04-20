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

import controllers.ControllerBaseSpec
import forms.YesNoPageFormProvider
import models.{MinimalSchemeDetails, NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.ScalaOngoingStubbing
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import pages.nonsipp.CheckReturnDatesPage
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import play.api.mvc.Call
import play.api.test.FakeRequest
import services.{FakeTaxYearService, SaveService, SchemeDateService, SchemeDetailsService, TaxYearService}
import uk.gov.hmrc.time.TaxYear
import utils.DateTimeUtils
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.YesNoPageViewModel
import views.html.YesNoPageView

import scala.concurrent.Future

class CheckReturnDatesControllerSpec extends ControllerBaseSpec with ScalaCheckPropertyChecks { self =>

  private val mockSchemeDetailsService = mock[SchemeDetailsService]
  private val taxYear = TaxYear(date.sample.value.getYear)

  override val additionalBindings: List[GuiceableModule] =
    List(
      bind[SchemeDetailsService].toInstance(mockSchemeDetailsService),
      bind[TaxYearService].toInstance(new FakeTaxYearService(taxYear.starts))
    )

  def onwardRoute = Call("GET", "/foo")

  lazy val checkReturnDatesRoute = routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
  lazy val onPageLoad = routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)
  lazy val onSubmit = routes.CheckReturnDatesController.onSubmit(srn, NormalMode)

  "CheckReturnDates.viewModel" - {

    val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value

    "contain correct title key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.title mustBe Message("checkReturnDates.title")
      }
    }

    "contain correct heading key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.heading mustBe Message("checkReturnDates.heading")
      }
    }

    "contain from date when it is after open date" in {

      val updatedDetails = minimalSchemeDetails.copy(openDate = Some(earliestDate), windUpDate = None)

      forAll(date, date) { (fromDate, toDate) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, toDate, updatedDetails)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe List(
          ParagraphMessage(Message("checkReturnDates.description", formattedFromDate, formattedToDate))
        )
      }
    }

    "contain open date when it is after from date" in {

      val detailsGen = minimalSchemeDetailsGen.map(_.copy(windUpDate = None))

      forAll(detailsGen, date) { (details, toDate) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, earliestDate, toDate, details)
        val formattedFromDate = DateTimeUtils.formatHtml(details.openDate.getOrElse(earliestDate))
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe List(
          ParagraphMessage(Message("checkReturnDates.description", formattedFromDate, formattedToDate))
        )
      }
    }

    "contain to date when it is before wind up date" in {

      val updatedDetails = minimalSchemeDetails.copy(openDate = None, windUpDate = Some(latestDate))

      forAll(date, date) { (fromDate, toDate) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, toDate, updatedDetails)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(toDate)

        viewModel.description mustBe List(
          ParagraphMessage(Message("checkReturnDates.description", formattedFromDate, formattedToDate))
        )
      }
    }

    "contain wind up date when it is before to date" in {

      val detailsGen = minimalSchemeDetailsGen.map(_.copy(openDate = None))

      forAll(detailsGen, date) { (details, fromDate) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, NormalMode, fromDate, latestDate, details)
        val formattedFromDate = DateTimeUtils.formatHtml(fromDate)
        val formattedToDate = DateTimeUtils.formatHtml(details.windUpDate.getOrElse(latestDate))

        viewModel.description mustBe List(
          ParagraphMessage(Message("checkReturnDates.description", formattedFromDate, formattedToDate))
        )
      }
    }

    "contain correct legend key" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.legend.value mustBe Message("checkReturnDates.legend")
      }
    }

    "populate the onSubmit with srn and mode" in {

      forAll(srnGen, modeGen, date) { (srn, mode, dates) =>
        val viewModel = CheckReturnDatesController.viewModel(srn, mode, dates, dates, minimalSchemeDetails)
        viewModel.onSubmit mustBe routes.CheckReturnDatesController.onSubmit(srn, mode)
      }
    }
  }

  "CheckReturnDates Controller" - {

    val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value
    lazy val viewModel: YesNoPageViewModel =
      CheckReturnDatesController.viewModel(
        srn,
        NormalMode,
        taxYear.starts,
        taxYear.finishes,
        minimalSchemeDetails
      )

    val form = CheckReturnDatesController.form(new YesNoPageFormProvider())

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[YesNoPageView]
      view(form, viewModel)
    }.before(setSchemeDetails(Some(minimalSchemeDetails))))

    act.like(renderPrePopView(onPageLoad, CheckReturnDatesPage(srn), true) { implicit app => implicit request =>
      val view = injected[YesNoPageView]
      view(form.fill(true), viewModel)
    }.before(setSchemeDetails(Some(minimalSchemeDetails))))

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(setSchemeDetails(Some(minimalSchemeDetails)))
        .updateName("onPageLoad" + _)
    )

    act.like(
      journeyRecoveryPage(onPageLoad)
        .before(setSchemeDetails(None))
        .updateName(_ => "onPageLoad redirect to journey recovery page when scheme date not found")
    )

    act.like(saveAndContinue(onSubmit, formData(form, true): _*).before(setSchemeDetails(Some(minimalSchemeDetails))))

    act.like(invalidForm(onSubmit).before(setSchemeDetails(Some(minimalSchemeDetails))))

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(setSchemeDetails(Some(minimalSchemeDetails)))
        .updateName("onSubmit" + _)
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(setSchemeDetails(None))
        .updateName(_ => "onSubmit redirect to journey recovery page when scheme date not found")
    )
  }

  def setSchemeDetails(
    schemeDetails: Option[MinimalSchemeDetails]
  ): ScalaOngoingStubbing[Future[Option[MinimalSchemeDetails]]] =
    when(mockSchemeDetailsService.getMinimalSchemeDetails(any(), any())(any(), any()))
      .thenReturn(Future.successful(schemeDetails))
}
