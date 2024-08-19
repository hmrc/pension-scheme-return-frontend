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

package controllers.nonsipp.memberpensionpayments

import services.{PsrSubmissionService, SaveService}
import pages.nonsipp.memberdetails.MemberStatus
import viewmodels.implicits._
import play.api.mvc._
import controllers.nonsipp.memberpensionpayments.MemberPensionPaymentsCYAController._
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import config.Refined.Max300
import controllers.PSRController
import models._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp.memberpensionpayments.{MemberPensionPaymentsCYAPage, TotalAmountPensionPaymentsPage}
import controllers.actions.IdentifyAndRequireData
import pages.nonsipp.CompilationOrSubmissionDatePage
import play.api.Logger
import navigation.Navigator
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.{Inject, Named}

class MemberPensionPaymentsCYAController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  psrSubmissionService: PsrSubmissionService,
  saveService: SaveService,
  view: CheckYourAnswersView
)(implicit ec: ExecutionContext)
    extends PSRController {

  private val logger = Logger(getClass)

  def onPageLoad(
    srn: Srn,
    index: Max300,
    mode: Mode
  ): Action[AnyContent] =
    identifyAndRequireData(srn) { implicit request =>
      onPageLoadCommon(srn: Srn, index: Max300, mode: Mode)
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
      onPageLoadCommon(srn: Srn, index: Max300, mode: Mode)
    }

  def onPageLoadCommon(srn: Srn, index: Max300, mode: Mode)(implicit request: DataRequest[AnyContent]): Result =
    (
      for {
        pensionPayment <- request.userAnswers.get(TotalAmountPensionPaymentsPage(srn, index))
        optionList: List[Option[NameDOB]] = request.userAnswers.membersOptionList(srn)
      } yield optionList(index.value - 1)
        .map(_.fullName)
        .getOrRecoverJourney
        .map(
          memberName =>
            Ok(
              view(
                viewModel(
                  srn,
                  memberName,
                  index,
                  pensionPayment,
                  mode,
                  viewOnlyUpdated = false,
                  optYear = request.year,
                  optCurrentVersion = request.currentVersion,
                  optPreviousVersion = request.previousVersion,
                  compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
                )
              )
            )
        )
        .merge
    ).get

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      lazy val memberPensionPaymentsChanged: Boolean =
        request.userAnswers.changed(_.buildMemberPensionPayments(srn, index))

      for {
        updatedAnswers <- request.userAnswers
          .setWhen(memberPensionPaymentsChanged)(MemberStatus(srn, index), {
            logger.info(s"Pension payments has changed for member $index. Setting MemberStatus to Changed")
            MemberState.Changed
          })
          .mapK[Future]
        _ <- saveService.save(updatedAnswers)
        submissionResult <- psrSubmissionService
          .submitPsrDetailsWithUA(
            srn,
            updatedAnswers,
            fallbackCall = controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
              .onPageLoad(srn, index, mode)
          )
      } yield submissionResult.getOrRecoverJourney(
        _ => Redirect(navigator.nextPage(MemberPensionPaymentsCYAPage(srn), mode, request.userAnswers))
      )
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}

object MemberPensionPaymentsCYAController {
  def viewModel(
    srn: Srn,
    memberName: String,
    index: Max300,
    pensionPayments: Money,
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
        normal = "MemberPensionPaymentsCYA.title",
        check = "MemberPensionPaymentsCYA.change.title",
        viewOnly = "MemberPensionPaymentsCYA.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "MemberPensionPaymentsCYA.heading",
        check = Message(
          "MemberPensionPaymentsCYA.change.heading",
          memberName
        ),
        viewOnly = Message("MemberPensionPaymentsCYA.viewOnly.heading", memberName)
      ),
      description = None,
      page = CheckYourAnswersViewModel(
        sections(
          srn,
          memberName,
          index,
          pensionPayments,
          mode match {
            case ViewOnlyMode => NormalMode
            case _ => mode
          }
        )
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
        .onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "MemberPensionPaymentsCYA.viewOnly.title",
            heading = Message("MemberPensionPaymentsCYA.viewOnly.heading", memberName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                controllers.nonsipp.memberpensionpayments.routes.MemberPensionPaymentsCYAController
                  .onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )

  private def sections(
    srn: Srn,
    memberName: String,
    index: Max300,
    pensionPayments: Money,
    mode: Mode
  ): List[CheckYourAnswersSection] =
    List(
      CheckYourAnswersSection(
        None,
        List(
          CheckYourAnswersRowViewModel(
            Message("MemberPensionPaymentsCYA.section.memberName.header"),
            Message(memberName)
          ),
          CheckYourAnswersRowViewModel(
            Message("MemberPensionPaymentsCYA.section.memberName", memberName),
            Message("MemberPensionPaymentsCYA.section.amount", pensionPayments.displayAs)
          ).withAction(
            SummaryAction(
              "site.change",
              controllers.nonsipp.memberpensionpayments.routes.TotalAmountPensionPaymentsController
                .onSubmit(srn, index, mode)
                .url
            ).withVisuallyHiddenContent(Message("MemberPensionPaymentsCYA.section.hide", memberName))
          )
        )
      )
    )

}
