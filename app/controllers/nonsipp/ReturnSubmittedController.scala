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

import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.FrontendAppConfig
import cats.implicits.{catsSyntaxTuple2Semigroupal, toShow}
import config.Constants.{RETURN_PERIODS, SUBMISSION_DATE}
import controllers.actions._
import play.api.libs.json.Json
import models.requests.psr.MinimalRequiredSubmission.nonEmptyListFormat
import controllers.nonsipp.ReturnSubmittedController._
import cats.data.NonEmptyList
import views.html.SubmissionView
import models.SchemeId.Srn
import utils.DateTimeUtils.{localDateShow, localDateTimeShow}
import models.{DateRange, Mode}
import play.api.i18n.{I18nSupport, MessagesApi}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage._
import viewmodels.models.SubmissionViewModel

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class ReturnSubmittedController @Inject()(
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  allowAccess: AllowAccessActionProvider,
  view: SubmissionView,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identify.andThen(allowAccess(srn)).async {
    implicit request =>
      (request.session.get(RETURN_PERIODS), request.session.get(SUBMISSION_DATE))
        .mapN { (returnPeriods, submissionDate) =>
          Ok(
            view(
              viewModel(
                request.schemeDetails.schemeName,
                request.minimalDetails.email,
                Json.parse(returnPeriods).as[NonEmptyList[DateRange]],
                LocalDateTime.parse(submissionDate, DateTimeFormatter.ISO_DATE_TIME),
                config.urls.pensionSchemeEnquiry,
                config.urls.managePensionsSchemes.schemeSummaryDashboard(srn)
              )
            )
          )
        }
        .fold(
          Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        )(res => Future(res))
  }
}

object ReturnSubmittedController {

  def viewModel(
    schemeName: String,
    email: String,
    returnPeriods: NonEmptyList[DateRange],
    submissionDate: LocalDateTime,
    pensionSchemeEnquiriesUrl: String,
    managePensionSchemeDashboardUrl: String
  ): SubmissionViewModel =
    SubmissionViewModel(
      "returnSubmitted.title",
      "returnSubmitted.panel.heading",
      "returnSubmitted.panel.content",
      content = ParagraphMessage(Message("returnSubmitted.paragraph", email)) ++
        TableMessage(
          NonEmptyList.of(
            Message("returnSubmitted.table.field1") -> Message(schemeName),
            Message("returnSubmitted.table.field2") -> returnPeriodsToMessage(returnPeriods),
            Message("returnSubmitted.table.field3") -> Message(
              "site.at",
              submissionDate.show,
              submissionDate.format(DateRange.readableTimeFormat).toLowerCase()
            )
          )
        ),
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

  private def returnPeriodsToMessage(returnPeriods: NonEmptyList[DateRange]): DisplayMessage = {
    def toMessage(returnPeriod: DateRange): Message = Message(
      "site.to",
      returnPeriod.from.show,
      returnPeriod.to.show
    )

    returnPeriods match {
      case NonEmptyList(returnPeriod, Nil) => toMessage(returnPeriod)
      case _ => ListMessage(returnPeriods.map(toMessage), ListType.NewLine)
    }
  }
}
