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

package controllers.nonsipp

import controllers.ControllerBaseSpec
import org.mockito.ArgumentMatchers.any
import play.api.inject
import play.api.inject.guice.GuiceableModule
import services.SchemeDateService
import views.html.TaskListView

class TaskListControllerSpec extends ControllerBaseSpec {

  val schemeDateRange = dateRangeGen.sample.value

  private val mockSchemeDateService = mock[SchemeDateService]

  override val additionalBindings: List[GuiceableModule] =
    List(inject.bind[SchemeDateService].toInstance(mockSchemeDateService))

  override def beforeEach(): Unit = {
    super.beforeEach()
    when(mockSchemeDateService.schemeDate(any())(any())).thenReturn(Some(schemeDateRange))
  }

  "TaskListController" - {

    lazy val viewModel = TaskListController.viewModel(srn, schemeName, schemeDateRange.from, schemeDateRange.to)
    lazy val onPageLoad = routes.TaskListController.onPageLoad(srn)

    act.like(renderView(onPageLoad) { implicit app => implicit request =>
      val view = injected[TaskListView]
      view(viewModel)
    })

    act.like(journeyRecoveryPage(onPageLoad).updateName("onPageLoad " + _))
  }
}
