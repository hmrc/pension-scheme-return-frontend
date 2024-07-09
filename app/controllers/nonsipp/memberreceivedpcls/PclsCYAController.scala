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

package controllers.nonsipp.memberreceivedpcls

import services.PsrSubmissionService
import pages.nonsipp.memberdetails.MemberDetailsPage
import play.api.mvc._
import config.Refined._
import controllers.PSRController
import controllers.nonsipp.memberreceivedpcls.PclsCYAController._
import controllers.nonsipp.routes
import controllers.actions._
import models._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import viewmodels.implicits._
import pages.nonsipp.memberreceivedpcls.{PclsCYAPage, PensionCommencementLumpSumAmountPage}
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import navigation.Navigator
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.{Inject, Named}

class PclsCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService
)(implicit ec: ExecutionContext)
    extends PSRController {

  def onPageLoad(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn, index, mode)(implicitly)
    }

  def onPageLoadViewOnly(
    srn: Srn,
    index: Max300,
    mode: Mode,
    year: String,
    current: Int,
    previous: Int
  ): Action[AnyContent] =
    identifyAndRequireData(srn, mode, year, current, previous) { implicit request =>
      onPageLoadCommon(srn, index, mode)(implicitly)
    }

  def onPageLoadCommon(srn: Srn, index: Max300, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    (
      for {
        memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index)).getOrRecoverJourney
        amounts <- request.userAnswers
          .get(PensionCommencementLumpSumAmountPage(srn, index))
          .getOrRecoverJourney
      } yield {
        Ok(
          view(
            viewModel(
              srn,
              memberDetails.fullName,
              index,
              amounts,
              mode,
              viewOnlyUpdated = false, // flag is not displayed on this tier
              optYear = request.year,
              optCurrentVersion = request.currentVersion,
              optPreviousVersion = request.previousVersion,
              compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
            )
          )
        )
      }
    ).merge

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      psrSubmissionService
        .submitPsrDetails(
          srn,
          fallbackCall = controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController.onPageLoad(srn, index, mode)
        )
        .map {
          case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
          case Some(_) =>
            Redirect(navigator.nextPage(PclsCYAPage(srn, index), mode, request.userAnswers))
        }
    }

  def onSubmitViewOnly(srn: Srn, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(Redirect(routes.ViewOnlyTaskListController.onPageLoad(srn, year, current, previous)))
    }

}

object PclsCYAController {
  def viewModel(
    srn: Srn,
    memberName: String,
    index: Max300,
    amounts: PensionCommencementLumpSum,
    mode: Mode,
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel[CheckYourAnswersViewModel](
      mode = mode,
      title = mode.fold(
        normal = "checkYourAnswers.title",
        check = Message("pclsCYA.change.title.check", memberName),
        viewOnly = "pclsCYA.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "checkYourAnswers.heading",
        check = Message("pclsCYA.heading.check", memberName),
        viewOnly = "pclsCYA.viewOnly.heading"
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections = rows(
          srn,
          memberName,
          index,
          amounts,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.continue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "pclsCYA.viewOnly.title",
            heading = Message("pclsCYA.viewOnly.heading", memberName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController
                  .onSubmitViewOnly(srn, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.memberreceivedpcls.routes.PclsCYAController
                  .onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def rows(
    srn: Srn,
    memberName: String,
    index: Max300,
    amounts: PensionCommencementLumpSum,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
        List(
          CheckYourAnswersRowViewModel(
            Message("pclsCYA.rows.membersName"),
            Message(memberName)
          ),
          CheckYourAnswersRowViewModel(
            Message("pclsCYA.rows.amount.received", memberName),
            Message("£" + amounts.lumpSumAmount.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumAmountController
                .onPageLoad(srn, index, CheckMode)
                .url + "#received"
            ).withVisuallyHiddenContent(Message("pclsCYA.rows.amount.received.hidden", memberName))
          ),
          CheckYourAnswersRowViewModel(
            Message("pclsCYA.rows.amount.relevant", memberName),
            Message("£" + amounts.designatedPensionAmount.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberreceivedpcls.routes.PensionCommencementLumpSumAmountController
                .onPageLoad(srn, index, mode)
                .url + "#relevant"
            ).withVisuallyHiddenContent(Message("pclsCYA.rows.amount.relevant.hidden", memberName))
          )
        )
      )
    )
}
