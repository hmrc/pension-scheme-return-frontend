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

import services.{PsrVersionsService, TaxYearService}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.govukfrontend.views.Aliases.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions}
import controllers.actions.{AllowAccessActionProvider, DataRetrievalAction, IdentifierAction}
import models.{DateRange, Enumerable}
import uk.gov.hmrc.time.TaxYear
import views.html.ReturnHistoryView
import models.SchemeId.Srn
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.ReturnHistorySummary

import scala.concurrent.{ExecutionContext, Future}

import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ReturnHistoryController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  getData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  psrVersionsService: PsrVersionsService,
  view: ReturnHistoryView,
  taxYearService: TaxYearService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val allDates = ReturnHistoryController.options(taxYearService.current)
  private val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

  def onPageLoad(srn: Srn): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).async { implicit request =>
      val currentTaxYear = 2022 // TODO
      val response = psrVersionsService
        .getVersionsForYears(request.schemeDetails.pstr, allDates.toList.map(dates => dates._2.from.toString))
      response.map { years =>
        val seqRetHistorySummary = years.flatMap { year =>
          val reportVersions = year.data.map(_.reportVersion)
          year.data.map { psrVersion =>
            val maxReportVersion = reportVersions.max
            ReturnHistorySummary(
              key = year.startDate,
              firstValue = psrVersion.reportVersion.toString,
              secondValue = psrVersion.reportFormBundleNumber,
              thirdValue = psrVersion.reportStatus.toString.capitalize,
              fourthValue = "", // TODO
              actions = Some(
                Actions(
                  items = Seq(
                    if (psrVersion.reportVersion == maxReportVersion) {
                      ActionItem(
                        content = Text("Change"), // TODO
                        href = controllers.routes.ReturnHistoryController
                          .onSelect(srn, psrVersion.reportFormBundleNumber)
                          .url
                      )
                    } else {
                      ActionItem(
                        content = Text("View"), // TODO
                        href = controllers.nonsipp.routes.TaskListViewController
                          .onPageLoad(srn, year.startDate, psrVersion.reportVersion, psrVersion.reportVersion - 1)
                          .url
                      )
                    }
                  )
                )
              )
            )
          }
        }

        val schemeName = request.schemeDetails.schemeName
        val taxYearRange = (currentTaxYear.toString, (currentTaxYear + 1).toString)
        Ok(view(seqRetHistorySummary, taxYearRange._1, taxYearRange._2, schemeName))
      }
    }

  def onSelect(srn: Srn, fbNumber: String): Action[AnyContent] =
    identify.andThen(allowAccess(srn)).andThen(getData).async { implicit request =>
      Future.successful(Redirect(controllers.routes.WhatYouWillNeedController.onPageLoad(srn, fbNumber, "", "")))
    }
}

object ReturnHistoryController {

  def options(startingTaxYear: TaxYear): Enumerable[DateRange] = {

    val taxYears = List.iterate(startingTaxYear, 7)(_.previous)

    val taxYearRanges = taxYears.map(DateRange.from).map(r => (r.toString, r))

    Enumerable(taxYearRanges: _*)
  }
}
