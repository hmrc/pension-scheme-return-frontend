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

package controllers.nonsipp.receivetransfer

import config.Refined._
import controllers.ControllerBaseSpec
import controllers.nonsipp.receivetransfer.WhenWasTransferReceivedController._
import eu.timepit.refined.refineMV
import forms.DatePageFormProvider
import models.NormalMode
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.receivetransfer.{TransferringSchemeNamePage, WhenWasTransferReceivedPage}
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.DatePageView

class WhenWasTransferReceivedControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)
  private lazy val onPageLoad =
    routes.WhenWasTransferReceivedController.onPageLoad(srn, index, secondaryIndex, NormalMode)
  private lazy val onSubmit = routes.WhenWasTransferReceivedController.onSubmit(srn, index, secondaryIndex, NormalMode)

  private implicit val mockSchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(inject.bind[SchemeDateService].toInstance(mockSchemeDateService))

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange)))
  }

  val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(TransferringSchemeNamePage(srn, index, secondaryIndex), transferringSchemeName)

  "TransferReceivedController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[DatePageView]
        .apply(
          form(injected[DatePageFormProvider], dateRange),
          viewModel(srn, index, secondaryIndex, transferringSchemeName, memberDetails.fullName, NormalMode)
        )
    })

    act.like(
      renderPrePopView(onPageLoad, WhenWasTransferReceivedPage(srn, index, secondaryIndex), dateRange.to, userAnswers) {
        implicit app => implicit request =>
          injected[DatePageView].apply(
            form(injected[DatePageFormProvider], dateRange).fill(dateRange.to),
            viewModel(srn, index, secondaryIndex, transferringSchemeName, memberDetails.fullName, NormalMode)
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
