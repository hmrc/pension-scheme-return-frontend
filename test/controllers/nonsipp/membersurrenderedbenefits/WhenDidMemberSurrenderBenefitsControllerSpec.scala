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

package controllers.nonsipp.membersurrenderedbenefits

import config.Refined.Max300
import controllers.ControllerBaseSpec
import eu.timepit.refined.refineMV
import forms.DatePageFormProvider
import models.NormalMode
import pages.nonsipp.memberdetails.MemberDetailsPage
import pages.nonsipp.membersurrenderedbenefits.{SurrenderedBenefitsAmountPage, WhenDidMemberSurrenderBenefitsPage}
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.DatePageView

class WhenDidMemberSurrenderBenefitsControllerSpec extends ControllerBaseSpec {

  private val index = refineMV[Max300.Refined](1)

  private lazy val onPageLoad = routes.WhenDidMemberSurrenderBenefitsController.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.WhenDidMemberSurrenderBenefitsController.onSubmit(srn, index, NormalMode)

  private implicit val mockSchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(inject.bind[SchemeDateService].toInstance(mockSchemeDateService))

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange)))
  }

  val userAnswers = defaultUserAnswers
    .unsafeSet(MemberDetailsPage(srn, index), memberDetails)
    .unsafeSet(SurrenderedBenefitsAmountPage(srn, index), surrenderedBenefitsAmount)

  "WhenDidMemberSurrenderBenefitsController" - {

    act.like(
      renderView(onPageLoad, userAnswers) { implicit app => implicit request =>
        injected[DatePageView].apply(
          WhenDidMemberSurrenderBenefitsController
            .form(injected[DatePageFormProvider], dateRange),
          WhenDidMemberSurrenderBenefitsController
            .viewModel(srn, index, memberDetails.fullName, surrenderedBenefitsAmount.displayAs, NormalMode)
        )
      }
    )

    act.like(
      renderPrePopView(onPageLoad, WhenDidMemberSurrenderBenefitsPage(srn, index), dateRange.to, userAnswers) {
        implicit app => implicit request =>
          injected[DatePageView].apply(
            WhenDidMemberSurrenderBenefitsController
              .form(injected[DatePageFormProvider], dateRange)
              .fill(dateRange.to),
            WhenDidMemberSurrenderBenefitsController
              .viewModel(srn, index, memberDetails.fullName, surrenderedBenefitsAmount.displayAs, NormalMode)
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
