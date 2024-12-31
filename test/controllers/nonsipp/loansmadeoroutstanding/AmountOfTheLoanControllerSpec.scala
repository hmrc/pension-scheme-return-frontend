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

package controllers.nonsipp.loansmadeoroutstanding

import services.SchemeDateService
import controllers.nonsipp.loansmadeoroutstanding.AmountOfTheLoanController.partialAnswersForm
import play.api.inject.bind
import views.html.MultipleQuestionView
import eu.timepit.refined.refineMV
import forms.MoneyFormProvider
import models._
import pages.nonsipp.loansmadeoroutstanding.{AmountOfTheLoanPage, AreRepaymentsInstalmentsPage}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, when}
import config.RefinedTypes.OneTo5000
import controllers.ControllerBaseSpec

class AmountOfTheLoanControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  val schemeDatePeriod: DateRange = dateRangeGen.sample.value
  val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]
  val maxAllowedAmount = 999999999.99

  override val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    setSchemeDate(Some(schemeDatePeriod))
  }

  def setSchemeDate(date: Option[DateRange]): Unit =
    when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(date)

  val partialUserAnswers: UserAnswers =
    defaultUserAnswers.unsafeSet(AmountOfTheLoanPage(srn, index), partialAmountOfTheLoan)

  val prePopUserAnswers: UserAnswers =
    defaultUserAnswers.unsafeSet(AreRepaymentsInstalmentsPage(srn, index), true)

  "AmountOfTheLoanController" - {

    val schemeName = defaultSchemeDetails.schemeName

    val form = AmountOfTheLoanController.form(new MoneyFormProvider(), schemeDatePeriod)
    lazy val viewModel = AmountOfTheLoanController.viewModel(srn, index, NormalMode, schemeName, schemeDatePeriod, _)

    lazy val onPageLoad = routes.AmountOfTheLoanController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.AmountOfTheLoanController.onSubmit(srn, index, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[MultipleQuestionView]
      view(form, viewModel(form))
    })

    act.like(renderPrePopView(onPageLoad, AmountOfTheLoanPage(srn, index), amountOfTheLoan) {
      implicit app => implicit request =>
        val view = injected[MultipleQuestionView]
        view(form.fill((money, money, money)), viewModel(form))
    }.withName("return OK and the correct pre-populated view for a GET (full answers)"))

    act.like(renderViewWithPrePopSession(onPageLoad, partialUserAnswers) { implicit app => implicit request =>
      val view = injected[MultipleQuestionView]
      view(partialAnswersForm.fill((money, None, None)), viewModel(form))
    }.withName("return OK and the correct pre-populated view for a GET (partial answers only)"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      journeyRecoveryPage(onPageLoad)
        .withName("onPageLoad redirect to journey recovery page when scheme date not found")
        .before(setSchemeDate(None))
    )
    act.like(
      saveAndContinue(
        onSubmit,
        "value.1" -> money.value.toString,
        "value.2" -> money.value.toString,
        "value.3" -> money.value.toString
      )
    )

    act.like(
      redirectToPageWithPrePopSession(
        call = onSubmit,
        page = controllers.nonsipp.loansmadeoroutstanding.routes.InterestOnLoanController
          .onPageLoad(srn, index, NormalMode),
        userAnswers = prePopUserAnswers,
        previousUserAnswers = defaultUserAnswers,
        mockSaveService = None,
        form = "value.1" -> money.value.toString,
        "value.2" -> money.value.toString,
        "value.3" -> money.value.toString
      )
    )

    act.like(invalidForm(onSubmit))

    act.like(
      invalidForm(onSubmit, "value" -> (maxAllowedAmount + 0.001).toString)
        .withName("fail to submit when amount entered is greater than maximum allowed amount")
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(
      journeyRecoveryPage(onSubmit)
        .withName("onSubmit redirect to journey recovery page when scheme date not found")
        .before(setSchemeDate(None))
    )
  }
}
