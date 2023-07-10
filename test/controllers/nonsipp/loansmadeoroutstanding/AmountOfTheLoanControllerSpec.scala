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

import config.Refined.OneTo9999999
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.MoneyFormProvider
import models.{DateRange, NormalMode}
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.loansmadeoroutstanding.AmountOfTheLoanPage
import play.api.inject.bind
import services.SchemeDateService
import views.html.MoneyView

class AmountOfTheLoanControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[OneTo9999999](1)

  val schemeDatePeriod = dateRangeGen.sample.value
  val mockSchemeDateService = mock[SchemeDateService]
  val maxAllowedAmount = 999999999.99

  override val additionalBindings = List(
    bind[SchemeDateService].toInstance(mockSchemeDateService)
  )

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    setSchemeDate(Some(schemeDatePeriod))
  }

  def setSchemeDate(date: Option[DateRange]): Unit =
    when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(date)

  "AmountOfTheLoanController" - {

    val schemeName = defaultSchemeDetails.schemeName

    val form = AmountOfTheLoanController.form(new MoneyFormProvider(), schemeDatePeriod)
    lazy val viewModel = AmountOfTheLoanController.viewModel(srn, index, NormalMode, schemeName, schemeDatePeriod, _)

    lazy val onPageLoad = routes.AmountOfTheLoanController.onPageLoad(srn, index, NormalMode)
    lazy val onSubmit = routes.AmountOfTheLoanController.onSubmit(srn, index, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[MoneyView]
      view(viewModel(form))
    })

    act.like(renderPrePopView(onPageLoad, AmountOfTheLoanPage(srn, index), (money, money, money)) {
      implicit app => implicit request =>
        val view = injected[MoneyView]
        view(viewModel(form.fill((money, money, money))))
    })

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
