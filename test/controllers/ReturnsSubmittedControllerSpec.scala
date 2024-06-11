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

package controllers

import services.{ComparisonService, PsrRetrievalService, PsrVersionsService, SaveService}
import play.api.inject.bind
import pages.nonsipp.{FbVersionPage, WhichTaxYearPage}
import controllers.ReturnsSubmittedController._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, times, verify, when}
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule
import views.html.ReturnsSubmittedView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.{LinkMessage, Message}
import viewmodels.models.TableElem

import scala.concurrent.Future

class ReturnsSubmittedControllerSpec extends ControllerBaseSpec with CommonTestValues {

  private val populatedUserAnswers = {
    defaultUserAnswers
      .unsafeSet(WhichTaxYearPage(srn), dateRange)
      .unsafeSet(FbVersionPage(srn), version)
  }
  private val page = 1
  private def data(srn: Srn) =
    List(
      List(
        TableElem(Message("2", List()), None),
        TableElem(Message("7 April 2020", List()), None),
        TableElem(Message("first last", List()), None),
        TableElem(
          LinkMessage(
            Message("View or change", List()),
            s"/pension-scheme-return/${srn.value}/view-change/select-submitted-returns?fbNumber=223456785022"
          ),
          None
        )
      ),
      List(
        TableElem(Message("1", List()), None),
        TableElem(Message("6 April 2020", List()), None),
        TableElem(Message("pspOrgName", List()), None),
        TableElem(
          LinkMessage(
            Message("View", List()),
            s"/pension-scheme-return/${srn.value}/pension-scheme-return-view-only-task-list/2020-04-06/1/0"
          ),
          None
        )
      )
    )

  private implicit val mockPsrVersionsService: PsrVersionsService = mock[PsrVersionsService]
  private implicit val mockComparisonService: ComparisonService = mock[ComparisonService]
  private implicit val mockSaveService: SaveService = mock[SaveService]
  private implicit val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrVersionsService].toInstance(mockPsrVersionsService),
    bind[ComparisonService].toInstance(mockComparisonService),
    bind[SaveService].toInstance(mockSaveService),
    bind[PsrRetrievalService].toInstance(mockPsrRetrievalService)
  )

  override protected def beforeAll(): Unit = {
    reset(mockPsrVersionsService)
    reset(mockComparisonService)
    reset(mockSaveService)
    reset(mockPsrRetrievalService)
    when(mockPsrVersionsService.getVersions(any(), any())(any(), any())).thenReturn(
      Future.successful(versionsResponse)
    )
    when(mockPsrRetrievalService.getStandardPsrDetails(any(), any(), any(), any())(any(), any(), any())).thenReturn(
      Future.successful(populatedUserAnswers)
    )
    when(mockSaveService.save(any())(any(), any())).thenReturn(Future.successful(()))
    when(mockComparisonService.getPureUserAnswers()(any())).thenReturn(
      Future.successful(Some(populatedUserAnswers))
    )
  }

  "ReturnsSubmittedController" - {

    lazy val onPageLoad = routes.ReturnsSubmittedController.onPageLoad(srn, page)
    lazy val onSelect = routes.ReturnsSubmittedController.onSelect(srn, fbNumber)

    act.like(
      renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
        injected[ReturnsSubmittedView]
          .apply(viewModel(srn, page, data(srn), fromYearUi, toYearUi, schemeName))
      }.before(
          when(mockPsrVersionsService.getVersions(any(), any())(any(), any())).thenReturn(
            Future.successful(versionsResponse)
          )
        )
        .withName("onPageLoad renders ok")
    )

    act.like(
      redirectToPage(
        onSelect,
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      ).withName("onSelect redirects ok")
    )

    "save service is called when currentFbVersion and pureFbVersion are different" - {
      act.like(
        renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
          injected[ReturnsSubmittedView]
            .apply(viewModel(srn, page, data(srn), fromYearUi, toYearUi, schemeName))
        }.before({
            when(mockComparisonService.getPureUserAnswers()(any())).thenReturn(
              Future.successful(Some(defaultUserAnswers.unsafeSet(FbVersionPage(srn), "000")))
            )
          })
          .after({
            verify(mockSaveService, times(1)).save(any())(any(), any())
          })
          .withName("onPageLoad renders ok")
      )
    }
  }
}
