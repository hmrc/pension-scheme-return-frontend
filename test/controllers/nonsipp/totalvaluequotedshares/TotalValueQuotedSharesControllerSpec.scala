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

package controllers.nonsipp.totalvaluequotedshares

import services.SchemeDateService
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.MoneyView
import controllers.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesController.viewModel
import forms.MoneyFormProvider
import models.DateRange
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.totalvaluequotedshares.TotalValueQuotedSharesPage

class TotalValueQuotedSharesControllerSpec extends ControllerBaseSpec {

  val schemeDatePeriod = dateRangeGen.sample.value
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

  "TotalValueQuotedSharesController" - {

    val schemeName = defaultSchemeDetails.schemeName
    val validMoney = moneyGen.sample.value

    val form = TotalValueQuotedSharesController.form(new MoneyFormProvider(), schemeDatePeriod)

    lazy val onPageLoad = routes.TotalValueQuotedSharesController.onPageLoad(srn)
    lazy val onSubmit = routes.TotalValueQuotedSharesController.onSubmit(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MoneyView].apply(viewModel(srn, schemeName, form, schemeDatePeriod))
    })

    act.like(renderPrePopView(onPageLoad, TotalValueQuotedSharesPage(srn), validMoney) {
      implicit app => implicit request =>
        injected[MoneyView].apply(viewModel(srn, schemeName, form.fill(validMoney), schemeDatePeriod))
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(
      journeyRecoveryPage(onPageLoad)
        .withName("onPageLoad redirect to journey recovery page when scheme date not found")
        .before(setSchemeDate(None))
    )

    act.like(saveAndContinue(onSubmit, formData(form, validMoney): _*))

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
