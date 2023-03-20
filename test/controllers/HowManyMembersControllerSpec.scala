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

package controllers

import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import pages.HowManyMembersPage
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import forms.TripleIntFormProvider
import views.html.TripleIntView
import HowManyMembersController._

import scala.concurrent.Future

class HowManyMembersControllerSpec extends ControllerBaseSpec {

  private lazy val onPageLoad = routes.HowManyMembersController.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.HowManyMembersController.onSubmit(srn, NormalMode)

  "HowManyMembersController" should {

    behave.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[TripleIntView]

      view(form(injected[TripleIntFormProvider]), viewModel(srn, ???, ???, NormalMode))
    })

    behave.like(renderPrePopView(onPageLoad, HowManyMembersPage(srn), (1, 2, 3)) { implicit app => implicit request =>
      val view = injected[TripleIntView]

      view(form(injected[TripleIntFormProvider]).fill((1, 2, 3)), viewModel(srn, ???, ???, NormalMode))
    })

    behave.like(journeyRecoveryPage("onPageLoad", onPageLoad))

    behave.like(saveAndContinue(onSubmit, "value.1" -> "1", "value.2" -> "2", "value.3" -> "3"))

    behave.like(invalidForm(onSubmit))
  }
}
