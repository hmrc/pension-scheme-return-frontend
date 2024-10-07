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

package controllers.nonsipp.memberdetails

import services.{PsrSubmissionService, SaveService}
import controllers.nonsipp.memberdetails.SchemeMemberDetailsAnswersController._
import pages.nonsipp.memberdetails._
import play.api.mvc._
import com.google.inject.Inject
import config.Refined.Max300
import controllers.PSRController
import cats.implicits.{toShow, toTraverseOps}
import controllers.actions._
import play.api.i18n.MessagesApi
import models.requests.DataRequest
import viewmodels.implicits._
import utils.MessageUtils.booleanToMessage
import views.html.CheckYourAnswersView
import models.SchemeId.Srn
import pages.nonsipp.CompilationOrSubmissionDatePage
import uk.gov.hmrc.domain.Nino
import play.api.Logger
import navigation.Navigator
import utils.DateTimeUtils.localDateShow
import models._
import utils.FunctionKUtils._
import viewmodels.DisplayMessage.Message
import viewmodels.models._

import scala.concurrent.{ExecutionContext, Future}

import java.time.LocalDateTime
import javax.inject.Named

class SchemeMemberDetailsAnswersController @Inject()(
  override val messagesApi: MessagesApi,
  @Named("non-sipp") navigator: Navigator,
  identifyAndRequireData: IdentifyAndRequireData,
  saveService: SaveService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  psrSubmissionService: PsrSubmissionService
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
      onPageLoadCommon(srn, index, mode)
    }

  def onPageLoadCommon(srn: Srn, index: Max300, mode: Mode)(
    implicit request: DataRequest[AnyContent]
  ): Result =
    (
      for {
        memberDetails <- request.userAnswers.get(MemberDetailsPage(srn, index))
        hasNINO <- request.userAnswers.get(DoesMemberHaveNinoPage(srn, index))
        maybeNino <- Option.when(hasNINO)(request.userAnswers.get(MemberDetailsNinoPage(srn, index))).sequence
        maybeNoNinoReason <- Option.when(!hasNINO)(request.userAnswers.get(NoNINOPage(srn, index))).sequence
      } yield Ok(
        view(
          viewModel(
            index,
            srn,
            mode,
            memberDetails,
            hasNINO,
            maybeNino,
            maybeNoNinoReason,
            viewOnlyUpdated = false,
            optYear = request.year,
            optCurrentVersion = request.currentVersion,
            optPreviousVersion = request.previousVersion,
            compilationOrSubmissionDate = request.userAnswers.get(CompilationOrSubmissionDatePage(srn))
          )
        )
      )
    ).getOrElse(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))

  def onSubmit(srn: Srn, index: Max300, mode: Mode): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      lazy val memberDetailsChanged: Boolean = (for {
        initial <- request.pureUserAnswers
        initialMemberPayments <- initial.buildMemberDetails(srn, index)
        currentMemberPayments <- request.userAnswers.buildMemberDetails(srn, index)
      } yield initialMemberPayments != currentMemberPayments).getOrElse(false)

      lazy val justAdded = mode.isNormalMode && request.userAnswers.get(MemberStatus(srn, index)).isEmpty

      lazy val addedThisVersion = request.userAnswers.get(MemberPsrVersionPage(srn, index)).isEmpty

      for {
        updatedUserAnswers <- request.userAnswers
        // Only set member state to CHANGED if something has actually changed
        // if memberPsrVersion is not present for member, do not set to CHANGED
        // CHANGED status is checked in the transformer later on to make sure this is the status we want to send to ETMP
          .setWhen(
            mode.isCheckMode && memberDetailsChanged && !addedThisVersion
          )(
            MemberStatus(srn, index), {
              logger.info(s"Something has changed in member payments for member index $index, setting state to CHANGED")
              MemberState.Changed
            }
          )
          // If member is already CHANGED, do not override with NEW if user goes back to CYA page on check mode
          .setWhen(justAdded)(MemberStatus(srn, index), MemberState.New)
          .setWhen(justAdded)(SafeToHardDelete(srn, index), Flag)
          .set(MemberDetailsCompletedPage(srn, index), SectionCompleted)
          .mapK[Future]
        _ <- saveService.save(updatedUserAnswers)
        submissionResult <- psrSubmissionService.submitPsrDetailsWithUA(
          srn,
          updatedUserAnswers,
          fallbackCall = controllers.nonsipp.memberdetails.routes.SchemeMemberDetailsAnswersController
            .onPageLoad(srn, index, mode)
        )
      } yield submissionResult.fold(
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      )(_ => Redirect(navigator.nextPage(SchemeMemberDetailsAnswersPage(srn), NormalMode, request.userAnswers)))
    }

  def onSubmitViewOnly(srn: Srn, page: Int, year: String, current: Int, previous: Int): Action[AnyContent] =
    identifyAndRequireData(srn).async {
      Future.successful(
        Redirect(
          controllers.nonsipp.memberdetails.routes.SchemeMembersListController
            .onPageLoadViewOnly(srn, page, year, current, previous)
        )
      )
    }
}

object SchemeMemberDetailsAnswersController {

