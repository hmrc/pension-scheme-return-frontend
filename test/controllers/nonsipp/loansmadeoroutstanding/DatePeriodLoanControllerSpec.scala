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

import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.MultipleQuestionView
import utils.IntUtils.given
import models.NormalMode
import pages.nonsipp.loansmadeoroutstanding.{CompanyRecipientNamePage, DatePeriodLoanPage}
import services.SchemeDateService
import controllers.nonsipp.loansmadeoroutstanding.DatePeriodLoanController._
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.reset

import java.time.LocalDate

class DatePeriodLoanControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

  private val index = 1

  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  private val dateTooEarlyForm = List(
    "value.1.day" -> "31",
    "value.1.month" -> "12",
    "value.1.year" -> "1899",
    "value.2" -> moneyNegative.value.toString,
    "value.3" -> "12"
  )

  override def beforeEach(): Unit =
    reset(mockSchemeDateService)

  "DatePeriodLoanController" - {

    val populatedUserAnswers = defaultUserAnswers.set(CompanyRecipientNamePage(srn, index), companyName).get
    lazy val onPageLoad = routes.DatePeriodLoanController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.DatePeriodLoanController.onSubmit(srn, index, NormalMode)

    val date = LocalDate.of(2020, 12, 10)

    val taxYear = Some(Left(dateRange))

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MultipleQuestionView].apply(
        form(date),
        viewModel(srn, index, schemeName, NormalMode, form(date))
      )
    }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear)))

    act.like(
      renderPrePopView(
        onPageLoad,
        DatePeriodLoanPage(srn, index),
        (date, money, 12)
      ) { implicit app => implicit request =>
        val preparedForm = form(date).fill((date, money, 12))

        injected[MultipleQuestionView].apply(
          preparedForm,
          viewModel(srn, index, schemeName, NormalMode, form(date))
        )

      }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      saveAndContinue(
        onSubmit,
        "value.1.day" -> "10",
        "value.1.month" -> "12",
        "value.1.year" -> "2020",
        "value.2" -> moneyNegative.value.toString,
        "value.3" -> "12"
      ).before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(
      invalidForm(onSubmit, populatedUserAnswers).before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(
      invalidForm(onSubmit, populatedUserAnswers, dateTooEarlyForm: _*)
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
    )
  }
}
