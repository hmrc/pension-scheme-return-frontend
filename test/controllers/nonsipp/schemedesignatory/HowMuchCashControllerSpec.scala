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

package controllers.nonsipp.schemedesignatory

import services.SchemeDateService
import pages.nonsipp.schemedesignatory.HowMuchCashPage
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import play.api.inject.bind
import views.html.MultipleQuestionView
import utils.Transform._
import play.api.libs.json.JsPath
import forms.MoneyFormProvider
import models.{DateRange, Money, NormalMode}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, when}

class HowMuchCashControllerSpec extends ControllerBaseSpec with ControllerBehaviours {

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
    when(mockSchemeDateService.schemeDate(any())(using any())).thenReturn(date)

  "HowMuchCashController" - {

    val schemeName = defaultSchemeDetails.schemeName

    val form = HowMuchCashController.form(new MoneyFormProvider(), schemeName, schemeDatePeriod)
    lazy val viewModel = HowMuchCashController.viewModel(srn, NormalMode, schemeName, schemeDatePeriod, _)

    val moneyInPeriodData = moneyInPeriodGen.sample.value

    lazy val onPageLoad = routes.HowMuchCashController.onPageLoad(srn, NormalMode)
    lazy val onSubmit = routes.HowMuchCashController.onSubmit(srn, NormalMode)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[MultipleQuestionView]
      view(form, viewModel(form))
    })

    act.like(renderPrePopView(onPageLoad, HowMuchCashPage(srn, NormalMode), moneyInPeriodData) {
      implicit app => implicit request =>
        val view = injected[MultipleQuestionView]
        view(form.fill(moneyInPeriodData.from[(Money, Money)]), viewModel(form))
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
        Some(JsPath \ "schemeDesignatory" \ "totalCash"),
        formData(form, moneyInPeriodData.from[(Money, Money)])*
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
