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

import services.{SaveService, SchemeDetailsService}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import viewmodels.implicits._
import play.api.mvc.Call
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.YesNoPageView
import pages.nonsipp.{BasicDetailsCompletedPage, CheckReturnDatesPage, WhichTaxYearPage}
import forms.YesNoPageFormProvider
import org.mockito.stubbing.OngoingStubbing
import models.{MinimalSchemeDetails, NormalMode, UserAnswers}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import utils.DateTimeUtils
import play.api.inject.guice.GuiceableModule
import viewmodels.DisplayMessage.{Message, ParagraphMessage}
import viewmodels.models.{FormPageViewModel, SectionCompleted, YesNoPageViewModel}

import scala.concurrent.Future

class CheckReturnDatesControllerSpec extends ControllerBaseSpec with ScalaCheckPropertyChecks { self =>

  private val mockSchemeDetailsService = mock[SchemeDetailsService]
  private implicit val mockSaveService: SaveService = mock[SaveService]

  override val additionalBindings: List[GuiceableModule] =
    List(
      bind[SchemeDetailsService].toInstance(mockSchemeDetailsService)
    )

  override protected def beforeEach(): Unit =
    MockSaveService.save()

  private val userAnswers = defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), dateRange)

  def onwardRoute: Call = Call("GET", "/foo")

  lazy val checkReturnDatesRoute: String = routes.CheckReturnDatesController.onPageLoad(srn, NormalMode).url
  lazy val onPageLoad: Call = routes.CheckReturnDatesController.onPageLoad(srn, NormalMode)
  lazy val onSubmit: Call = routes.CheckReturnDatesController.onSubmit(srn, NormalMode)

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

  "CheckReturnDates Controller" - {

    val minimalSchemeDetails = minimalSchemeDetailsGen.sample.value
    lazy val viewModel: FormPageViewModel[YesNoPageViewModel] =
      CheckReturnDatesController.viewModel(
        srn,
        NormalMode,
        dateRange.from,
        dateRange.to
      )

    val form = CheckReturnDatesController.form(new YesNoPageFormProvider())

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      val view = injected[YesNoPageView]
      view(form, viewModel)
    }.before(setSchemeDetails(Some(minimalSchemeDetails))))

    act.like(renderPrePopView(onPageLoad, CheckReturnDatesPage(srn), true, userAnswers) {
      implicit app => implicit request =>
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

    act.like(
      saveAndContinue(onSubmit, userAnswers, formData(form, true): _*)
        .before(setSchemeDetails(Some(minimalSchemeDetails)))
    )

    act.like(invalidForm(onSubmit, userAnswers).before(setSchemeDetails(Some(minimalSchemeDetails))))

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(setSchemeDetails(Some(minimalSchemeDetails)))
        .updateName("onSubmit" + _)
    )

    val userAnswersCaptor: ArgumentCaptor[UserAnswers] = ArgumentCaptor.forClass(classOf[UserAnswers])

    act.like(
      redirectToPage(
        call = onSubmit,
        page = controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoad(srn, NormalMode),
        userAnswers = userAnswers
          .unsafeSet(BasicDetailsCompletedPage(srn), SectionCompleted)
          .unsafeSet(CheckReturnDatesPage(srn), true),
        previousUserAnswers = emptyUserAnswers,
        form = formData(form, true): _*
      ).before(
          setSchemeDetails(Some(minimalSchemeDetails))
        )
        .withName(
          "onSubmit redirect to basic details CYA when basicDetails are completed and answers is the same as previous answer"
        )
    )

    act.like(
      redirectToPage(
        call = onSubmit,
        page = controllers.nonsipp.schemedesignatory.routes.ActiveBankAccountController.onPageLoad(srn, NormalMode),
        userAnswers = userAnswers
          .unsafeSet(CheckReturnDatesPage(srn), false),
        previousUserAnswers = emptyUserAnswers,
        mockSaveService = Some(mockSaveService),
        form = formData(form, true): _*
      ).before {
          setSchemeDetails(Some(minimalSchemeDetails))
        }
        .withName(
          "onSubmit redirect to next page when the user has not finished the basic details section yet"
        )
        .after {
          MockSaveService.capture(userAnswersCaptor)
          userAnswersCaptor.getValue.get(CheckReturnDatesPage(srn)) mustEqual Some(true)
        }
    )

    act.like(
      journeyRecoveryPage(onSubmit)
        .before(setSchemeDetails(None))
        .updateName(_ => "onSubmit redirect to journey recovery page when scheme date not found")
    )
  }

  def setSchemeDetails(
    schemeDetails: Option[MinimalSchemeDetails]
  ): OngoingStubbing[Future[Option[MinimalSchemeDetails]]] =
    when(mockSchemeDetailsService.getMinimalSchemeDetails(any(), any())(any(), any()))
      .thenReturn(Future.successful(schemeDetails))
}
