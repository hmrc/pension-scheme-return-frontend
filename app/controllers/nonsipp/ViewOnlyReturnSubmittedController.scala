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
import utils.ListUtils.ListOps
import controllers.PSRController
import config.FrontendAppConfig
import cats.implicits.toShow
import controllers.actions._
import pages.nonsipp.WhichTaxYearPage
import models.backend.responses.PsrVersionsResponse
import play.api.i18n.MessagesApi
import cats.data.NonEmptyList
import views.html.SubmissionView
import models.SchemeId.Srn
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models.{DateRange, UserAnswers}
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
          config.urls.managePensionsSchemes.schemeSummaryDashboard(srn)
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

    val schemeRow = Message("returnSubmitted.table.field1") -> Message(schemeName)

    val periodOfReturnRow: List[(Message, Message)] = retrievedUserAnswers.get(WhichTaxYearPage(srn)) match {
      case Some(dateRange) =>
        List(Message("returnSubmitted.table.field2") -> Message("site.to", dateRange.from.show, dateRange.to.show))
      case _ => List.empty
    }

    val dateSubmittedRow: Option[(Message, Message)] = psrVersionsResponse.lift(version) match {
      case Some(psrVersionResponse) =>
        Some(
          Message("returnSubmitted.table.field3") -> Message(
            "site.at",
            psrVersionResponse.compilationOrSubmissionDate.show,
            psrVersionResponse.compilationOrSubmissionDate.format(DateRange.readableTimeFormat).toLowerCase()
          )
        )
      case None => None
    }

    val tailRows: List[(Message, Message)] = periodOfReturnRow :?+ dateSubmittedRow

    SubmissionViewModel(
      "returnSubmitted.title",
      "returnSubmitted.panel.heading",
      "returnSubmitted.panel.content",
      content = TableMessage(NonEmptyList(schemeRow, tailRows)),
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
