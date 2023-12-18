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

package controllers.nonsipp.loansmadeoroutstanding

import config.Refined.OneTo5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.loansmadeoroutstanding.DatePeriodLoanController._
import eu.timepit.refined.refineMV
import models.NormalMode
import pages.nonsipp.loansmadeoroutstanding.{CompanyRecipientNamePage, DatePeriodLoanPage}
import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.MultipleQuestionView

import java.time.LocalDate

class DatePeriodLoanControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo5000](1)

  private implicit val mockSchemeDateService = mock[SchemeDateService]

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
      injected[MultipleQuestionView].apply(viewModel(srn, index, schemeName, NormalMode, form(date)))
    }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear)))

    act.like(renderPrePopView(onPageLoad, DatePeriodLoanPage(srn, index), (date, money, 12)) {
      implicit app => implicit request =>
        val preparedForm = form(date).fill((date, money, 12))
        injected[MultipleQuestionView].apply(viewModel(srn, index, schemeName, NormalMode, preparedForm))
    }.before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear)))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      saveAndContinue(
        onSubmit,
        "value.1.day" -> "10",
        "value.1.month" -> "12",
        "value.1.year" -> "2020",
        "value.2" -> moneyNegative.value.toString,
        "value.3" -> "12"
      ).withName("save and continue")
        .before(MockSchemeDateService.taxYearOrAccountingPeriods(taxYear))
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
