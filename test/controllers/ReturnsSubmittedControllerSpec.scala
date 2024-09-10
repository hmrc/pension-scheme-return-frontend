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

import services.{PsrRetrievalService, PsrVersionsService}
import pages.nonsipp.schemedesignatory.HowManyMembersPage
import play.api.inject.bind
import eu.timepit.refined.refineMV
import pages.nonsipp.WhichTaxYearPage
import controllers.ReturnsSubmittedController._
import org.mockito.ArgumentMatchers.any
import pages.nonsipp.memberdetails.DoesMemberHaveNinoPage
import org.mockito.Mockito._
import utils.CommonTestValues
import play.api.inject.guice.GuiceableModule
import views.html.ReturnsSubmittedView
import models.SchemeId.Srn
import viewmodels.DisplayMessage.{LinkMessage, Message}
import viewmodels.models.TableElem

import scala.concurrent.Future

class ReturnsSubmittedControllerSpec extends ControllerBaseSpec with CommonTestValues {

  private val populatedUserAnswers = {
    defaultUserAnswers.unsafeSet(WhichTaxYearPage(srn), dateRange)
  }
  private val page = 1

  private def dataItemFirst(srn: Srn): List[TableElem] =
    List(
      TableElem(Message("1", List()), None),
      TableElem(Message("6 April 2020", List()), None),
      TableElem(Message("pspOrgName", List()), None),
      TableElem(
        LinkMessage(
          Message("View", List()),
          s"/pension-scheme-return/${srn.value}/view-change/select-submitted-returns-to-view/2020-04-06/1/0"
        ),
        None
      )
    )

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
      dataItemFirst(srn)
    )
  private def dataInProgress(srn: Srn): List[List[TableElem]] =
    List(
      List(
        TableElem(Message("3", List()), None),
        TableElem(Message("8 April 2020", List()), None),
        TableElem(Message("Changes in progress", List()), None),
        TableElem(
          LinkMessage(
            Message("Continue", List()),
            s"/pension-scheme-return/${srn.value}/view-change/select-submitted-returns?fbNumber=323456785033",
            hiddenText = Message("making changes", List())
          ),
          None
        )
      ),
      List(
        TableElem(Message("2", List()), None),
        TableElem(Message("7 April 2020", List()), None),
        TableElem(Message("first last", List()), None),
        TableElem(
          LinkMessage(
            Message("View", List()),
            s"/pension-scheme-return/${srn.value}/view-change/select-submitted-returns-to-view/2020-04-06/2/1"
          ),
          None
        )
      ),
      dataItemFirst(srn)
    )

  private implicit val mockPsrVersionsService: PsrVersionsService = mock[PsrVersionsService]
  private implicit val mockPsrRetrievalService: PsrRetrievalService = mock[PsrRetrievalService]

  override protected val additionalBindings: List[GuiceableModule] = List(
    bind[PsrVersionsService].toInstance(mockPsrVersionsService),
    bind[PsrRetrievalService].toInstance(mockPsrRetrievalService)
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockPsrVersionsService)
    reset(mockPsrRetrievalService)
  }

  "ReturnsSubmittedController" - {

    lazy val onPageLoad = routes.ReturnsSubmittedController.onPageLoad(srn, page)
    lazy val onSelect = routes.ReturnsSubmittedController.onSelect(srn, fbNumber)
    lazy val onSelectToView =
      routes.ReturnsSubmittedController.onSelectToView(srn, yearString, submissionNumberTwo, submissionNumberOne)

    act.like(
      renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
        injected[ReturnsSubmittedView]
          .apply(viewModel(srn, page, data(srn), fromYearUi, toYearUi, schemeName))
      }.before(
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any())).thenReturn(
            Future.successful(versionsResponse)
          )
        )
        .after(verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any()))
        .withName("onPageLoad renders ok")
    )

    act.like(
      renderView(onPageLoad, populatedUserAnswers) { implicit app => implicit request =>
        injected[ReturnsSubmittedView]
          .apply(viewModel(srn, page, dataInProgress(srn), fromYearUi, toYearUi, schemeName))
      }.before(
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any())).thenReturn(
            Future.successful(versionsResponseInProgress)
          )
        )
        .after(verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any()))
        .withName("onPageLoad inProgress renders ok")
    )

    act.like(
      redirectToPage(
        onSelect,
        controllers.nonsipp.routes.TaskListController.onPageLoad(srn)
      ).withName("onSelect redirects ok")
    )

    act.like(
      redirectToPage(
        onSelectToView,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(
            srn,
            yearString,
            submissionNumberTwo,
            submissionNumberOne
          )
      ).after {
          verify(mockPsrVersionsService, never).getVersions(any(), any(), any())(any(), any())
          verify(mockPsrRetrievalService, never)
            .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
        }
        .withName("onSelectToView redirects ok ViewOnlyTaskListController when members empty")
    )

    act.like(
      redirectToPage(
        onSelectToView,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(
            srn,
            yearString,
            submissionNumberTwo,
            submissionNumberOne
          ),
        emptyUserAnswers.unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersUnderThreshold)
      ).after {
          verify(mockPsrVersionsService, never).getVersions(any(), any(), any())(any(), any())
          verify(mockPsrRetrievalService, never)
            .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
        }
        .withName(
          "onSelectToView redirects ok ViewOnlyTaskListController when members under threshold"
        )
    )

    act.like(
      redirectToPage(
        onSelectToView,
        controllers.nonsipp.routes.ViewOnlyTaskListController
          .onPageLoad(
            srn,
            yearString,
            submissionNumberTwo,
            submissionNumberOne
          ),
        userAnswers = emptyUserAnswers.unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold),
        previousUserAnswers = emptyUserAnswers.unsafeSet(DoesMemberHaveNinoPage(srn, refineMV(1)), true)
      ).after {
          verify(mockPsrVersionsService, never).getVersions(any(), any(), any())(any(), any())
          verify(mockPsrRetrievalService, never)
            .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
        }
        .withName(
          "onSelectToView redirects ok ViewOnlyTaskListController when members over threshold and previous userAnswers has some memberDetails"
        )
    )

    act.like(
      redirectToPage(
        onSelectToView,
        controllers.routes.JourneyRecoveryController.onPageLoad(),
        userAnswers = emptyUserAnswers.unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)
      ).after {
          verify(mockPsrVersionsService, never).getVersions(any(), any(), any())(any(), any())
          verify(mockPsrRetrievalService, never)
            .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
        }
        .withName(
          "onSelectToView redirects ok ViewOnlyTaskListController when members over threshold and previous userAnswers empty and no TaxYear data"
        )
    )

    val overThresholdUA = emptyUserAnswers
      .unsafeSet(HowManyMembersPage(srn, psaId), memberNumbersOverThreshold)
      .unsafeSet(WhichTaxYearPage(srn), dateRange)
    act.like(
      redirectToPage(
        onSelectToView,
        controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoadViewOnly(
          srn,
          yearString,
          submissionNumberTwo,
          submissionNumberOne
        ),
        userAnswers = overThresholdUA
      ).after {
          verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any())
          verify(mockPsrRetrievalService, never)
            .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
        }
        .before(
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any())).thenReturn(
            Future.successful(Seq())
          )
        )
        .withName(
          "onSelectToView redirects ok the BasicDetailsCYA page when members over threshold and no previous userAnswers and no previous psr return at all"
        )
    )

    act.like(
      redirectToPage(
        onSelectToView,
        controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoadViewOnly(
          srn,
          yearString,
          submissionNumberTwo,
          submissionNumberOne
        ),
        userAnswers = overThresholdUA
      ).after {
          verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any())
          verify(mockPsrRetrievalService, times(1))
            .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
        }
        .before {
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any())).thenReturn(
            Future.successful(versionsResponse)
          )
          when(
            mockPsrRetrievalService
              .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
          ).thenReturn(Future.successful(emptyUserAnswers))
        }
        .withName(
          "onSelectToView redirects ok the BasicDetailsCYA page when members over threshold and no " +
            "previous userAnswers and no previous psr return with memberDetails"
        )
    )

    act.like(
      redirectToPage(
        onSelectToView,
        controllers.nonsipp.routes.BasicDetailsCheckYourAnswersController.onPageLoadViewOnly(
          srn,
          yearString,
          submissionNumberTwo,
          submissionNumberOne
        ),
        userAnswers = overThresholdUA
      ).before {
          when(mockPsrVersionsService.getVersions(any(), any(), any())(any(), any())).thenReturn(
            Future.successful(versionsResponse)
          )
          when(
            mockPsrRetrievalService
              .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
          ).thenReturn(Future.successful(overThresholdUA))
        }
        .after {
          verify(mockPsrVersionsService, times(1)).getVersions(any(), any(), any())(any(), any())
          verify(mockPsrRetrievalService, times(1))
            .getAndTransformStandardPsrDetails(any(), any(), any(), any(), any())(any(), any(), any())
        }
        .withName(
          "onSelectToView redirects ok the BasicDetailsCYA page when members over threshold and no " +
            "previous userAnswers and previous psr return with memberDetails"
        )
    )
  }
}
