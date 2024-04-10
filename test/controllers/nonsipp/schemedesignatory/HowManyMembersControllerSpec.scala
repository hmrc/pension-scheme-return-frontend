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
import pages.nonsipp.schemedesignatory.HowManyMembersPage
import play.api.inject.guice.GuiceableModule
import controllers.ControllerBaseSpec
import play.api.inject.bind
import views.html.IntView
import controllers.nonsipp.schemedesignatory.HowManyMembersController._
import forms.IntFormProvider
import models.{NormalMode, SchemeMemberNumbers}
import org.mockito.ArgumentMatchers.any

import java.time.LocalDate

class HowManyMembersControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.HowManyMembersController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.HowManyMembersController.onSubmit(srn, NormalMode)

  private val submissionEndDate = date.sample.value
  private val pensionSchemeId = pensionSchemeIdGen.sample.value
  private val mockSchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(bind[SchemeDateService].toInstance(mockSchemeDateService))

  def setEndDate(date: Option[LocalDate]): Unit =
    when(mockSchemeDateService.schemeEndDate(any())(any())).thenReturn(date)

  override def beforeAll(): Unit = setEndDate(Some(submissionEndDate))

  override def afterEach(): Unit = setEndDate(Some(submissionEndDate))

  "HowManyMembersController" - {

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[IntView]
      val viewForm = form(injected[IntFormProvider])

      view(
        viewModel(srn, defaultSchemeDetails.schemeName, submissionEndDate, NormalMode, viewForm)
      )
    })

    act.like(renderPrePopView(onPageLoad, HowManyMembersPage(srn, pensionSchemeId), SchemeMemberNumbers(1, 2, 3)) {
      implicit app => implicit request =>
        val view = injected[IntView]
        val populatedForm = form(injected[IntFormProvider]).fill((1, 2, 3))

        view(
          viewModel(srn, defaultSchemeDetails.schemeName, submissionEndDate, NormalMode, populatedForm)
        )
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))

    act.like(
      journeyRecoveryPage(onPageLoad, Some(defaultUserAnswers))
        .before(setEndDate(None))
        .withName("onPageLoad redirect to journey recovery page when no end date found")
    )

    act.like(saveAndContinue(onSubmit, "value.1" -> "1", "value.2" -> "2", "value.3" -> "3"))

    act.like(invalidForm(onSubmit))

    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))

    act.like(
      journeyRecoveryPage(onSubmit, Some(defaultUserAnswers))
        .before(setEndDate(None))
        .withName("onSubmit redirect to journey recovery page when no end date found")
    )
  }
}