  private def rows(
    index: Max300,
    srn: Srn,
    memberDetails: NameDOB,
    hasNINO: Boolean,
    maybeNino: Option[Nino],
    maybeNoNinoReason: Option[String]
  ): List[CheckYourAnswersRowViewModel] =
    List(
      CheckYourAnswersRowViewModel("memberDetails.firstName", memberDetails.firstName)
        .withAction(
          SummaryAction(
            "site.change",
            routes.MemberDetailsController.onPageLoad(srn, index, CheckMode).url + "#firstName"
          ).withVisuallyHiddenContent("memberDetails.firstName")
        ),
      CheckYourAnswersRowViewModel("memberDetails.lastName", memberDetails.lastName)
        .withAction(
          SummaryAction(
            "site.change",
            routes.MemberDetailsController.onPageLoad(srn, index, CheckMode).url + "#lastName"
          ).withVisuallyHiddenContent("memberDetails.lastName")
        ),
      CheckYourAnswersRowViewModel("memberDetails.dateOfBirth", memberDetails.dob.show)
        .withAction(
          SummaryAction(
            "site.change",
            routes.MemberDetailsController.onPageLoad(srn, index, CheckMode).url + "#dateOfBirth"
          ).withVisuallyHiddenContent("memberDetails.dateOfBirth")
        ),
      CheckYourAnswersRowViewModel(
        Message("nationalInsuranceNumber.heading", memberDetails.fullName),
        booleanToMessage(hasNINO)
      ).withAction(
        SummaryAction("site.change", routes.DoesSchemeMemberHaveNINOController.onPageLoad(srn, index, CheckMode).url)
          .withVisuallyHiddenContent(("memberDetailsCYA.nationalInsuranceNumber.hidden", memberDetails.fullName))
      )
    ) ++
      ninoRow(maybeNino, memberDetails.fullName, srn, index) ++
      noNinoReasonRow(maybeNoNinoReason, memberDetails.fullName, srn, index)

  private def ninoRow(
    maybeNino: Option[Nino],
    memberName: String,
    srn: Srn,
    index: Max300
  ): List[CheckYourAnswersRowViewModel] =
    maybeNino.fold(List.empty[CheckYourAnswersRowViewModel])(
      nino =>
        List(
          CheckYourAnswersRowViewModel(Message("memberDetailsNino.heading", memberName), nino.value)
            .withAction(
              SummaryAction("site.change", routes.MemberDetailsNinoController.onPageLoad(srn, index, CheckMode).url)
                .withVisuallyHiddenContent(("memberDetailsCYA.nino.hidden", memberName))
            )
        )
    )

  private def noNinoReasonRow(
    maybeNoNinoReason: Option[String],
    memberName: String,
    srn: Srn,
    index: Max300
  ): List[CheckYourAnswersRowViewModel] =
    maybeNoNinoReason.fold(List.empty[CheckYourAnswersRowViewModel])(
      noNinoReason =>
        List(
          CheckYourAnswersRowViewModel(Message("noNINO.heading", memberName), noNinoReason)
            .withAction(
              SummaryAction("site.change", routes.NoNINOController.onPageLoad(srn, index, CheckMode).url)
                .withVisuallyHiddenContent(("memberDetailsCYA.noNINO.hidden", memberName))
            )
        )
    )

  def viewModel(
    index: Max300,
    srn: Srn,
    mode: Mode,
    memberDetails: NameDOB,
    hasNINO: Boolean,
    maybeNino: Option[Nino],
    maybeNoNinoReason: Option[String],
    viewOnlyUpdated: Boolean,
    optYear: Option[String] = None,
    optCurrentVersion: Option[Int] = None,
    optPreviousVersion: Option[Int] = None,
    compilationOrSubmissionDate: Option[LocalDateTime] = None
  ): FormPageViewModel[CheckYourAnswersViewModel] =
    FormPageViewModel(
      mode = mode,
      title = mode.fold(
        normal = "checkYourAnswers.title",
        check = "changeMemberDetails.title",
        viewOnly = "memberDetailsCYA.viewOnly.title"
      ),
      heading = mode.fold(
        normal = "checkYourAnswers.heading",
        check = Message("changeMemberDetails.heading", memberDetails.fullName),
        viewOnly = Message("memberDetailsCYA.viewOnly.title", memberDetails.fullName)
      ),
      description = None,
      page = CheckYourAnswersViewModel.singleSection(
        rows(index, srn, memberDetails, hasNINO, maybeNino, maybeNoNinoReason)
      ),
      refresh = None,
      buttonText = mode.fold(normal = "site.saveAndContinue", check = "site.continue", viewOnly = "site.continue"),
      onSubmit = routes.SchemeMemberDetailsAnswersController.onSubmit(srn, index, mode),
      optViewOnlyDetails = if (mode == ViewOnlyMode) {
        Some(
          ViewOnlyDetailsViewModel(
            updated = viewOnlyUpdated,
            link = None,
            submittedText = Some(Message("")),
            title = "memberDetailsCYA.viewOnly.title",
            heading = Message("memberDetailsCYA.viewOnly.heading", memberDetails.fullName),
            buttonText = "site.continue",
            onSubmit = (optYear, optCurrentVersion, optPreviousVersion) match {
              case (Some(year), Some(currentVersion), Some(previousVersion)) =>
                // view-only continue button always navigates back to the first list page if paginating
                routes.SchemeMemberDetailsAnswersController
                  .onSubmitViewOnly(srn, 1, year, currentVersion, previousVersion)
              case _ =>
                routes.SchemeMemberDetailsAnswersController
                  .onSubmit(srn, index, mode)
            }
          )
        )
      } else {
        None
      }
    )
}
