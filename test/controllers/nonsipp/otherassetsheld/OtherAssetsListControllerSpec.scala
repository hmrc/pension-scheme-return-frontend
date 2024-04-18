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

package controllers.nonsipp.otherassetsheld

import services.PsrSubmissionService
import pages.nonsipp.otherassetsheld._
import config.Refined.Max5000
import controllers.ControllerBaseSpec
import views.html.TwoColumnsTripleAction
import eu.timepit.refined.refineMV
import play.api.inject
import forms.YesNoPageFormProvider
import controllers.nonsipp.otherassetsheld.OtherAssetsListController._
import models._
import viewmodels.models.SectionCompleted
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import org.mockito.Mockito.{reset, when}

import scala.concurrent.Future

class OtherAssetsListControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max5000.Refined](1)
  private val page = 1
  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  private lazy val onPageLoad =
    controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onPageLoad(srn, page, NormalMode)

  private lazy val onSubmit =
    controllers.nonsipp.otherassetsheld.routes.OtherAssetsListController.onSubmit(srn, page, NormalMode)

  private val userAnswers =
    defaultUserAnswers
      .unsafeSet(OtherAssetsCompleted(srn, index), SectionCompleted)
      .unsafeSet(WhatIsOtherAssetPage(srn, index), nameOfAsset)

  private val otherAssetsData = List(
    OtherAssetsData(
      index,
      nameOfAsset
    )
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  override protected val additionalBindings: List[GuiceableModule] = List(
    inject.bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  "OtherAssetsListController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[TwoColumnsTripleAction]
        .apply(form(injected[YesNoPageFormProvider]), viewModel(srn, page, NormalMode, otherAssetsData))
    })

    act.like(
      renderPrePopView(onPageLoad, OtherAssetsListPage(srn), true, userAnswers) { implicit app => implicit request =>
        injected[TwoColumnsTripleAction]
          .apply(
            form(injected[YesNoPageFormProvider]).fill(true),
            viewModel(srn, page, NormalMode, otherAssetsData)
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
