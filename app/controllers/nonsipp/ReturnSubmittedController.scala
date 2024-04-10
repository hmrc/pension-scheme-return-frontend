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

import play.api.libs.json.Writes._
import services.{SaveService, SchemeDateService}
import viewmodels.implicits._
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.FrontendAppConfig
import cats.implicits.toShow
import controllers.actions._
import pages.nonsipp.ReturnSubmittedPage
import models.requests.DataRequest
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
import javax.inject.Inject

class ReturnSubmittedController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  view: SubmissionView,
  dateService: SchemeDateService,
  config: FrontendAppConfig,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn, mode: Mode): Action[AnyContent] = identifyAndRequireData(srn).async { implicit request =>
    dateService.returnPeriods(srn) match {
      case None => Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
      case Some(returnPeriods) =>
        getOrSaveSubmissionDate(srn).map { submissionDate =>
          Ok(
            view(
              viewModel(
                request.schemeDetails.schemeName,
                request.minimalDetails.email,
                returnPeriods,
                submissionDate,
                config.urls.pensionSchemeEnquiry,
                config.urls.managePensionsSchemes.dashboard
              )
            )
          )
        }
    }
  }

  private def getOrSaveSubmissionDate(srn: Srn)(implicit request: DataRequest[_]): Future[LocalDateTime] =
    request.userAnswers.get(ReturnSubmittedPage(srn)) match {
      case Some(submissionDate) => Future.successful(submissionDate)
      case None =>
        val submissionDate = dateService.now()
        for {
          updatedUserAnswers <- Future.fromTry(request.userAnswers.set(ReturnSubmittedPage(srn), submissionDate))
          _ <- saveService.save(updatedUserAnswers)
        } yield submissionDate
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
            LinkMessage("returnSubmitted.whatHappensNext.paragraph2.link", pensionSchemeEnquiriesUrl)
          ) ++
          ParagraphMessage("returnSubmitted.whatHappensNext.paragraph3") ++
          ListMessage.Bullet(
            Message("returnSubmitted.whatHappensNext.list1") ++ LinkMessage(
              Message("returnSubmitted.whatHappensNext.list1.link", schemeName),
              managePensionSchemeDashboardUrl
            ),
            LinkMessage("returnSubmitted.whatHappensNext.list2", "javascript:window.print();")
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
