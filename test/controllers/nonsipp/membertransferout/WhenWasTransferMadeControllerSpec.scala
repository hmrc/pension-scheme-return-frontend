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

package controllers.nonsipp.membertransferout

import services.SchemeDateService
import config.Refined.{Max300, Max5}
import controllers.ControllerBaseSpec
import views.html.DatePageView
import eu.timepit.refined.refineMV
import play.api.inject
import forms.DatePageFormProvider
import models.{DateRange, NormalMode, UserAnswers}
import pages.nonsipp.membertransferout.{ReceivingSchemeNamePage, WhenWasTransferMadePage}
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito.{reset, when}

import java.time.LocalDate

class WhenWasTransferMadeControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)
  private lazy val onPageLoad =
    routes.WhenWasTransferMadeController.onPageLoad(srn, index, secondaryIndex, NormalMode)
  private lazy val onSubmit = routes.WhenWasTransferMadeController.onSubmit(srn, index, secondaryIndex, NormalMode)

  val schemeDatePeriod: DateRange = DateRange(LocalDate.parse("2020-04-06"), LocalDate.parse("2021-04-05"))
  private implicit val mockSchemeDateService: SchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(inject.bind[SchemeDateService].toInstance(mockSchemeDateService))

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    setSchemeDate(Some(schemeDatePeriod))
  }

  def setSchemeDate(date: Option[DateRange]): Unit =
    when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(date)

  val userAnswers: UserAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(ReceivingSchemeNamePage(srn, index, secondaryIndex), receivingSchemeName)

  "WhenWasTransferMadeController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[DatePageView]
        .apply(
          WhenWasTransferMadeController.form(injected[DatePageFormProvider], dateRange),
          WhenWasTransferMadeController
            .viewModel(srn, index, secondaryIndex, receivingSchemeName, memberDetails.fullName, NormalMode)
        )
    })

    act.like(
      renderPrePopView(onPageLoad, WhenWasTransferMadePage(srn, index, secondaryIndex), dateRange.to, userAnswers) {
        implicit app => implicit request =>
          injected[DatePageView].apply(
            WhenWasTransferMadeController.form(injected[DatePageFormProvider], dateRange).fill(dateRange.to),
            WhenWasTransferMadeController
              .viewModel(srn, index, secondaryIndex, receivingSchemeName, memberDetails.fullName, NormalMode)
          )
      }
    )

    act.like(
      redirectNextPage(onSubmit, userAnswers, "value.day" -> "10", "value.month" -> "06", "value.year" -> "2020")
    )

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, userAnswers, "value.day" -> "10", "value.month" -> "06", "value.year" -> "2020"))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
