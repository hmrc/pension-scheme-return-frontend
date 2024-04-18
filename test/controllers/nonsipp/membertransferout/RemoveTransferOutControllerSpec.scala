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

import services.PsrSubmissionService
import config.Refined.{Max300, Max5}
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.YesNoPageView
import eu.timepit.refined.refineMV
import controllers.nonsipp.membertransferout.RemoveTransferOutController._
import forms.YesNoPageFormProvider
import models.NameDOB
import pages.nonsipp.membertransferout.ReceivingSchemeNamePage
import org.mockito.ArgumentMatchers.any
import play.api.inject.guice.GuiceableModule
import pages.nonsipp.memberdetails.MemberDetailsPage
import org.mockito.Mockito.{reset, when}

import scala.concurrent.Future

class RemoveTransferOutControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)
  private val secondaryIndex = refineMV[Max5.Refined](1)
  private lazy val onPageLoad = routes.RemoveTransferOutController.onPageLoad(srn, index, secondaryIndex)
  private lazy val onSubmit = routes.RemoveTransferOutController.onSubmit(srn, index, secondaryIndex)

  private val mockPsrSubmissionService = mock[PsrSubmissionService]

  override val memberDetails: NameDOB = nameDobGen.sample.value

  private val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, refineMV(1)), memberDetails)
    .unsafeSet(MemberDetailsPage(srn, refineMV(2)), memberDetails)
    .unsafeSet(ReceivingSchemeNamePage(srn, index, secondaryIndex), receivingSchemeName)

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrSubmissionService].toInstance(mockPsrSubmissionService)
  )

  override protected def beforeEach(): Unit = {
    reset(mockPsrSubmissionService)
    when(mockPsrSubmissionService.submitPsrDetailsWithUA(any(), any())(any(), any(), any()))
      .thenReturn(Future.successful(Some(())))
  }

  "RemoveTransferOutController" - {

    act.like(renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
      injected[YesNoPageView]
        .apply(
          form(injected[YesNoPageFormProvider]),
          viewModel(srn, index, secondaryIndex, memberDetails.fullName, receivingSchemeName)
        )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(continueNoSave(onSubmit, userAnswers, "value" -> "false"))

    act.like(saveAndContinue(onSubmit, userAnswers, "value" -> "true"))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

  }

}
