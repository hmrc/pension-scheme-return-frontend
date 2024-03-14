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

package controllers.nonsipp.unregulatedorconnectedbonds

import config.Refined.Max5000
import controllers.ControllerBaseSpec
import controllers.nonsipp.unregulatedorconnectedbonds.BondsListController._
import controllers.nonsipp.unregulatedorconnectedbonds.BondsListController.BondsData
import eu.timepit.refined.refineMV
import forms.YesNoPageFormProvider
import models._
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.bonds._
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.PsrSubmissionService
import viewmodels.models.SectionCompleted
import views.html.TwoColumnsTripleAction

import scala.concurrent.Future

class BondsListControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val page = 1
  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private lazy val onPageLoad =
    controllers.nonsipp.unregulatedorconnectedbonds.routes.BondsListController.onPageLoad(srn, page, NormalMode)

  private lazy val onSubmit =
    controllers.nonsipp.unregulatedorconnectedbonds.routes.BondsListController.onSubmit(srn, page, NormalMode)

  private val userAnswers =
    defaultUserAnswers
      .unsafeSet(BondsCompleted(srn, index), SectionCompleted)
      .unsafeSet(NameOfBondsPage(srn, index), "Name")
      .unsafeSet(WhyDoesSchemeHoldBondsPage(srn, index), SchemeHoldBond.Acquisition)
      .unsafeSet(CostOfBondsPage(srn, index), money)

  private val bondsData = List(
    BondsData(
      index,
      nameOfBonds = "Name",
      acquisitionType = SchemeHoldBond.Acquisition,
      costOfBonds = money
    )
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetails(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "BondsListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[TwoColumnsTripleAction]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, page, NormalMode, bondsData))
    })

    act.like(
      renderPrePopView(onPageLoad, BondsListPage(srn), true, userAnswers) { implicit app => implicit request =>
        injected[TwoColumnsTripleAction]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, page, NormalMode, bondsData)
          )
      }
    )

    act.like(redirectNextPage(onSubmit, "value" -> "true"))
    act.like(redirectNextPage(onSubmit, "value" -> "false"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "true"))

    act.like(invalidForm(onSubmit, userAnswers))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
