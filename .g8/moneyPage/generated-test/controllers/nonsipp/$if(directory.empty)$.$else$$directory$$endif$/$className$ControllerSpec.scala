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

$if(directory.empty)$
package controllers.nonsipp
$else$
package controllers.nonsipp.$directory$
$endif$

import models.{NormalMode, UserAnswers}
import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.SessionRepository
import forms.MoneyFormProvider
import views.html.MoneyView
import controllers.ControllerBaseSpec
import $className;format="cap"$Controller._
$if(directory.empty)$
import pages.$className$Page
$else$
import pages.nonsipp.$directory$.$className$Page
$endif$
$if(!index.empty)$
import config.Refined.$index$
import eu.timepit.refined.refineMV
$endif$

import scala.concurrent.Future

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  $if(index.empty)$
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)
  $else$
  private val index = refineMV[$index$.Refined](1)
  private lazy val onPageLoad = routes.$className; format = "cap" $Controller.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.$className; format = "cap" $Controller.onSubmit(srn, index, NormalMode)
  $endif$

  "$className;format="cap"$Controller" - {

    $if(index.empty)$
    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MoneyView].apply(viewModel(srn, form(injected[MoneyFormProvider]), NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, $className;format="cap"$Page(srn), money) { implicit app => implicit request =>
      injected[MoneyView].apply(viewModel(srn, form(injected[MoneyFormProvider]).fill(money), NormalMode))
    })
    $else$
    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      injected[MoneyView].apply(viewModel(srn, index, form(injected[MoneyFormProvider]), NormalMode))
    })

    act.like(renderPrePopView(onPageLoad, $className;format="cap"$Page(srn, index), money) { implicit app => implicit request =>
      injected[MoneyView].apply(viewModel(srn, index, form(injected[MoneyFormProvider]).fill(money), NormalMode))
    })
    $endif$

    act.like(redirectNextPage(onSubmit, "value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, "value" -> "1"))

    act.like(invalidForm(onSubmit))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
