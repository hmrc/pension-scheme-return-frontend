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

$! Generic !$
$if(directory.empty)$
package controllers.nonsipp
$else$
package controllers.nonsipp.$directory$
$endif$

$if(directory.empty)$
import pages.nonsipp.$className$Page
import controllers.nonsipp.$className;format="cap"$Controller._
$else$
import pages.nonsipp.$directory$.$className$Page
import controllers.nonsipp.$directory$.$className;format="cap"$Controller._
$endif$

$if(!index.empty)$
import config.Refined._
import eu.timepit.refined.refineMV
$endif$

import models.NormalMode
import controllers.ControllerBaseSpec
$! Generic end !$

import java.time.LocalDate
import views.html.DatePageView
import forms.DatePageFormProvider
import services.SchemeDateService
import play.api.inject
import play.api.inject.guice.GuiceableModule

class $className;format="cap"$ControllerSpec extends ControllerBaseSpec {

  $! Generic !$
  $if(index.empty)$
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, NormalMode)
  $else$
  private val index = refineMV[$index$.Refined](1)
  $if(secondaryIndex.empty)$
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, index, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, index, NormalMode)
  $else$
  private val secondaryIndex = refineMV[$secondaryIndex$.Refined](1)
  private lazy val onPageLoad = routes.$className;format="cap"$Controller.onPageLoad(srn, index, secondaryIndex, NormalMode)
  private lazy val onSubmit = routes.$className;format="cap"$Controller.onSubmit(srn, index, secondaryIndex, NormalMode)
  $endif$
  $endif$

  $if(!requiredPage.empty) $
  private val userAnswers = defaultUserAnswers.unsafeSet($requiredPage$(srn, index), ???)
  $endif$
  $! Generic end !$

  private implicit val mockSchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(inject.bind[SchemeDateService].toInstance(mockSchemeDateService))

  override def beforeEach(): Unit = {
    reset(mockSchemeDateService)
    MockSchemeDateService.taxYearOrAccountingPeriods(Some(Left(dateRange)))
  }

  "$className;format="cap"$Controller" - {

    $! Generic (change view and form value) !$
    act like renderView(onPageLoad$if(!requiredPage.empty)$, userAnswers$endif$) { implicit app => implicit request =>
      injected[DatePageView].apply(form(injected[DatePageFormProvider], localDate), viewModel(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$NormalMode))
    }

    act like renderPrePopView(onPageLoad, $className;format="cap"$Page(srn$if(!index.empty)$, index$endif$$if(!secondaryIndex.empty)$, secondaryIndex$endif$), localDate$if(!requiredPage.empty)$, userAnswers$endif$) { implicit app => implicit request =>
      injected[DatePageView].apply(form(injected[DatePageFormProvider], localDate).fill(localDate), viewModel(srn, $if(!index.empty)$index, $endif$$if(!secondaryIndex.empty)$secondaryIndex, $endif$NormalMode))
    }
    $! Generic end !$

    act.like(redirectNextPage(onSubmit, $if(!requiredPage.empty)$userAnswers, $endif$"value" -> "1"))

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad" + _))

    act.like(saveAndContinue(onSubmit, $if(!requiredPage.empty)$userAnswers, $endif$"value" -> "1"))

    act.like(invalidForm(onSubmit$if(!requiredPage.empty)$, userAnswers$endif$))
    act.like(journeyRecoveryPage(onSubmit).updateName("onSubmit" + _))
  }
}
