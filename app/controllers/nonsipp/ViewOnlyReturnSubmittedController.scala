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

package controllers.nonsipp

import services.{PsrRetrievalService, PsrVersionsService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import controllers.PSRController
import config.FrontendAppConfig
import cats.implicits.toShow
import controllers.actions._
import pages.nonsipp.WhichTaxYearPage
import models.backend.responses.PsrVersionsResponse
import views.html.SubmissionView
import models.SchemeId.Srn
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models.{DateRange, UserAnswers}
import play.api.i18n.MessagesApi
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage._
import viewmodels.models.SubmissionViewModel

import scala.concurrent.ExecutionContext

import javax.inject.Inject

class ViewOnlyReturnSubmittedController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: SubmissionView,
  psrRetrievalService: PsrRetrievalService,
  psrVersionsService: PsrVersionsService,
  config: FrontendAppConfig
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, year: String, version: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      val dashboardLink = if (request.pensionSchemeId.isPSP) {
        config.urls.managePensionsSchemes.schemeSummaryPSPDashboard(srn)
      } else {
        config.urls.managePensionsSchemes.schemeSummaryDashboard(srn)
      }
      for {
        retrievedUserAnswers <- psrRetrievalService.getAndTransformStandardPsrDetails(
          None,
          Some(year),
          Some("%03d".format(version)),
          controllers.routes.OverviewController.onPageLoad(srn)
        )
        psrVersionsResponse <- psrVersionsService.getVersions(request.schemeDetails.pstr, year, srn)
        viewModel = ViewOnlyReturnSubmittedController.viewModel(
          srn,
          request.schemeDetails.schemeName,
          retrievedUserAnswers,
          psrVersionsResponse,
          version,
          dashboardLink
        )
      } yield Ok(view(viewModel))
    }
}

object ViewOnlyReturnSubmittedController {

  def viewModel(
    srn: Srn,
    schemeName: String,
    retrievedUserAnswers: UserAnswers,
    psrVersionsResponse: Seq[PsrVersionsResponse],
    version: Int,
    managePensionSchemeDashboardUrl: String
  ): SubmissionViewModel = {

    val periodOfReturn: DisplayMessage = retrievedUserAnswers.get(WhichTaxYearPage(srn)) match {
      case Some(dateRange) =>
        Message("site.to", dateRange.from.show, dateRange.to.show)
      case _ => Empty
    }

    val dateSubmitted: DisplayMessage = psrVersionsResponse.find(p => p.reportVersion == version) match {
      case Some(psrVersionResponse) =>
        Message(
          "site.at",
          psrVersionResponse.compilationOrSubmissionDate.show,
          psrVersionResponse.compilationOrSubmissionDate.format(DateRange.readableTimeFormat).toLowerCase()
        )
      case None => Empty
    }

    SubmissionViewModel(
      "returnSubmitted.title",
      "returnSubmitted.panel.heading",
      "returnSubmitted.panel.content",
      scheme = Message(schemeName),
      periodOfReturn = periodOfReturn,
      dateSubmitted = dateSubmitted,
      whatHappensNextContent =
        ParagraphMessage("returnSubmitted.whatHappensNext.paragraph1") ++
          ParagraphMessage(
            "returnSubmitted.whatHappensNext.paragraph2",
            LinkMessage(
              Message("returnSubmitted.whatHappensNext.paragraph2.link", schemeName),
              managePensionSchemeDashboardUrl
            ),
            "returnSubmitted.whatHappensNext.paragraph2.linkMessage"
          ) ++
          ParagraphMessage(
            "returnSubmitted.whatHappensNext.paragraph3",
            LinkMessage("returnSubmitted.whatHappensNext.list2", "#print")
          )
    )
  }
}
